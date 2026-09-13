import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Chỉ phụ trách giao diện. Logic tách nằm trong MarkdownSplitService. */
public class MarkdownSplitter extends JFrame {
    private final JTextField inputField = new JTextField();
//    private final JTextField outputField = new JTextField();
    private final JCheckBox delimiterRule = new JCheckBox("Tách khi gặp dòng ---", true);
    private final JCheckBox pageRule = new JCheckBox("Tách trước tiêu đề Page... / # Page...", true);
    private final JCheckBox lineRule = new JCheckBox("Giới hạn số dòng trong mỗi file", false);
    private final JCheckBox updateOriginal = new JCheckBox(
            "Cập nhật file gốc thành mục lục Obsidian", false);
    private final JSpinner maxLinesSpinner = new JSpinner(
            new SpinnerNumberModel(500, 1, 1_000_000, 50));
    private final JButton splitButton = new JButton("Split file");
    private final JLabel statusLabel = new JLabel("Chọn file Markdown cần tách.");
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of(
            System.getProperty("user.home"),
            "Documents",
            "obsidian-note",
            "note",
            "02-References",
            "ai-extracts"
    );
    public MarkdownSplitter() {
        super("Markdown File Splitter");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setLocationByPlatform(true);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        JLabel title = new JLabel("Markdown File Splitter");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(16));

        root.add(pathRow("File .md:", inputField, "Browse...", this::chooseInput));
//        root.add(Box.createVerticalStrut(10));
//        root.add(pathRow("Nơi lưu:", outputField, "Chọn thư mục...", this::chooseOutput));
        root.add(Box.createVerticalStrut(18));

        JPanel rules = new JPanel();
        rules.setLayout(new BoxLayout(rules, BoxLayout.Y_AXIS));
        rules.setBorder(BorderFactory.createTitledBorder("Tiêu chí tách"));
        rules.setAlignmentX(Component.LEFT_ALIGNMENT);
        rules.add(delimiterRule);
        rules.add(pageRule);

        JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linePanel.add(lineRule);
        linePanel.add(Box.createHorizontalStrut(10));
        linePanel.add(maxLinesSpinner);
        linePanel.add(new JLabel(" dòng"));
        rules.add(linePanel);
        rules.add(updateOriginal);
        root.add(rules);
        root.add(Box.createVerticalStrut(16));

        splitButton.setFont(splitButton.getFont().deriveFont(Font.BOLD, 14f));
        splitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        splitButton.addActionListener(e -> splitInBackground());
        root.add(splitButton);
        root.add(Box.createVerticalStrut(14));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(statusLabel);
    }

    private JPanel pathRow(String label, JTextField field, String buttonText, Runnable action) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pathLabel = new JLabel(label);
        pathLabel.setPreferredSize(new Dimension(72, 28));
        JButton button = new JButton(buttonText);
        button.addActionListener(e -> action.run());
        panel.add(pathLabel, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        return panel;
    }

    private void chooseInput() {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(
                new FileNameExtensionFilter("Markdown (*.md)", "md")
        );

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path input = chooser.getSelectedFile()
                    .toPath()
                    .toAbsolutePath();

            inputField.setText(input.toString());
        }
    }

//    private void chooseOutput() {
//        JFileChooser chooser = new JFileChooser();
//        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        if (!outputField.getText().isBlank()) {
//            chooser.setCurrentDirectory(Path.of(outputField.getText()).toFile());
//        }
//        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//            outputField.setText(chooser.getSelectedFile().toPath().toAbsolutePath().toString());
//        }
//    }

    private void splitInBackground() {
        final Path input;
        final Path outputParent;
        try {
            input = Path.of(inputField.getText().trim()).toAbsolutePath();
//            outputParent = outputField.getText().isBlank()
//                    ? input.getParent()
//                    : Path.of(outputField.getText().trim()).toAbsolutePath();
//            validateInputs(input, outputParent);

            outputParent = DEFAULT_OUTPUT_DIRECTORY;

            Files.createDirectories(outputParent);

            validateInputs(input, outputParent);
        } catch (Exception ex) {
            showError(ex.getMessage());
            return;
        }

        if (!delimiterRule.isSelected() && !pageRule.isSelected() && !lineRule.isSelected()) {
            showError("Hãy bật ít nhất một tiêu chí tách.");
            return;
        }

        SplitOptions options = new SplitOptions(
                delimiterRule.isSelected(), pageRule.isSelected(), lineRule.isSelected(),
                (Integer) maxLinesSpinner.getValue(), updateOriginal.isSelected());

        splitButton.setEnabled(false);
        statusLabel.setText("Đang xử lý...");

        SwingWorker<SplitResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SplitResult doInBackground() throws Exception {
                return MarkdownSplitService.splitFile(input, outputParent, options);
            }

            @Override
            protected void done() {
                splitButton.setEnabled(true);
                try {
                    SplitResult result = get();
                    statusLabel.setText("Hoàn tất: " + result.fileCount()
                            + " file — " + result.outputDirectory());
                    String message = "Đã tạo " + result.fileCount() + " file tại:\n"
                            + result.outputDirectory();
                    if (result.backupFile() != null) {
                        message += "\n\nBackup file gốc:\n" + result.backupFile();
                    }
                    JOptionPane.showMessageDialog(MarkdownSplitter.this, message,
                            "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(result.outputDirectory().toFile());
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    statusLabel.setText("Không thể tách file.");
                    showError(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void validateInputs(Path input, Path outputParent) {
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("Đường dẫn file không tồn tại hoặc không phải file.");
        }
        if (!input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file có đuôi .md.");
        }
        if (outputParent == null || !Files.isDirectory(outputParent)) {
            throw new IllegalArgumentException("Thư mục lưu kết quả không tồn tại.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message == null ? "Lỗi không xác định." : message,
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MarkdownSplitter().setVisible(true);
        });
    }
}
