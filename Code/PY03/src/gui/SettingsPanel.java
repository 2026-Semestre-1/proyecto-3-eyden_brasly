package gui;

import constants.SystemConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

@SuppressWarnings({"serial", "this-escape"})
public class SettingsPanel extends JPanel {
    private final Consumer<TerminalTheme> themeConsumer;
    private TerminalTheme theme;
    private final JTextPane preview;
    private final JSpinner fontSizeSpinner;
    private final JButton themeToggleButton;
    private final List<JLabel> formLabels;
    private JPanel formPanel;
    private TitledBorder previewBorder;

    public SettingsPanel(TerminalTheme initialTheme, Consumer<TerminalTheme> themeConsumer) {
        super(new BorderLayout());
        this.theme = initialTheme;
        this.themeConsumer = themeConsumer;
        this.preview = new JTextPane();
        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(
                initialTheme.getTerminalFont().getSize(),
                10,
                28,
                1
        ));
        this.themeToggleButton = button(themeToggleText(), event -> toggleTheme());
        this.formLabels = new ArrayList<>();
        buildLayout();
        applyPreview();
    }

    private void buildLayout() {
        setBackground(theme.getFrameBackground());

        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(theme.getFrameBackground());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(formPanel, constraints, 0, "Tema", themeToggleButton);
        addRow(formPanel, constraints, 1, "Fondo terminal", button("Cambiar", event -> chooseBackground()));
        addRow(formPanel, constraints, 2, "Texto terminal", button("Cambiar", event -> chooseForeground()));
        addRow(formPanel, constraints, 3, "Color prompt", button("Cambiar", event -> choosePrompt()));
        addRow(formPanel, constraints, 4, "Tamano fuente", fontSizeSpinner);

        JButton applyFontButton = button("Aplicar tamano", event -> {
            int size = ((Number) fontSizeSpinner.getValue()).intValue();
            setTheme(theme.withFontSize(size));
        });
        addRow(formPanel, constraints, 5, "", applyFontButton);

        JButton restoreButton = button("Restaurar valores por defecto", event -> setTheme(TerminalTheme.puttyDark()));
        addRow(formPanel, constraints, 6, "", restoreButton);

        add(formPanel, BorderLayout.CENTER);

        preview.setEditable(false);
        previewBorder = BorderFactory.createTitledBorder("Vista previa");
        previewBorder.setTitleColor(theme.getTerminalForeground());
        preview.setBorder(previewBorder);
        add(preview, BorderLayout.EAST);
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, java.awt.Component input) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(theme.getTerminalForeground());
        formLabels.add(jLabel);
        panel.add(jLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(input, constraints);
    }

    private JButton button(String text, java.awt.event.ActionListener actionListener) {
        JButton button = new JButton(text);
        GuiStyles.styleActionButton(button, theme);
        button.addActionListener(actionListener);
        return button;
    }

    private void chooseBackground() {
        Color selected = JColorChooser.showDialog(this, "Color de fondo de terminal", theme.getTerminalBackground());
        if (selected != null) {
            setTheme(theme.withTerminalBackground(selected));
        }
    }

    private void chooseForeground() {
        Color selected = JColorChooser.showDialog(this, "Color del texto", theme.getTerminalForeground());
        if (selected != null) {
            setTheme(theme.withTerminalForeground(selected));
        }
    }

    private void choosePrompt() {
        Color selected = JColorChooser.showDialog(this, "Color del prompt", theme.getPrompt());
        if (selected != null) {
            setTheme(theme.withPrompt(selected));
        }
    }

    private void toggleTheme() {
        int fontSize = ((Number) fontSizeSpinner.getValue()).intValue();
        TerminalTheme nextTheme = theme.isLightMode() ? TerminalTheme.puttyDark() : TerminalTheme.light();
        setTheme(nextTheme.withFontSize(fontSize));
    }

    private String themeToggleText() {
        return theme.isLightMode() ? "Cambiar a oscuro" : "Cambiar a claro";
    }

    private void setTheme(TerminalTheme theme) {
        applyTheme(theme);
        themeConsumer.accept(theme);
    }

    public void applyTheme(TerminalTheme theme) {
        this.theme = theme;
        setBackground(theme.getFrameBackground());
        if (formPanel != null) {
            formPanel.setBackground(theme.getFrameBackground());
        }
        for (JLabel label : formLabels) {
            label.setForeground(theme.getTerminalForeground());
        }
        if (previewBorder != null) {
            previewBorder.setTitleColor(theme.getTerminalForeground());
        }
        fontSizeSpinner.setValue(theme.getTerminalFont().getSize());
        themeToggleButton.setText(themeToggleText());
        GuiStyles.restyleButtons(this, theme);
        applyPreview();
    }

    private void applyPreview() {
        preview.setBackground(theme.getTerminalBackground());
        preview.setForeground(theme.getTerminalForeground());
        preview.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN));
        StyledDocument document = preview.getStyledDocument();
        try {
            document.remove(0, document.getLength());
            appendPrompt(document, "ls");
            append(document, "[DIR] ", style(theme.getDirectory(), Font.BOLD));
            append(document, "documentos\n", style(theme.getDirectory(), Font.PLAIN));
            append(document, "[FILE] ", style(theme.getFile(), Font.BOLD));
            append(document, "notas.txt\n", style(theme.getFile(), Font.PLAIN));
            appendPrompt(document, "rm sistema.bin");
            append(document, "rm: permiso denegado\n", style(theme.getError(), Font.PLAIN));
            appendPrompt(document, "infoFS");
            append(document, "Bloques usados: 21\n", style(theme.getSuccess(), Font.PLAIN));
        } catch (BadLocationException exception) {
            throw new IllegalStateException("No se pudo actualizar la vista previa.", exception);
        }
    }

    private void appendPrompt(StyledDocument document, String command) throws BadLocationException {
        append(document, "root@" + SystemConstants.FILE_SYSTEM_NAME, style(theme.getPrompt(), Font.BOLD));
        append(document, ":", style(theme.getTerminalForeground(), Font.BOLD));
        append(document, SystemConstants.ROOT_HOME_PATH, style(theme.getDirectory(), Font.BOLD));
        append(document, "$ ", style(theme.getTerminalForeground(), Font.BOLD));
        append(document, command + "\n", style(theme.getTerminalForeground(), Font.PLAIN));
    }

    private void append(StyledDocument document, String text, SimpleAttributeSet style) throws BadLocationException {
        document.insertString(document.getLength(), text, style);
    }

    private SimpleAttributeSet style(Color color, int fontStyle) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontFamily(style, theme.getTerminalFont().getFamily());
        StyleConstants.setFontSize(style, theme.getTerminalFont().getSize());
        StyleConstants.setBold(style, (fontStyle & Font.BOLD) != 0);
        StyleConstants.setItalic(style, (fontStyle & Font.ITALIC) != 0);
        return style;
    }
}
