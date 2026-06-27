package gui;

import constants.SystemConstants;
import filesystem.Bitmap;
import filesystem.FileSystem;
import filesystem.SuperBlock;
import filesystem.nodes.DirectoryNode;
import filesystem.nodes.FileNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

@SuppressWarnings({"serial", "this-escape"})
public class DiskMapPanel extends JPanel {
    private static final Color FREE_COLOR = new Color(0x55, 0xCC, 0x4B);
    private static final Color USED_COLOR = new Color(79, 134, 224);
    private static final Color RESERVED_COLOR = new Color(154, 85, 200);
    private static final Color SELECTED_COLOR = new Color(242, 201, 76);
    private static final Color FRAGMENTED_COLOR = new Color(224, 101, 79);

    private final Supplier<TerminalPanel> terminalSupplier;
    private final DiskGridPanel gridPanel;
    private final DefaultTableModel statsModel;
    private final DefaultTableModel blockDetailsModel;
    private final JTable statsTable;
    private final JTable blockDetailsTable;
    private JPanel headerPanel;
    private JPanel rightPanel;
    private JPanel tablesPanel;
    private JPanel legendPanel;
    private JPanel statsSection;
    private JPanel blockDetailsSection;
    private JPanel statsTitleBar;
    private JPanel blockDetailsTitleBar;
    private JLabel titleLabel;
    private JLabel statsTitleLabel;
    private JLabel blockDetailsTitleLabel;
    private JScrollPane gridScroll;
    private JScrollPane statsScroll;
    private JScrollPane blockDetailsScroll;
    private TerminalTheme theme;
    private DiskSnapshot snapshot;

    public DiskMapPanel(Supplier<TerminalPanel> terminalSupplier) {
        super(new BorderLayout());
        this.terminalSupplier = terminalSupplier;
        this.gridPanel = new DiskGridPanel();
        this.statsModel = infoModel();
        this.blockDetailsModel = infoModel();
        this.statsTable = new JTable(statsModel);
        this.blockDetailsTable = new JTable(blockDetailsModel);
        this.theme = TerminalTheme.puttyDark();
        buildLayout();
    }

    private void buildLayout() {
        setBackground(theme.getFrameBackground());

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        titleLabel = new JLabel("Mapa de Disco");
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        actions.setOpaque(false);
        JButton refreshButton = new JButton("Actualizar mapa");
        JButton defragButton = new JButton("Defragmentar disco");
        JButton detailsButton = new JButton("Detalles del bloque");
        GuiStyles.styleActionButton(refreshButton, theme);
        GuiStyles.styleActionButton(defragButton, theme);
        GuiStyles.styleActionButton(detailsButton, theme);
        refreshButton.addActionListener(event -> refresh());
        defragButton.addActionListener(event -> updateTable(blockDetailsModel, new String[][]{
            {"Defragmentar", "Pendiente en el core"},
            {"Detalle", "Vista preparada para el comando final"}
        }));
        detailsButton.addActionListener(event -> showSelectedBlockDetails());
        actions.add(refreshButton);
        actions.add(defragButton);
        actions.add(detailsButton);
        headerPanel.add(actions, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        gridScroll = new JScrollPane(gridPanel);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        add(gridScroll, BorderLayout.CENTER);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(380, 0));
        tablesPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        tablesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        statsTitleLabel = new JLabel("Estadisticas");
        blockDetailsTitleLabel = new JLabel("Bloque seleccionado");
        statsSection = section(statsTitleLabel, statsTable, true);
        blockDetailsSection = section(blockDetailsTitleLabel, blockDetailsTable, false);
        tablesPanel.add(statsSection);
        tablesPanel.add(blockDetailsSection);

        rightPanel.add(tablesPanel, BorderLayout.CENTER);
        rightPanel.add(buildLegend(), BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
        applyTheme(theme);
    }

    public void applyTheme(TerminalTheme theme) {
        this.theme = theme;
        setBackground(theme.getFrameBackground());
        if (headerPanel != null) {
            headerPanel.setBackground(theme.getSurface());
        }
        if (titleLabel != null) {
            titleLabel.setForeground(theme.getPrompt());
            titleLabel.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 14f));
        }
        if (rightPanel != null) {
            rightPanel.setBackground(theme.getSurface());
            rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, theme.getSurfaceAlt()));
        }
        if (tablesPanel != null) {
            tablesPanel.setBackground(theme.getSurface());
        }
        gridPanel.setBackground(theme.getFrameBackground());
        if (gridScroll != null) {
            gridScroll.getViewport().setBackground(theme.getFrameBackground());
        }
        applySectionTheme(statsSection, statsTitleBar, statsTitleLabel);
        applySectionTheme(blockDetailsSection, blockDetailsTitleBar, blockDetailsTitleLabel);
        styleInfoTable(statsTable);
        styleInfoTable(blockDetailsTable);
        if (legendPanel != null) {
            legendPanel.setBackground(theme.getSurface());
        }
        GuiStyles.restyleButtons(this, theme);
        repaint();
    }

    private JPanel buildLegend() {
        legendPanel = new JPanel(new GridLayout(5, 1, 4, 4));
        legendPanel.setBackground(theme.getSurface());
        legendPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        legendPanel.add(legendItem("Disponible", FREE_COLOR));
        legendPanel.add(legendItem("Ocupado", USED_COLOR));
        legendPanel.add(legendItem("Reservado", RESERVED_COLOR));
        legendPanel.add(legendItem("Archivo seleccionado", SELECTED_COLOR));
        legendPanel.add(legendItem("Fragmentado", FRAGMENTED_COLOR));
        return legendPanel;
    }

    private JLabel legendItem(String label, Color color) {
        JLabel item = new JLabel("  " + label);
        item.setOpaque(true);
        item.setBackground(color);
        item.setForeground(Color.BLACK);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        return item;
    }

    private JPanel section(JLabel title, JTable table, boolean stats) {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel titleBar = new JPanel(new BorderLayout());
        title.setBorder(BorderFactory.createEmptyBorder(6, 9, 6, 9));
        titleBar.add(title, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (stats) {
            statsTitleBar = titleBar;
            statsScroll = scrollPane;
        } else {
            blockDetailsTitleBar = titleBar;
            blockDetailsScroll = scrollPane;
        }

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel infoModel() {
        return new DefaultTableModel(new String[]{"Campo", "Valor"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void updateTable(DefaultTableModel model, String[][] rows) {
        model.setRowCount(0);
        for (String[] row : rows) {
            model.addRow(row);
        }
    }

    private void applySectionTheme(JPanel section, JPanel titleBar, JLabel title) {
        if (section == null || titleBar == null || title == null) {
            return;
        }

        Color titleBackground = theme.isLightMode() ? new Color(76, 86, 106) : theme.getSurfaceAlt();
        section.setBackground(theme.getSurface());
        section.setBorder(BorderFactory.createLineBorder(theme.getSurfaceAlt()));
        titleBar.setBackground(titleBackground);
        title.setForeground(Color.WHITE);
        title.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 12f));
    }

    private void styleInfoTable(JTable table) {
        table.setFont(theme.getTerminalFont().deriveFont(Font.PLAIN, 12f));
        table.setRowHeight(Math.max(24, theme.getTerminalFont().getSize() + 10));
        table.setBackground(theme.getTerminalBackground());
        table.setForeground(theme.getTerminalForeground());
        table.setGridColor(theme.getSurfaceAlt());
        table.setSelectionBackground(theme.getSurfaceAlt());
        table.setSelectionForeground(theme.getTerminalForeground());
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setDefaultRenderer(Object.class, new InfoTableCellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setBackground(tableHeaderBackground());
        header.setForeground(Color.WHITE);
        header.setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 12f));
        header.setDefaultRenderer(new InfoTableHeaderRenderer());
        header.setReorderingAllowed(false);

        JScrollPane parentScroll = table == statsTable ? statsScroll : blockDetailsScroll;
        if (parentScroll != null) {
            parentScroll.getViewport().setBackground(theme.getTerminalBackground());
            parentScroll.setBackground(theme.getSurface());
            parentScroll.setColumnHeaderView(header);
            if (parentScroll.getColumnHeader() != null) {
                parentScroll.getColumnHeader().setBackground(tableHeaderBackground());
            }
        }
    }

    private Color tableHeaderBackground() {
        return theme.isLightMode() ? new Color(76, 86, 106) : theme.getSurfaceAlt();
    }

    private Color valueColor(String field, String value) {
        String normalizedField = normalize(field);
        String normalizedValue = normalize(value);
        if (normalizedField.contains("disponible") || normalizedField.contains("libres")
                || normalizedValue.equals("libre") || normalizedValue.equals("disponible")) {
            return FREE_COLOR;
        }
        if (normalizedField.contains("usado") || normalizedField.contains("ocupados")
                || normalizedField.equals("uso") || normalizedValue.equals("ocupado")) {
            return USED_COLOR;
        }
        if (normalizedField.contains("fragmentacion") || normalizedField.contains("fragmentados")
                || normalizedValue.equals("fragmentado")) {
            return FRAGMENTED_COLOR;
        }
        if (normalizedValue.equals("reservado")) {
            return RESERVED_COLOR;
        }
        if (normalizedField.contains("archivo asociado") && !"-".equals(value)) {
            return SELECTED_COLOR;
        }
        if (normalizedField.contains("file system")) {
            return theme.getPrompt();
        }
        return theme.getTerminalForeground();
    }

    private boolean isStrongValue(String field, String value) {
        return !valueColor(field, value).equals(theme.getTerminalForeground());
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase()
                        .replace('\u00e1', 'a')
                        .replace('\u00e9', 'e')
                        .replace('\u00ed', 'i')
                        .replace('\u00f3', 'o')
                        .replace('\u00fa', 'u');
    }

    public void refresh() {
        TerminalPanel terminal = terminalSupplier.get();
        if (terminal == null || terminal.getSession().getFileSystem() == null) {
            snapshot = DiskSnapshot.empty("No hay File System montado en la terminal activa.");
        } else {
            snapshot = DiskSnapshot.from(terminal.getSession().getFileSystem());
        }

        updateTable(statsModel, snapshot.statsRows());
        updateTable(blockDetailsModel, snapshot.selectedBlockRows());
        gridPanel.setSnapshot(snapshot);
    }

    private void showSelectedBlockDetails() {
        if (snapshot == null) {
            refresh();
        }
        updateTable(blockDetailsModel, snapshot.selectedBlockRows());
    }

    @SuppressWarnings("serial")
    private class DiskGridPanel extends JPanel {
        private static final int CELL_SIZE = 9;
        private static final int CELL_GAP = 2;
        private DiskSnapshot snapshot = DiskSnapshot.empty("No hay File System montado.");

        DiskGridPanel() {
            setBackground(new Color(31, 38, 51));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    selectBlock(event.getPoint());
                }
            });
        }

        void setSnapshot(DiskSnapshot snapshot) {
            this.snapshot = snapshot;
            int columns = columnsForWidth(Math.max(getWidth(), 760));
            int rows = (int) Math.ceil(snapshot.totalBlocks() / (double) columns);
            int width = columns * (CELL_SIZE + CELL_GAP) + 24;
            int height = rows * (CELL_SIZE + CELL_GAP) + 24;
            setPreferredSize(new Dimension(width, Math.max(height, 420)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int columns = columnsForWidth(getWidth());
            for (int block = 0; block < snapshot.totalBlocks(); block++) {
                int row = block / columns;
                int column = block % columns;
                int x = 12 + column * (CELL_SIZE + CELL_GAP);
                int y = 12 + row * (CELL_SIZE + CELL_GAP);
                graphics2D.setColor(snapshot.colorFor(block));
                graphics2D.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            }

            graphics2D.dispose();
        }

        private void selectBlock(Point point) {
            int columns = columnsForWidth(getWidth());
            int column = (point.x - 12) / (CELL_SIZE + CELL_GAP);
            int row = (point.y - 12) / (CELL_SIZE + CELL_GAP);

            if (column < 0 || row < 0) {
                return;
            }

            int block = row * columns + column;
            if (block >= snapshot.totalBlocks()) {
                return;
            }

            snapshot.selectBlock(block);
            updateTable(blockDetailsModel, snapshot.selectedBlockRows());
            repaint();
        }

        private int columnsForWidth(int width) {
            return Math.max(16, (width - 24) / (CELL_SIZE + CELL_GAP));
        }
    }

    private static class DiskSnapshot {
        private final int totalBlocks;
        private final int usedBlocks;
        private final int freeBlocks;
        private final long totalBytes;
        private final int blockSize;
        private final boolean[] used;
        private final boolean[] reserved;
        private final boolean[] fragmented;
        private final Map<Integer, FileNode> ownersByBlock;
        private final Set<Integer> selectedFileBlocks;
        private final int fragmentedFiles;
        private final String message;
        private int selectedBlock;

        private DiskSnapshot(
                int totalBlocks,
                int usedBlocks,
                int freeBlocks,
                long totalBytes,
                int blockSize,
                boolean[] used,
                boolean[] reserved,
                boolean[] fragmented,
                Map<Integer, FileNode> ownersByBlock,
                Set<Integer> selectedFileBlocks,
                int fragmentedFiles,
                String message
        ) {
            this.totalBlocks = totalBlocks;
            this.usedBlocks = usedBlocks;
            this.freeBlocks = freeBlocks;
            this.totalBytes = totalBytes;
            this.blockSize = blockSize;
            this.used = used;
            this.reserved = reserved;
            this.fragmented = fragmented;
            this.ownersByBlock = ownersByBlock;
            this.selectedFileBlocks = selectedFileBlocks;
            this.fragmentedFiles = fragmentedFiles;
            this.message = message;
            this.selectedBlock = 0;
        }

        static DiskSnapshot empty(String message) {
            boolean[] used = new boolean[1];
            boolean[] reserved = new boolean[1];
            boolean[] fragmented = new boolean[1];
            return new DiskSnapshot(1, 0, 1, 0, SystemConstants.VIRTUAL_DISK_BLOCK_SIZE,
                    used, reserved, fragmented, new HashMap<>(), new HashSet<>(), 0, message);
        }

        static DiskSnapshot from(FileSystem fileSystem) {
            Bitmap bitmap = fileSystem.getBitmap();
            SuperBlock superBlock = fileSystem.getSuperBlock();
            int totalBlocks = bitmap.getTotalBlocks();
            boolean[] used = new boolean[totalBlocks];
            boolean[] reserved = new boolean[totalBlocks];
            boolean[] fragmented = new boolean[totalBlocks];

            for (int block = 0; block < totalBlocks; block++) {
                used[block] = !bitmap.isFree(block);
                reserved[block] = block < SystemConstants.DATA_START_BLOCK;
            }

            Map<Integer, FileNode> ownersByBlock = new HashMap<>();
            List<FileNode> files = new ArrayList<>();
            collectFiles(fileSystem.getDirectoryTree().getRoot(), files);

            int fragmentedFiles = 0;
            for (FileNode file : files) {
                List<Integer> blocks = file.getFCB().getBlocks();
                for (Integer block : blocks) {
                    if (block != null && block >= 0 && block < totalBlocks) {
                        ownersByBlock.put(block, file);
                    }
                }

                if (isFragmented(blocks)) {
                    fragmentedFiles++;
                    for (Integer block : blocks) {
                        if (block != null && block >= 0 && block < totalBlocks) {
                            fragmented[block] = true;
                        }
                    }
                }
            }

            return new DiskSnapshot(
                    totalBlocks,
                    bitmap.countUsedBlocks(),
                    bitmap.countFreeBlocks(),
                    superBlock.getTotalSizeBytes(),
                    superBlock.getBlockSize(),
                    used,
                    reserved,
                    fragmented,
                    ownersByBlock,
                    new HashSet<>(),
                    fragmentedFiles,
                    ""
            );
        }

        int totalBlocks() {
            return totalBlocks;
        }

        Color colorFor(int block) {
            if (selectedFileBlocks.contains(block)) {
                return SELECTED_COLOR;
            }
            if (reserved[block]) {
                return RESERVED_COLOR;
            }
            if (fragmented[block]) {
                return FRAGMENTED_COLOR;
            }
            if (used[block]) {
                return USED_COLOR;
            }
            return FREE_COLOR;
        }

        void selectBlock(int block) {
            selectedBlock = block;
            selectedFileBlocks.clear();
            FileNode selectedFile = ownersByBlock.get(block);
            if (selectedFile != null) {
                selectedFileBlocks.addAll(selectedFile.getFCB().getBlocks());
            }
        }

        String[][] statsRows() {
            if (!message.isBlank()) {
                return new String[][]{{"Mensaje", message}};
            }

            long usedBytes = (long) usedBlocks * blockSize;
            long freeBytes = (long) freeBlocks * blockSize;
            double usage = totalBlocks == 0 ? 0.0 : usedBlocks * 100.0 / totalBlocks;
            double fragmentation = usedBlocks == 0 ? 0.0 : fragmentedBlockCount() * 100.0 / usedBlocks;

            return new String[][]{
                {"Nombre del File System", SystemConstants.FILE_SYSTEM_NAME},
                {"Tamano total", totalBytes + " bytes"},
                {"Espacio usado", usedBytes + " bytes"},
                {"Espacio disponible", freeBytes + " bytes"},
                {"Bloques totales", String.valueOf(totalBlocks)},
                {"Bloques libres", String.valueOf(freeBlocks)},
                {"Bloques ocupados", String.valueOf(usedBlocks)},
                {"Uso", String.format("%.2f%%", usage)},
                {"Fragmentacion", String.format("%.2f%%", fragmentation)},
                {"Archivos fragmentados", String.valueOf(fragmentedFiles)}
            };
        }

        String[][] selectedBlockRows() {
            if (!message.isBlank()) {
                return new String[][]{{"Mensaje", message}};
            }

            FileNode owner = ownersByBlock.get(selectedBlock);
            String state = reserved[selectedBlock] ? "Reservado"
                    : fragmented[selectedBlock] ? "Fragmentado"
                    : used[selectedBlock] ? "Ocupado"
                    : "Libre";

            return new String[][]{
                {"Numero de bloque", String.valueOf(selectedBlock)},
                {"Estado", state},
                {"Archivo asociado", owner == null ? "-" : owner.getFullPath()},
                {"Dueno", owner == null ? "-" : owner.getFCB().getOwner()},
                {"Tamano del archivo", owner == null ? "-" : owner.getFCB().getSize() + " bytes"},
                {"Abierto", owner == null ? "-" : owner.isOpen() ? "si" : "no"}
            };
        }

        private int fragmentedBlockCount() {
            int count = 0;
            for (boolean value : fragmented) {
                if (value) {
                    count++;
                }
            }
            return count;
        }

        private static void collectFiles(DirectoryNode directory, List<FileNode> files) {
            files.addAll(directory.getFiles());
            for (DirectoryNode child : directory.getDirectories()) {
                collectFiles(child, files);
            }
        }

        private static boolean isFragmented(List<Integer> blocks) {
            if (blocks == null || blocks.size() < 2) {
                return false;
            }

            for (int index = 1; index < blocks.size(); index++) {
                if (blocks.get(index) != blocks.get(index - 1) + 1) {
                    return true;
                }
            }

            return false;
        }
    }

    private class InfoTableHeaderRenderer extends DefaultTableCellRenderer {
        InfoTableHeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getFrameBackground()),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(tableHeaderBackground());
            setForeground(Color.WHITE);
            setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 12f));
            return this;
        }
    }

    private class InfoTableCellRenderer extends DefaultTableCellRenderer {
        InfoTableCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String field = String.valueOf(table.getValueAt(row, 0));
            String text = value == null ? "" : String.valueOf(value);

            setBackground(isSelected ? theme.getSurfaceAlt() : theme.getTerminalBackground());
            if (column == 1) {
                setForeground(valueColor(field, text));
                setFont(theme.getTerminalFont().deriveFont(
                        isStrongValue(field, text) ? Font.BOLD : Font.PLAIN,
                        12f
                ));
            } else {
                setForeground(theme.getTerminalForeground());
                setFont(theme.getTerminalFont().deriveFont(Font.BOLD, 12f));
            }
            return this;
        }
    }
}
