package gui;

import constants.SystemConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

@SuppressWarnings({"serial", "this-escape"})
public class TerminalManagerFrame extends JFrame {
    private static final String TERMINAL_VIEW = "terminal";
    private static final String DISK_MAP_VIEW = "diskMap";
    private static final String SETTINGS_VIEW = "settings";

    private final ThreadDispatchPrintStream outputDispatcher;
    private final String initialDiskName;
    private final JTabbedPane terminalTabs;
    private final CardLayout cardLayout;
    private final JPanel cards;
    private final JLabel statusBar;
    private final DiskMapPanel diskMapPanel;
    private final SettingsPanel settingsPanel;
    private JPanel rootPanel;
    private JPanel titleBar;
    private JPanel terminalViewPanel;
    private JPanel sidebarPanel;
    private JPanel sidebarActionsPanel;
    private JLabel titleLabel;
    private JLabel versionLabel;
    private JLabel sidebarTitleLabel;
    private JLabel sidebarFooterLabel;
    private JSplitPane splitPane;
    private TerminalTheme theme;
    private int nextTerminalId;
    private JPanel plusTabPanel;
    private JMenuItem themeToggleMenuItem;
    private boolean updatingTerminalTabs;

    public TerminalManagerFrame(String diskName, ThreadDispatchPrintStream outputDispatcher) {
        super(SystemConstants.FILE_SYSTEM_NAME + " Terminal Manager");
        this.initialDiskName = diskName;
        this.outputDispatcher = outputDispatcher;
        this.terminalTabs = new JTabbedPane();
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.statusBar = new JLabel("Listo");
        this.theme = TerminalTheme.puttyDark();
        this.nextTerminalId = 1;
        this.diskMapPanel = new DiskMapPanel(this::getActiveTerminal);
        this.settingsPanel = new SettingsPanel(theme, this::applyTheme);

        buildFrame();
        newTerminal(initialDiskName);
        refreshStatus();
    }

    private void buildFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setJMenuBar(buildMenuBar());

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(theme.getFrameBackground());
        setContentPane(rootPanel);

        rootPanel.add(buildTitleBar(), BorderLayout.NORTH);

        cards.add(buildTerminalView(), TERMINAL_VIEW);
        cards.add(diskMapPanel, DISK_MAP_VIEW);
        cards.add(settingsPanel, SETTINGS_VIEW);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), cards);
        splitPane.setDividerLocation(185);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        rootPanel.add(splitPane, BorderLayout.CENTER);

        statusBar.setOpaque(true);
        statusBar.setBackground(theme.getSurface());
        statusBar.setForeground(new Color(190, 199, 211));
        statusBar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        statusBar.setBorder(new EmptyBorder(4, 10, 4, 10));
        rootPanel.add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(theme.getSurface());
        titleBar.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));

        titleLabel = new JLabel(SystemConstants.FILE_SYSTEM_NAME + " Terminal Manager  -  " + initialDiskName);
        titleLabel.setForeground(new Color(190, 199, 211));
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        titleBar.add(titleLabel, BorderLayout.WEST);

        versionLabel = new JLabel("Java Swing");
        versionLabel.setForeground(new Color(160, 170, 185));
        versionLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        titleBar.add(versionLabel, BorderLayout.EAST);

        return titleBar;
    }

    private JPanel buildTerminalView() {
        terminalViewPanel = new JPanel(new BorderLayout());
        terminalViewPanel.setBackground(theme.getTerminalBackground());
        terminalTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        terminalTabs.setForeground(Color.BLACK);
        addPlusTab();
        terminalViewPanel.add(terminalTabs, BorderLayout.CENTER);
        terminalTabs.addChangeListener(event -> {
            if (updatingTerminalTabs) {
                return;
            }
            if (terminalTabs.getSelectedComponent() == plusTabPanel) {
                newTerminal(initialDiskName);
                return;
            }
            refreshStatus();
            TerminalPanel active = getActiveTerminal();
            if (active != null) {
                active.requestTerminalFocus();
            }
        });
        return terminalViewPanel;
    }

    private JPanel buildSidebar() {
        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(theme.getSurface());
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(70, 82, 100)));

        sidebarTitleLabel = new JLabel("Accesos rapidos");
        sidebarTitleLabel.setForeground(new Color(160, 170, 185));
        sidebarTitleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        sidebarTitleLabel.setBorder(new EmptyBorder(10, 12, 8, 12));
        sidebarPanel.add(sidebarTitleLabel, BorderLayout.NORTH);

        sidebarActionsPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        sidebarActionsPanel.setBackground(theme.getSurface());
        sidebarActionsPanel.setBorder(new EmptyBorder(4, 8, 4, 8));
        sidebarActionsPanel.add(sidebarButton("Terminal", () -> showView(TERMINAL_VIEW)));
        sidebarActionsPanel.add(sidebarButton("Mapa de disco", () -> showView(DISK_MAP_VIEW)));
        sidebarActionsPanel.add(sidebarButton("Defragmentar", this::showDefragPending));
        sidebarActionsPanel.add(sidebarButton("Ajustes", () -> showView(SETTINGS_VIEW)));
        sidebarPanel.add(sidebarActionsPanel, BorderLayout.CENTER);

        sidebarFooterLabel = new JLabel("<html>" + SystemConstants.FILE_SYSTEM_NAME
                + "<br>Terminal visual<br>Comandos reales</html>");
        sidebarFooterLabel.setForeground(new Color(160, 170, 185));
        sidebarFooterLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        sidebarFooterLabel.setBorder(new EmptyBorder(10, 12, 12, 12));
        sidebarFooterLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        sidebarPanel.add(sidebarFooterLabel, BorderLayout.SOUTH);

        return sidebarPanel;
    }

    private JButton sidebarButton(String label, Runnable action) {
        JButton button = new JButton(label);
        GuiStyles.styleSidebarButton(button, theme);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu());
        menuBar.add(terminalMenu());
        menuBar.add(diskMenu());
        menuBar.add(settingsMenu());
        menuBar.add(helpMenu());
        return menuBar;
    }

    private JMenu fileMenu() {
        JMenu menu = new JMenu("Archivo");
        menu.add(item("Nuevo disco", () -> submitCommand("format")));
        menu.add(item("Montar disco", this::openDiskFile));
        menu.add(item("Cerrar disco", this::closeActiveTerminal));
        menu.addSeparator();
        menu.add(item("Salir", this::dispose));
        return menu;
    }

    private JMenu terminalMenu() {
        JMenu menu = new JMenu("Terminal");
        menu.add(item("Nueva terminal", () -> newTerminal(initialDiskName)));
        menu.add(item("Limpiar terminal", () -> submitCommand("clear")));
        menu.add(item("Cambiar usuario", this::runSu));
        menu.add(item("Ver usuario actual", () -> submitCommand("whoami")));
        menu.add(item("Ver ruta actual", () -> submitCommand("pwd")));
        return menu;
    }

    private JMenu diskMenu() {
        JMenu menu = new JMenu("Disco");
        menu.add(item("Ver informacion del sistema de archivos", () -> submitCommand("infoFS")));
        menu.add(item("Ver mapa de bloques", () -> showView(DISK_MAP_VIEW)));
        menu.add(item("Ver fragmentacion", () -> showView(DISK_MAP_VIEW)));
        menu.add(item("Defragmentar disco", this::showDefragPending));
        menu.addSeparator();
        menu.add(item("Ver FCB de archivo", this::runViewFcb));
        menu.add(item("Ver archivos abiertos", () -> submitCommand("viewFilesOpen")));
        return menu;
    }

    private JMenu settingsMenu() {
        JMenu menu = new JMenu("Ajustes");
        themeToggleMenuItem = item(themeToggleText(), this::toggleTheme);
        menu.add(themeToggleMenuItem);
        menu.addSeparator();
        menu.add(item("Cambiar color de fondo", () -> showView(SETTINGS_VIEW)));
        menu.add(item("Cambiar color del texto", () -> showView(SETTINGS_VIEW)));
        menu.add(item("Cambiar color del prompt", () -> showView(SETTINGS_VIEW)));
        menu.add(item("Cambiar tamano de fuente", () -> showView(SETTINGS_VIEW)));
        return menu;
    }

    private JMenu helpMenu() {
        JMenu menu = new JMenu("Ayuda");
        menu.add(item("Ver comandos disponibles", () -> submitCommand("help")));
        menu.add(item("Acerca de " + SystemConstants.FILE_SYSTEM_NAME, this::showAbout));
        return menu;
    }

    private JMenuItem item(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> action.run());
        return item;
    }

    private void newTerminal(String diskName) {
        TerminalPanel terminalPanel = new TerminalPanel(
                nextTerminalId++,
                diskName,
                outputDispatcher,
                theme,
                this::refreshStatus
        );
        int insertIndex = getPlusTabIndex();
        updatingTerminalTabs = true;
        try {
            if (insertIndex >= 0) {
                terminalTabs.insertTab(terminalPanel.getTabTitle(), null, terminalPanel, null, insertIndex);
            } else {
                terminalTabs.addTab(terminalPanel.getTabTitle(), terminalPanel);
            }
            int terminalIndex = terminalTabs.indexOfComponent(terminalPanel);
            terminalTabs.setTabComponentAt(terminalIndex, new TerminalTabHeader(terminalPanel));
            terminalTabs.setSelectedComponent(terminalPanel);
        } finally {
            updatingTerminalTabs = false;
        }
        showView(TERMINAL_VIEW);
        refreshStatus();
    }

    private void closeActiveTerminal() {
        TerminalPanel active = getActiveTerminal();
        if (active == null) {
            return;
        }

        closeTerminal(active);
    }

    private void closeTerminal(TerminalPanel terminalPanel) {
        if (terminalPanel == null) {
            return;
        }

        terminalPanel.stopShell();
        int remainingTerminals;
        updatingTerminalTabs = true;
        try {
            int index = terminalTabs.indexOfComponent(terminalPanel);
            if (index >= 0) {
                terminalTabs.remove(index);
            }
            remainingTerminals = getTerminalCount();
            if (remainingTerminals > 0 && terminalTabs.getSelectedComponent() == plusTabPanel) {
                selectFirstTerminal();
            }
        } finally {
            updatingTerminalTabs = false;
        }
        if (remainingTerminals == 0) {
            newTerminal(initialDiskName);
        }
        refreshStatus();
    }

    private void addPlusTab() {
        plusTabPanel = new JPanel();
        plusTabPanel.setOpaque(false);
        terminalTabs.addTab("+", plusTabPanel);
    }

    private int getPlusTabIndex() {
        return plusTabPanel == null ? -1 : terminalTabs.indexOfComponent(plusTabPanel);
    }

    private int getTerminalCount() {
        int count = 0;
        for (int index = 0; index < terminalTabs.getTabCount(); index++) {
            if (terminalTabs.getComponentAt(index) instanceof TerminalPanel) {
                count++;
            }
        }
        return count;
    }

    private void selectFirstTerminal() {
        for (int index = 0; index < terminalTabs.getTabCount(); index++) {
            if (terminalTabs.getComponentAt(index) instanceof TerminalPanel) {
                terminalTabs.setSelectedIndex(index);
                return;
            }
        }
    }

    private void openDiskFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            newTerminal(chooser.getSelectedFile().getPath());
        }
    }

    private void submitCommand(String command) {
        TerminalPanel active = getActiveTerminal();
        if (active == null) {
            return;
        }

        showView(TERMINAL_VIEW);
        active.submitCommand(command);
        refreshStatus();
    }

    private void runSu() {
        String username = JOptionPane.showInputDialog(this, "Usuario destino. Vacio = root:", "");
        if (username == null) {
            return;
        }

        submitCommand(username.isBlank() ? "su" : "su " + username.trim());
    }

    private void runViewFcb() {
        String filename = JOptionPane.showInputDialog(this, "Archivo para viewFCB:", "");
        if (filename != null && !filename.isBlank()) {
            submitCommand("viewFCB " + filename.trim());
        }
    }

    private void showDefragPending() {
        showView(DISK_MAP_VIEW);
        JOptionPane.showMessageDialog(
                this,
                "La vista esta preparada, pero el core todavia no tiene comando de defragmentacion.",
                "Defragmentar disco",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                SystemConstants.FILE_SYSTEM_NAME + " Terminal Manager\n"
                + "Interfaz Swing para el simulador Java de sistema de archivos.\n"
                + "La terminal sigue siendo el centro del sistema.",
                "Acerca de " + SystemConstants.FILE_SYSTEM_NAME,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showView(String view) {
        if (DISK_MAP_VIEW.equals(view)) {
            diskMapPanel.refresh();
        }
        cardLayout.show(cards, view);
        if (TERMINAL_VIEW.equals(view)) {
            TerminalPanel active = getActiveTerminal();
            if (active != null) {
                active.requestTerminalFocus();
            }
        }
        refreshStatus();
    }

    private TerminalPanel getActiveTerminal() {
        java.awt.Component selected = terminalTabs.getSelectedComponent();
        if (selected instanceof TerminalPanel terminalPanel) {
            return terminalPanel;
        }
        return null;
    }

    private void toggleTheme() {
        int fontSize = theme.getTerminalFont().getSize();
        TerminalTheme nextTheme = theme.isLightMode() ? TerminalTheme.puttyDark() : TerminalTheme.light();
        applyTheme(nextTheme.withFontSize(fontSize));
    }

    private String themeToggleText() {
        return theme.isLightMode() ? "Cambiar a modo oscuro" : "Cambiar a modo claro";
    }

    private void applyTheme(TerminalTheme theme) {
        this.theme = theme;
        setBackground(theme.getFrameBackground());
        getContentPane().setBackground(theme.getFrameBackground());
        applyFrameTheme();
        terminalTabs.setForeground(Color.BLACK);
        if (themeToggleMenuItem != null) {
            themeToggleMenuItem.setText(themeToggleText());
        }
        GuiStyles.restyleButtons(getContentPane(), theme);
        diskMapPanel.applyTheme(theme);
        settingsPanel.applyTheme(theme);
        for (int index = 0; index < terminalTabs.getTabCount(); index++) {
            java.awt.Component component = terminalTabs.getComponentAt(index);
            if (component instanceof TerminalPanel terminalPanel) {
                terminalPanel.applyTheme(theme);
            }
        }
        refreshStatus();
    }

    private void applyFrameTheme() {
        Color text = theme.getTerminalForeground();
        if (rootPanel != null) {
            rootPanel.setBackground(theme.getFrameBackground());
        }
        if (cards != null) {
            cards.setBackground(theme.getFrameBackground());
        }
        if (splitPane != null) {
            splitPane.setBackground(theme.getFrameBackground());
        }
        if (titleBar != null) {
            titleBar.setBackground(theme.getSurface());
        }
        if (titleLabel != null) {
            titleLabel.setForeground(text);
            titleLabel.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 12f));
        }
        if (versionLabel != null) {
            versionLabel.setForeground(text);
            versionLabel.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 11f));
        }
        if (terminalViewPanel != null) {
            terminalViewPanel.setBackground(theme.getTerminalBackground());
        }
        if (sidebarPanel != null) {
            sidebarPanel.setBackground(theme.getSurface());
            sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, theme.getSurfaceAlt()));
        }
        if (sidebarActionsPanel != null) {
            sidebarActionsPanel.setBackground(theme.getSurface());
        }
        if (sidebarTitleLabel != null) {
            sidebarTitleLabel.setForeground(text);
            sidebarTitleLabel.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 11f));
        }
        if (sidebarFooterLabel != null) {
            sidebarFooterLabel.setForeground(text);
            sidebarFooterLabel.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 11f));
        }
        statusBar.setBackground(theme.getSurface());
        statusBar.setForeground(text);
        statusBar.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 11f));
    }

    private void refreshStatus() {
        TerminalPanel active = getActiveTerminal();
        if (active == null) {
            statusBar.setText("Sin terminal activa");
            return;
        }

        int index = terminalTabs.indexOfComponent(active);
        if (index >= 0) {
            terminalTabs.setTitleAt(index, active.getTabTitle());
            Component tabComponent = terminalTabs.getTabComponentAt(index);
            if (tabComponent instanceof TerminalTabHeader header) {
                header.refreshTitle();
                header.applyTheme(theme);
            }
        }
        statusBar.setText(active.getStatusText());
    }

    private class TerminalTabHeader extends JPanel {
        private final TerminalPanel terminalPanel;
        private final JLabel titleLabel;

        TerminalTabHeader(TerminalPanel terminalPanel) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            this.terminalPanel = terminalPanel;
            this.titleLabel = new JLabel(terminalPanel.getTabTitle());
            setOpaque(false);

            JButton closeButton = new JButton("x");
            GuiStyles.styleTabCloseButton(closeButton, theme);
            closeButton.addActionListener(event -> closeTerminal(terminalPanel));

            add(titleLabel);
            add(closeButton);
            applyTheme(theme);
        }

        void refreshTitle() {
            titleLabel.setText(terminalPanel.getTabTitle());
        }

        void applyTheme(TerminalTheme theme) {
            titleLabel.setForeground(Color.BLACK);
            titleLabel.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 12f));
            GuiStyles.restyleButtons(this, theme);
        }
    }
}
