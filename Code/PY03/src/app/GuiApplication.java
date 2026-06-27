package app;

import constants.SystemConstants;
import gui.TerminalManagerFrame;
import gui.ThreadDispatchPrintStream;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GuiApplication {
    public static void main(String[] args) {
        String diskName = args.length > 0 ? args[0] : SystemConstants.VIRTUAL_DISK_FILE_NAME;
        ThreadDispatchPrintStream outputDispatcher = ThreadDispatchPrintStream.install();

        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            TerminalManagerFrame frame = new TerminalManagerFrame(diskName, outputDispatcher);
            frame.setVisible(true);
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            // Swing can continue with the default look and feel.
        }
    }
}
