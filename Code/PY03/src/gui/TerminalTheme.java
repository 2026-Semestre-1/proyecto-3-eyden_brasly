package gui;

import java.awt.Color;
import java.awt.Font;

public class TerminalTheme {
    private final Color frameBackground;
    private final Color surface;
    private final Color surfaceAlt;
    private final Color terminalBackground;
    private final Color terminalForeground;
    private final Color prompt;
    private final Color error;
    private final Color warning;
    private final Color success;
    private final Color directory;
    private final Color file;
    private final Font terminalFont;

    public TerminalTheme(
            Color frameBackground,
            Color surface,
            Color surfaceAlt,
            Color terminalBackground,
            Color terminalForeground,
            Color prompt,
            Color error,
            Color warning,
            Color success,
            Color directory,
            Color file,
            Font terminalFont
    ) {
        this.frameBackground = frameBackground;
        this.surface = surface;
        this.surfaceAlt = surfaceAlt;
        this.terminalBackground = terminalBackground;
        this.terminalForeground = terminalForeground;
        this.prompt = prompt;
        this.error = error;
        this.warning = warning;
        this.success = success;
        this.directory = directory;
        this.file = file;
        this.terminalFont = terminalFont;
    }

    public static TerminalTheme puttyDark() {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        return new TerminalTheme(
                new Color(31, 38, 51),
                new Color(37, 45, 59),
                new Color(45, 55, 71),
                new Color(8, 13, 25),
                new Color(238, 238, 236),
                new Color(78, 154, 6),
                new Color(239, 41, 41),
                new Color(252, 175, 62),
                new Color(138, 226, 52),
                new Color(114, 159, 207),
                new Color(238, 238, 236),
                font
        );
    }

    public static TerminalTheme light() {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        return new TerminalTheme(
                new Color(238, 241, 245),
                new Color(226, 231, 238),
                new Color(214, 221, 231),
                new Color(250, 250, 250),
                new Color(35, 38, 42),
                new Color(0, 112, 60),
                new Color(174, 34, 34),
                new Color(160, 105, 0),
                new Color(0, 112, 60),
                new Color(0, 86, 145),
                new Color(35, 38, 42),
                font
        );
    }

    public TerminalTheme withTerminalBackground(Color color) {
        return new TerminalTheme(frameBackground, surface, surfaceAlt, color, terminalForeground, prompt,
                error, warning, success, directory, file, terminalFont);
    }

    public TerminalTheme withTerminalForeground(Color color) {
        return new TerminalTheme(frameBackground, surface, surfaceAlt, terminalBackground, color, prompt,
                error, warning, success, directory, file, terminalFont);
    }

    public TerminalTheme withPrompt(Color color) {
        return new TerminalTheme(frameBackground, surface, surfaceAlt, terminalBackground, terminalForeground,
                color, error, warning, success, directory, file, terminalFont);
    }

    public TerminalTheme withFontSize(int size) {
        Font font = terminalFont.deriveFont((float) size);
        return new TerminalTheme(frameBackground, surface, surfaceAlt, terminalBackground, terminalForeground,
                prompt, error, warning, success, directory, file, font);
    }

    public boolean isLightMode() {
        return perceivedBrightness(terminalBackground) >= 128;
    }

    private static int perceivedBrightness(Color color) {
        return (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000;
    }

    public Color getFrameBackground() {
        return frameBackground;
    }

    public Color getSurface() {
        return surface;
    }

    public Color getSurfaceAlt() {
        return surfaceAlt;
    }

    public Color getTerminalBackground() {
        return terminalBackground;
    }

    public Color getTerminalForeground() {
        return terminalForeground;
    }

    public Color getPrompt() {
        return prompt;
    }

    public Color getError() {
        return error;
    }

    public Color getWarning() {
        return warning;
    }

    public Color getSuccess() {
        return success;
    }

    public Color getDirectory() {
        return directory;
    }

    public Color getFile() {
        return file;
    }

    public Font getTerminalFont() {
        return terminalFont;
    }
}
