package gui;

import constants.SystemConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

final class GuiStyles {
    private static final String PROPERTY_PREFIX = SystemConstants.FILE_SYSTEM_NAME + ".";
    private static final String HAS_HOVER = PROPERTY_PREFIX + "hasHover";
    private static final String DEFAULT_BG = PROPERTY_PREFIX + "defaultBg";
    private static final String HOVER_BG = PROPERTY_PREFIX + "hoverBg";
    private static final String PRESSED_BG = PROPERTY_PREFIX + "pressedBg";
    private static final String DEFAULT_FG = PROPERTY_PREFIX + "defaultFg";
    private static final String VARIANT = PROPERTY_PREFIX + "variant";
    private static final String ACTION = "action";
    private static final String SIDEBAR = "sidebar";
    private static final String TAB_CLOSE = "tabClose";

    private GuiStyles() {
    }

    static void styleActionButton(JButton button, TerminalTheme theme) {
        button.putClientProperty(VARIANT, ACTION);
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getSurfaceAlt()),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );
        applyButtonStyle(
                button,
                theme,
                darken(theme.getSurfaceAlt(), 18),
                theme.getSurfaceAlt(),
                darken(theme.getSurfaceAlt(), 30),
                theme.getTerminalForeground(),
                border,
                SwingConstants.CENTER
        );
    }

    static void styleSidebarButton(JButton button, TerminalTheme theme) {
        button.putClientProperty(VARIANT, SIDEBAR);
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, theme.getSurface()),
                BorderFactory.createEmptyBorder(7, 10, 7, 8)
        );

        applyButtonStyle(
                button,
                theme,
                darken(theme.getSurface(), 16),
                darken(theme.getSurfaceAlt(), 8),
                darken(theme.getSurface(), 28),
                theme.getTerminalForeground(),
                border,
                SwingConstants.LEFT
        );
    }

    static void styleTabCloseButton(JButton button, TerminalTheme theme) {
        button.putClientProperty(VARIANT, TAB_CLOSE);
        Border border = BorderFactory.createEmptyBorder(0, 5, 0, 5);
        applyButtonStyle(
                button,
                theme,
                theme.getSurface(),
                theme.getError(),
                darken(theme.getError(), 18),
                theme.getTerminalForeground(),
                border,
                SwingConstants.CENTER
        );
        button.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 11f));
    }

    static void restyleButtons(Component component, TerminalTheme theme) {
        if (component instanceof JButton button) {
            Object variant = button.getClientProperty(VARIANT);
            if (SIDEBAR.equals(variant)) {
                styleSidebarButton(button, theme);
            } else if (TAB_CLOSE.equals(variant)) {
                styleTabCloseButton(button, theme);
            } else {
                styleActionButton(button, theme);
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                restyleButtons(child, theme);
            }
        }
    }

    private static void applyButtonStyle(
            JButton button,
            TerminalTheme theme,
            Color background,
            Color hoverBackground,
            Color pressedBackground,
            Color foreground,
            Border border,
            int horizontalAlignment
    ) {
        button.putClientProperty(DEFAULT_BG, background);
        button.putClientProperty(HOVER_BG, hoverBackground);
        button.putClientProperty(PRESSED_BG, pressedBackground);
        button.putClientProperty(DEFAULT_FG, foreground);

        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(border);
        button.setHorizontalAlignment(horizontalAlignment);
        button.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 12f));

        if (!Boolean.TRUE.equals(button.getClientProperty(HAS_HOVER))) {
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    button.setBackground((Color) button.getClientProperty(HOVER_BG));
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    button.setBackground((Color) button.getClientProperty(DEFAULT_BG));
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    button.setBackground((Color) button.getClientProperty(PRESSED_BG));
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    Color color = button.contains(event.getPoint())
                            ? (Color) button.getClientProperty(HOVER_BG)
                            : (Color) button.getClientProperty(DEFAULT_BG);
                    button.setBackground(color);
                }
            });
            button.putClientProperty(HAS_HOVER, true);
        }
    }

    private static Color brighten(Color color, int amount) {
        return new Color(
                clamp(color.getRed() + amount),
                clamp(color.getGreen() + amount),
                clamp(color.getBlue() + amount)
        );
    }

    private static Color darken(Color color, int amount) {
        return new Color(
                clamp(color.getRed() - amount),
                clamp(color.getGreen() - amount),
                clamp(color.getBlue() - amount)
        );
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
