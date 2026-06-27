package gui;

import app.Shell;
import app.TerminalSession;
import commands.CommandRegistry;
import constants.SystemConstants;
import filesystem.FileSystem;
import filesystem.FileSystemMounter;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.DirectoryTree;
import filesystem.nodes.FileNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

@SuppressWarnings({"serial", "this-escape"})
public class TerminalPanel extends JPanel {
    private static final int MAX_HISTORY_SIZE = 100;
    private static final char CTRL_X = '\u0018';
    private static final String CLEAR_SEQUENCE = "\033[H\033[2J";
    private static final Pattern MOUNTED_PROMPT = Pattern.compile(
            "^([^\\s:@]+@" + Pattern.quote(SystemConstants.FILE_SYSTEM_NAME) + ")(:)([^$\\n]+)(\\$\\s?)"
    );
    private static final Pattern INIT_PROMPT = Pattern.compile(
            "^(" + Pattern.quote(SystemConstants.FILE_SYSTEM_NAME) + "\\(init\\)>\\s?)"
    );

    private final int id;
    private final String diskName;
    private final ThreadDispatchPrintStream outputDispatcher;
    private final Runnable stateChangeCallback;
    private final TerminalSession session;
    private final JTextPane terminalPane;
    private final JLabel statusLabel;
    private final TerminalOutputStream outputStream;
    private final PipedOutputStream inputWriter;
    private final Thread shellThread;
    private final CommandRegistry commandRegistry;
    private final List<String> commandHistory;
    private final Deque<String> pendingPasteLines;
    private final SimpleAttributeSet inputStyle;
    private final SimpleAttributeSet defaultStyle;
    private final SimpleAttributeSet promptUserStyle;
    private final SimpleAttributeSet promptPathStyle;
    private final SimpleAttributeSet promptSymbolStyle;
    private final SimpleAttributeSet errorStyle;
    private final SimpleAttributeSet warningStyle;
    private final SimpleAttributeSet successStyle;
    private final SimpleAttributeSet helpHintStyle;
    private final SimpleAttributeSet directoryOutputStyle;
    private final SimpleAttributeSet fileOutputStyle;
    private final SimpleAttributeSet linkOutputStyle;
    private final StringBuilder hiddenInput;
    private TerminalTheme theme;
    private int inputStart;
    private int historyIndex;
    private String historyDraft;
    private boolean internalDocumentWrite;
    private boolean passwordInputMode;
    private boolean restylingDocument;
    private boolean acceptingShellCommand;
    private String pendingPasteDraft;

    public TerminalPanel(
            int id,
            String diskName,
            ThreadDispatchPrintStream outputDispatcher,
            TerminalTheme theme,
            Runnable stateChangeCallback
    ) {
        super(new BorderLayout());
        this.id = id;
        this.diskName = diskName;
        this.outputDispatcher = outputDispatcher;
        this.theme = theme;
        this.stateChangeCallback = stateChangeCallback;
        this.session = new TerminalSession(diskName);
        this.terminalPane = new JTextPane();
        this.statusLabel = new JLabel("Terminal " + id);
        this.outputStream = new TerminalOutputStream();
        this.commandRegistry = new CommandRegistry();
        this.commandHistory = new ArrayList<>();
        this.pendingPasteLines = new ArrayDeque<>();
        this.inputStyle = new SimpleAttributeSet();
        this.defaultStyle = new SimpleAttributeSet();
        this.promptUserStyle = new SimpleAttributeSet();
        this.promptPathStyle = new SimpleAttributeSet();
        this.promptSymbolStyle = new SimpleAttributeSet();
        this.errorStyle = new SimpleAttributeSet();
        this.warningStyle = new SimpleAttributeSet();
        this.successStyle = new SimpleAttributeSet();
        this.helpHintStyle = new SimpleAttributeSet();
        this.directoryOutputStyle = new SimpleAttributeSet();
        this.fileOutputStyle = new SimpleAttributeSet();
        this.linkOutputStyle = new SimpleAttributeSet();
        this.hiddenInput = new StringBuilder();
        this.inputStart = 0;
        this.historyIndex = 0;
        this.historyDraft = "";
        this.pendingPasteDraft = null;

        try {
            PipedInputStream inputPipe = new PipedInputStream();
            this.inputWriter = new PipedOutputStream(inputPipe);
            Scanner scanner = new Scanner(inputPipe, StandardCharsets.UTF_8);
            this.shellThread = new Thread(
                    () -> runShell(scanner),
                    SystemConstants.FILE_SYSTEM_NAME + "-gui-terminal-" + id
            );
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo iniciar la terminal grafica.", exception);
        }

        buildLayout();
        applyTheme(theme);
        shellThread.setDaemon(true);
        shellThread.start();
    }

    private void buildLayout() {
        terminalPane.setEditable(true);
        terminalPane.setMargin(new Insets(10, 12, 10, 12));
        terminalPane.setBorder(BorderFactory.createEmptyBorder());
        ((AbstractDocument) terminalPane.getDocument()).setDocumentFilter(new PromptProtectionFilter());
        terminalPane.addCaretListener(event -> {
            if (internalDocumentWrite) {
                return;
            }
            int length = terminalPane.getDocument().getLength();
            if (inputStart > length) {
                inputStart = length;
            }
            if (terminalPane.getCaretPosition() < inputStart) {
                setCaretPositionSafe(inputStart);
            }
        });
        installKeyBindings();

        JScrollPane scrollPane = new JScrollPane(terminalPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);
    }

    private void installKeyBindings() {
        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "submit-line");
        terminalPane.getActionMap().put("submit-line", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submitCurrentInput("");
            }
        });

        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("UP"), "history-previous");
        terminalPane.getActionMap().put("history-previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                navigateHistory(-1);
            }
        });

        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "history-next");
        terminalPane.getActionMap().put("history-next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                navigateHistory(1);
            }
        });

        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "complete-input");
        terminalPane.getActionMap().put("complete-input", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                completeInput();
            }
        });

        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("control X"), "submit-ctrl-x");
        terminalPane.getActionMap().put("submit-ctrl-x", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submitCurrentInput(String.valueOf(CTRL_X), "^X");
            }
        });

        terminalPane.getInputMap().put(KeyStroke.getKeyStroke("HOME"), "home-input");
        terminalPane.getActionMap().put("home-input", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                setCaretPositionSafe(inputStart);
            }
        });
    }

    private void runShell(Scanner scanner) {
        outputDispatcher.register(Thread.currentThread(), outputStream);
        try {
            initializeSession();
            System.out.println("Escribe \"help\" para ver los comandos disponibles.");
            System.out.println();
            new Shell(session, commandRegistry, scanner).start();
        } catch (RuntimeException exception) {
            System.out.println();
            System.out.println("Error en la terminal grafica: " + exception.getMessage());
        } finally {
            outputDispatcher.unregister(Thread.currentThread());
            SwingUtilities.invokeLater(() -> {
                terminalPane.setEditable(false);
                statusLabel.setText("Terminal finalizada");
                notifyStateChanged();
            });
        }
    }

    private void initializeSession() {
        FileSystemMounter mounter = new FileSystemMounter();

        if (!mounter.existsDisk(diskName)) {
            System.out.println("No se encontro un disco virtual formateado.");
            System.out.println("Debe ejecutar el comando format para crear el File System.");
            System.out.println();
            notifyStateChanged();
            return;
        }

        System.out.println("Disco encontrado.");
        System.out.println("Verificando MBR...");

        if (!mounter.isValidFileSystem(diskName)) {
            System.out.println("El disco existe, pero no contiene un File System valido.");
            System.out.println("Ejecute format para crear un File System nuevo.");
            System.out.println();
            notifyStateChanged();
            return;
        }

        try {
            System.out.println("File System valido.");
            System.out.println("Montando " + SystemConstants.FILE_SYSTEM_NAME + "...");
            FileSystem fileSystem = mounter.mount(diskName);
            session.mount(fileSystem);
            System.out.println("Sistema montado correctamente.");
            System.out.println();
        } catch (IOException exception) {
            System.out.println("No se pudo montar el File System: " + exception.getMessage());
            System.out.println();
        }

        notifyStateChanged();
    }

    public void submitCommand(String command) {
        showTerminalInput();
        replaceCurrentInput(command == null ? "" : command);
        submitCurrentInput("");
    }

    public void prefillCommand(String command) {
        replaceCurrentInput(command == null ? "" : command);
        showTerminalInput();
    }

    public void requestTerminalFocus() {
        showTerminalInput();
    }

    public void clearTerminal() {
        runOnDocument(() -> {
            StyledDocument document = terminalPane.getStyledDocument();
            try {
                inputStart = 0;
                hiddenInput.setLength(0);
                passwordInputMode = false;
                document.remove(0, document.getLength());
                setCaretPositionSafe(0);
            } catch (BadLocationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    public void stopShell() {
        try {
            inputWriter.close();
        } catch (IOException exception) {
            // The shell may already be closed.
        }
    }

    public void applyTheme(TerminalTheme theme) {
        this.theme = theme;
        setBackground(theme.getFrameBackground());
        terminalPane.setBackground(theme.getTerminalBackground());
        terminalPane.setForeground(theme.getTerminalForeground());
        terminalPane.setCaretColor(theme.getPrompt());
        terminalPane.setFont(theme.getTerminalFont());
        statusLabel.setOpaque(true);
        statusLabel.setBackground(theme.getSurface());
        statusLabel.setForeground(theme.getPrompt());
        configureStyles();
        restyleDocument();
    }

    public TerminalSession getSession() {
        return session;
    }

    public String getTabTitle() {
        if (session.getActiveUser() == null) {
            return "init@" + SystemConstants.FILE_SYSTEM_NAME + " [" + id + "]";
        }

        return session.getActiveUser().getUsername() + "@" + SystemConstants.FILE_SYSTEM_NAME + " [" + id + "]";
    }

    public String getStatusText() {
        String user = session.getActiveUser() == null ? "init" : session.getActiveUser().getUsername();
        return "Usuario: " + user + "    Ruta: " + session.getCurrentPath() + "    Disco: " + diskName;
    }

    private void submitCurrentInput(String suffixToSend) {
        submitCurrentInput(suffixToSend, "");
    }

    private void submitCurrentInput(String suffixToSend, String visibleSuffix) {
        try {
            StyledDocument document = terminalPane.getStyledDocument();
            int length = document.getLength();
            if (length < inputStart) {
                inputStart = length;
            }

            boolean hidden = passwordInputMode;
            String line = hidden ? hiddenInput.toString() : document.getText(inputStart, length - inputStart);
            boolean shellCommand = acceptingShellCommand && !hidden;
            if (shellCommand) {
                rememberCommand(line);
                acceptingShellCommand = false;
            }
            runOnDocument(() -> {
                try {
                    if (!hidden && !visibleSuffix.isEmpty()) {
                        document.insertString(document.getLength(), visibleSuffix, promptSymbolStyle);
                    }
                    document.insertString(document.getLength(), System.lineSeparator(), defaultStyle);
                    inputStart = document.getLength();
                    setCaretPositionSafe(document.getLength());
                } catch (BadLocationException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            if (hidden) {
                hiddenInput.setLength(0);
                passwordInputMode = false;
            }
            writeInput(line + suffixToSend + "\n");
        } catch (BadLocationException exception) {
            appendShellText("No se pudo leer la entrada de la terminal.\n");
        }
    }

    private void writeInput(String value) {
        try {
            inputWriter.write(value.getBytes(StandardCharsets.UTF_8));
            inputWriter.flush();
        } catch (IOException exception) {
            appendShellText("La terminal no acepta mas entrada.\n");
        }
    }

    private void rememberCommand(String command) {
        String value = command == null ? "" : command.trim();
        if (value.isEmpty()) {
            resetHistoryCursor();
            return;
        }

        if (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(value)) {
            commandHistory.add(value);
            if (commandHistory.size() > MAX_HISTORY_SIZE) {
                commandHistory.remove(0);
            }
        }
        resetHistoryCursor();
    }

    private void navigateHistory(int direction) {
        if (passwordInputMode || !acceptingShellCommand || commandHistory.isEmpty()) {
            return;
        }

        try {
            if (historyIndex == commandHistory.size()) {
                historyDraft = getCurrentInput();
            }

            int nextIndex = Math.max(0, Math.min(commandHistory.size(), historyIndex + direction));
            if (nextIndex == historyIndex) {
                return;
            }

            historyIndex = nextIndex;
            String value = historyIndex == commandHistory.size()
                    ? historyDraft
                    : commandHistory.get(historyIndex);
            replaceCurrentInput(value);
        } catch (BadLocationException exception) {
            appendShellText("No se pudo leer el historial de comandos.\n");
        }
    }

    private void resetHistoryCursor() {
        historyIndex = commandHistory.size();
        historyDraft = "";
    }

    private String getCurrentInput() throws BadLocationException {
        StyledDocument document = terminalPane.getStyledDocument();
        int length = document.getLength();
        if (inputStart > length) {
            inputStart = length;
        }
        return document.getText(inputStart, length - inputStart);
    }

    private void replaceCurrentInput(String value) {
        replaceCurrentInput(value, value == null ? 0 : value.length());
    }

    private void replaceCurrentInput(String value, int caretOffset) {
        if (passwordInputMode) {
            hiddenInput.setLength(0);
            hiddenInput.append(value);
            showTerminalInput();
            return;
        }

        runOnDocument(() -> {
            try {
                StyledDocument document = terminalPane.getStyledDocument();
                int length = document.getLength();
                if (inputStart > length) {
                    inputStart = length;
                }
                document.remove(inputStart, length - inputStart);
                String safeValue = value == null ? "" : value;
                document.insertString(inputStart, safeValue, inputStyle);
                setCaretPositionSafe(inputStart + Math.max(0, Math.min(caretOffset, safeValue.length())));
            } catch (BadLocationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private void completeInput() {
        if (passwordInputMode || !acceptingShellCommand) {
            return;
        }

        try {
            String input = getCurrentInput();
            int caretOffset = Math.max(0, Math.min(
                    terminalPane.getCaretPosition() - inputStart,
                    input.length()
            ));
            Token token = tokenAt(input, caretOffset);
            List<String> matches = completionMatches(input, token);

            if (matches.isEmpty()) {
                return;
            }

            if (matches.size() == 1) {
                String replacement = matches.get(0);
                if (token.isCommand() || !replacement.endsWith("/")) {
                    replacement += " ";
                }
                replaceToken(input, token, replacement);
                return;
            }

            String commonPrefix = commonPrefix(matches);
            if (commonPrefix.length() > token.value().length()) {
                replaceToken(input, token, commonPrefix);
                return;
            }

            showCompletionOptions(matches, input);
        } catch (BadLocationException exception) {
            appendShellText("No se pudo autocompletar la entrada.\n");
        }
    }

    private List<String> completionMatches(String input, Token token) {
        if (token.isCommand()) {
            return commandMatches(token.value());
        }

        return pathMatches(firstCommand(input), token.value());
    }

    private List<String> commandMatches(String prefix) {
        List<String> names = new ArrayList<>();
        if (session.getFileSystem() == null) {
            Collections.addAll(names, "format", "clear", "help", "exit");
        } else {
            commandRegistry.getCommands().forEach(command -> names.add(command.getName()));
        }

        return names.stream()
                .filter(name -> name.startsWith(prefix))
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> pathMatches(String commandName, String tokenValue) {
        if (session.getFileSystem() == null || tokenValue.contains("*")) {
            return List.of();
        }

        DirectoryTree directoryTree = session.getFileSystem().getDirectoryTree();
        int slashIndex = tokenValue.lastIndexOf('/');
        String rawDirectory = slashIndex >= 0 ? tokenValue.substring(0, slashIndex + 1) : "";
        String namePrefix = slashIndex >= 0 ? tokenValue.substring(slashIndex + 1) : tokenValue;
        String requestedDirectory = rawDirectory.isEmpty() ? "." : rawDirectory;
        String directoryPath = directoryTree.normalizePath(session.getCurrentPath(), requestedDirectory);
        DirectoryNode directory = directoryTree.find(directoryPath).orElse(null);

        if (directory == null) {
            return List.of();
        }

        boolean directoriesOnly = "cd".equals(commandName);
        List<String> matches = new ArrayList<>();
        for (DirectoryNode child : directory.getDirectories()) {
            if (child.getName().startsWith(namePrefix)) {
                matches.add(rawDirectory + child.getName() + "/");
            }
        }

        if (!directoriesOnly) {
            for (FileNode file : directory.getFiles()) {
                if (file.getName().startsWith(namePrefix)) {
                    matches.add(rawDirectory + file.getName());
                }
            }
            for (String linkName : directory.getLinks().keySet()) {
                if (linkName.startsWith(namePrefix)) {
                    matches.add(rawDirectory + linkName);
                }
            }
        }

        Collections.sort(matches);
        return matches;
    }

    private void replaceToken(String input, Token token, String replacement) {
        String newInput = input.substring(0, token.start())
                + replacement
                + input.substring(token.end());
        replaceCurrentInput(newInput, token.start() + replacement.length());
    }

    private void showCompletionOptions(List<String> options, String currentInput) {
        runOnDocument(() -> {
            try {
                StyledDocument document = terminalPane.getStyledDocument();
                document.insertString(document.getLength(), System.lineSeparator(), defaultStyle);
                document.insertString(document.getLength(), String.join("  ", options), defaultStyle);
                document.insertString(document.getLength(), System.lineSeparator(), defaultStyle);

                boolean previousRestyling = restylingDocument;
                restylingDocument = true;
                try {
                    appendStyledShellText(session.getPrompt());
                } finally {
                    restylingDocument = previousRestyling;
                }

                inputStart = document.getLength();
                acceptingShellCommand = true;
                resetHistoryCursor();
                document.insertString(inputStart, currentInput, inputStyle);
                setCaretPositionSafe(document.getLength());
            } catch (BadLocationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private Token tokenAt(String input, int caretOffset) {
        int start = caretOffset;
        while (start > 0 && !Character.isWhitespace(input.charAt(start - 1))) {
            start--;
        }

        int end = caretOffset;
        while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
            end++;
        }

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < input.length()
                && Character.isWhitespace(input.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        return new Token(start, end, input.substring(start, end), start == firstNonWhitespace);
    }

    private String firstCommand(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int whitespace = trimmed.indexOf(' ');
        return whitespace < 0 ? trimmed : trimmed.substring(0, whitespace);
    }

    private String commonPrefix(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }

        String prefix = values.get(0);
        for (int index = 1; index < values.size(); index++) {
            String value = values.get(index);
            int limit = Math.min(prefix.length(), value.length());
            int offset = 0;
            while (offset < limit && prefix.charAt(offset) == value.charAt(offset)) {
                offset++;
            }
            prefix = prefix.substring(0, offset);
            if (prefix.isEmpty()) {
                return "";
            }
        }
        return prefix;
    }

    private void showTerminalInput() {
        terminalPane.requestFocusInWindow();
        setCaretPositionSafe(terminalPane.getDocument().getLength());
    }

    private void appendShellText(String text) {
        SwingUtilities.invokeLater(() -> {
            String remaining = text;
            if (remaining.contains(CLEAR_SEQUENCE)) {
                clearTerminal();
                remaining = remaining.replace(CLEAR_SEQUENCE, "");
            }

            if (remaining.isEmpty()) {
                return;
            }

            String textToAppend = remaining;
            runOnDocument(() -> {
                appendStyledShellText(textToAppend);
                inputStart = terminalPane.getDocument().getLength();
                setCaretPositionSafe(inputStart);
            });
            passwordInputMode = isCurrentLinePasswordPrompt();
            if (passwordInputMode) {
                hiddenInput.setLength(0);
            }
            if (!passwordInputMode && isCurrentLineShellPrompt()) {
                acceptingShellCommand = true;
                resetHistoryCursor();
            }
            processPendingPasteInput();
            statusLabel.setText(getStatusText());
            notifyStateChanged();
        });
    }

    private void appendStyledShellText(String text) {
        int index = 0;
        while (index < text.length()) {
            int newline = text.indexOf('\n', index);
            int end = newline >= 0 ? newline + 1 : text.length();
            appendStyledSegment(text.substring(index, end));
            index = end;
        }
    }

    private void appendStyledSegment(String segment) {
        if (segment.isEmpty()) {
            return;
        }

        Matcher mountedPrompt = MOUNTED_PROMPT.matcher(segment);
        Matcher initPrompt = INIT_PROMPT.matcher(segment);

        try {
            StyledDocument document = terminalPane.getStyledDocument();
            if (mountedPrompt.find()) {
                String promptText = mountedPrompt.group(1) + mountedPrompt.group(2)
                        + mountedPrompt.group(3) + mountedPrompt.group(4);
                if (isDuplicatePrompt(promptText)) {
                    return;
                }
                document.insertString(document.getLength(), mountedPrompt.group(1), promptUserStyle);
                document.insertString(document.getLength(), mountedPrompt.group(2), promptSymbolStyle);
                document.insertString(document.getLength(), mountedPrompt.group(3), promptPathStyle);
                document.insertString(document.getLength(), mountedPrompt.group(4), promptSymbolStyle);
                String rest = segment.substring(mountedPrompt.end());
                document.insertString(document.getLength(), rest, inputStyle);
                markShellPromptReady();
                return;
            }

            if (initPrompt.find()) {
                if (isDuplicatePrompt(initPrompt.group(1))) {
                    return;
                }
                document.insertString(document.getLength(), initPrompt.group(1), promptUserStyle);
                String rest = segment.substring(initPrompt.end());
                document.insertString(document.getLength(), rest, inputStyle);
                markShellPromptReady();
                return;
            }

            document.insertString(document.getLength(), segment, styleForOutput(segment));
        } catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isDuplicatePrompt(String promptText) throws BadLocationException {
        if (restylingDocument) {
            return false;
        }

        StyledDocument document = terminalPane.getStyledDocument();
        int length = document.getLength();
        if (length == 0 || length != inputStart) {
            return false;
        }

        String text = document.getText(0, length);
        int lastNewline = text.lastIndexOf('\n');
        String currentLine = text.substring(lastNewline + 1);
        return currentLine.equals(promptText);
    }

    private void markShellPromptReady() {
        if (restylingDocument) {
            return;
        }
        acceptingShellCommand = true;
        resetHistoryCursor();
    }

    private AttributeSet styleForOutput(String text) {
        String value = text.toLowerCase();
        if (value.contains("escribe \"help\"")) {
            return helpHintStyle;
        }
        if (value.startsWith("[dir] ") || isRecursiveDirectoryHeader(text)) {
            return directoryOutputStyle;
        }
        if (value.startsWith("[file] ")) {
            return fileOutputStyle;
        }
        if (value.startsWith("[link] ")) {
            return linkOutputStyle;
        }
        if (value.contains("error") || value.contains("no se pudo") || value.contains(": no existe")
                || value.contains("denegado") || value.contains("invalido")) {
            return errorStyle;
        }
        if (value.contains("correctamente") || value.contains("creado") || value.contains("montado")) {
            return successStyle;
        }
        if (value.contains("use ") || value.contains("debe ") || value.contains("advertencia")
                || value.contains("cancelada")) {
            return warningStyle;
        }
        return defaultStyle;
    }

    private boolean isRecursiveDirectoryHeader(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith("/") && trimmed.endsWith(":");
    }

    private void configureStyles() {
        configure(inputStyle, theme.getTerminalForeground(), Font.BOLD);
        configure(defaultStyle, theme.getTerminalForeground(), Font.PLAIN);
        configure(promptUserStyle, theme.getPrompt(), Font.BOLD);
        configure(promptPathStyle, theme.getDirectory(), Font.BOLD);
        configure(promptSymbolStyle, theme.getTerminalForeground(), Font.BOLD);
        configure(errorStyle, theme.getError(), Font.PLAIN);
        configure(warningStyle, theme.getWarning(), Font.PLAIN);
        configure(successStyle, theme.getSuccess(), Font.PLAIN);
        configure(helpHintStyle, theme.getWarning(), Font.BOLD);
        configure(directoryOutputStyle, theme.getDirectory(), Font.BOLD);
        configure(fileOutputStyle, theme.getFile(), Font.PLAIN);
        configure(linkOutputStyle, linkColor(), Font.BOLD);
    }

    private Color linkColor() {
        return theme.isLightMode() ? new Color(0, 120, 145) : new Color(52, 226, 226);
    }

    private void configure(SimpleAttributeSet style, Color foreground, int fontStyle) {
        StyleConstants.setForeground(style, foreground);
        StyleConstants.setFontFamily(style, theme.getTerminalFont().getFamily());
        StyleConstants.setFontSize(style, theme.getTerminalFont().getSize());
        StyleConstants.setBold(style, (fontStyle & Font.BOLD) != 0);
        StyleConstants.setItalic(style, (fontStyle & Font.ITALIC) != 0);
    }

    private void restyleDocument() {
        runOnDocument(() -> {
            try {
                StyledDocument document = terminalPane.getStyledDocument();
                String text = document.getText(0, document.getLength());
                int savedInputStart = Math.min(inputStart, text.length());
                int savedCaret = Math.min(terminalPane.getCaretPosition(), text.length());

                document.remove(0, document.getLength());
                boolean previousRestyling = restylingDocument;
                restylingDocument = true;
                try {
                    appendStyledShellText(text);
                } finally {
                    restylingDocument = previousRestyling;
                }

                inputStart = Math.min(savedInputStart, document.getLength());
                setCaretPositionSafe(Math.max(inputStart, Math.min(savedCaret, document.getLength())));
            } catch (BadLocationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private void setCaretPositionSafe(int position) {
        int length = terminalPane.getDocument().getLength();
        int safePosition = Math.max(0, Math.min(position, length));
        if (terminalPane.getCaretPosition() != safePosition) {
            terminalPane.setCaretPosition(safePosition);
        }
    }

    private boolean isCurrentLinePasswordPrompt() {
        try {
            String currentLine = currentDocumentLine().toLowerCase().trim();
            String normalizedLine = currentLine.replace('\u00f1', 'n');
            return normalizedLine.contains("contrasena") && normalizedLine.endsWith(":");
        } catch (BadLocationException exception) {
            return false;
        }
    }

    private boolean isCurrentLineShellPrompt() {
        try {
            String currentLine = currentDocumentLine();
            return MOUNTED_PROMPT.matcher(currentLine).find()
                    || INIT_PROMPT.matcher(currentLine).find();
        } catch (BadLocationException exception) {
            return false;
        }
    }

    private boolean isCurrentLineTextPrompt() {
        try {
            String currentLine = currentDocumentLine().trim();
            return !currentLine.isEmpty() && currentLine.endsWith(":");
        } catch (BadLocationException exception) {
            return false;
        }
    }

    private String currentDocumentLine() throws BadLocationException {
        StyledDocument document = terminalPane.getStyledDocument();
        String text = document.getText(0, document.getLength());
        int lastNewline = text.lastIndexOf('\n');
        return text.substring(lastNewline + 1);
    }

    private void enqueuePastedInput(PastePlan plan) {
        pendingPasteLines.addAll(plan.linesToSubmit());
        if (plan.draft() != null) {
            pendingPasteDraft = plan.draft();
        }
        processPendingPasteInput();
    }

    private void processPendingPasteInput() {
        if (!isReadyForPastedInput()) {
            return;
        }

        if (!pendingPasteLines.isEmpty()) {
            String nextLine = pendingPasteLines.removeFirst();
            replaceCurrentInput(nextLine);
            submitCurrentInput("");
            return;
        }

        if (pendingPasteDraft != null) {
            String draft = pendingPasteDraft;
            pendingPasteDraft = null;
            replaceCurrentInput(draft);
        }
    }

    private boolean isReadyForPastedInput() {
        if (!terminalPane.isEditable()) {
            return false;
        }

        if (passwordInputMode) {
            return true;
        }

        return (acceptingShellCommand && isCurrentLineShellPrompt()) || isCurrentLineTextPrompt();
    }

    private boolean isMultilineText(String text) {
        return text != null && (text.contains("\n") || text.contains("\r"));
    }

    private PastePlan buildPastePlan(String currentInput, int replaceStart, int replaceEnd, String pastedText) {
        String safeInput = currentInput == null ? "" : currentInput;
        int safeStart = Math.max(0, Math.min(replaceStart, safeInput.length()));
        int safeEnd = Math.max(safeStart, Math.min(replaceEnd, safeInput.length()));
        String normalizedText = pastedText.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalizedText.split("\n", -1);
        String prefix = safeInput.substring(0, safeStart);
        String suffix = safeInput.substring(safeEnd);
        List<String> lines = new ArrayList<>();

        lines.add(prefix + parts[0]);
        for (int index = 1; index < parts.length - 1; index++) {
            lines.add(parts[index]);
        }

        String draft = null;
        if (normalizedText.endsWith("\n")) {
            if (!suffix.isEmpty()) {
                draft = suffix;
            }
        } else {
            draft = parts[parts.length - 1] + suffix;
        }

        return new PastePlan(lines, draft);
    }

    private void handleVisibleMultilinePaste(int offset, int length, String text) throws BadLocationException {
        StyledDocument document = terminalPane.getStyledDocument();
        int documentLength = document.getLength();
        int safeInputStart = Math.max(0, Math.min(inputStart, documentLength));
        String currentInput = document.getText(safeInputStart, documentLength - safeInputStart);
        int safeOffset = Math.max(offset, safeInputStart);
        int safeEnd = Math.max(offset + length, safeInputStart);
        PastePlan plan = buildPastePlan(
                currentInput,
                safeOffset - safeInputStart,
                safeEnd - safeInputStart,
                text
        );

        SwingUtilities.invokeLater(() -> enqueuePastedInput(plan));
    }

    private void handleHiddenMultilinePaste(String text) {
        PastePlan plan = buildPastePlan(hiddenInput.toString(), hiddenInput.length(), hiddenInput.length(), text);
        String firstLine = plan.linesToSubmit().isEmpty() ? "" : plan.linesToSubmit().get(0);
        List<String> remainingLines = plan.linesToSubmit().size() <= 1
                ? List.of()
                : new ArrayList<>(plan.linesToSubmit().subList(1, plan.linesToSubmit().size()));

        hiddenInput.setLength(0);
        hiddenInput.append(firstLine);
        SwingUtilities.invokeLater(() -> {
            if (passwordInputMode) {
                submitCurrentInput("");
            }
            enqueuePastedInput(new PastePlan(remainingLines, plan.draft()));
        });
    }

    private void runOnDocument(Runnable runnable) {
        internalDocumentWrite = true;
        try {
            runnable.run();
        } finally {
            internalDocumentWrite = false;
        }
    }

    private void notifyStateChanged() {
        if (stateChangeCallback != null) {
            SwingUtilities.invokeLater(stateChangeCallback);
        }
    }

    private class PromptProtectionFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                throws BadLocationException {
            if (internalDocumentWrite) {
                bypass.insertString(offset, text, attributes);
                return;
            }

            if (passwordInputMode) {
                if (isMultilineText(text)) {
                    handleHiddenMultilinePaste(text);
                    return;
                }
                if (text != null) {
                    hiddenInput.append(text);
                }
                return;
            }

            if (isMultilineText(text)) {
                handleVisibleMultilinePaste(offset, 0, text);
                return;
            }

            bypass.insertString(Math.max(offset, inputStart), text, inputStyle);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            if (internalDocumentWrite) {
                bypass.replace(offset, length, text, attributes);
                return;
            }

            if (passwordInputMode) {
                if (isMultilineText(text)) {
                    handleHiddenMultilinePaste(text);
                    return;
                }
                if (length > 0 && hiddenInput.length() > 0) {
                    hiddenInput.deleteCharAt(hiddenInput.length() - 1);
                }
                if (text != null) {
                    hiddenInput.append(text);
                }
                return;
            }

            int end = offset + length;
            int safeOffset = Math.max(offset, inputStart);
            int safeEnd = Math.max(end, inputStart);
            int safeLength = safeEnd - safeOffset;
            if (isMultilineText(text)) {
                handleVisibleMultilinePaste(offset, length, text);
                return;
            }
            bypass.replace(safeOffset, safeLength, text, inputStyle);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            if (internalDocumentWrite) {
                bypass.remove(offset, length);
                return;
            }

            if (passwordInputMode) {
                if (hiddenInput.length() > 0) {
                    hiddenInput.deleteCharAt(hiddenInput.length() - 1);
                }
                return;
            }

            int end = offset + length;
            if (end <= inputStart) {
                return;
            }

            int safeOffset = Math.max(offset, inputStart);
            int safeLength = end - safeOffset;
            bypass.remove(safeOffset, safeLength);
        }
    }

    private class TerminalOutputStream extends OutputStream {
        @Override
        public void write(int value) {
            byte[] oneByte = {(byte) value};
            appendShellText(new String(oneByte, StandardCharsets.UTF_8));
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            appendShellText(new String(bytes, offset, length, StandardCharsets.UTF_8));
        }
    }

    private record PastePlan(List<String> linesToSubmit, String draft) {
    }

    private record Token(int start, int end, String value, boolean isCommand) {
    }
}
