import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logic chuyển từ split_obsidian_md.py và spliet_with_page.py. */
public final class MarkdownSplitService {
    private static final DateTimeFormatter DIRECTORY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter BACKUP_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern FRONTMATTER = Pattern.compile(
            "\\A---\\h*\\R(.*?)\\R---\\h*\\R?", Pattern.DOTALL);
    private static final Pattern PAGE_HEADING = Pattern.compile(
            "^\\s*(?:#{1,6}\\s*)?page\\d*\\b.*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s*");

    private MarkdownSplitService() {
    }

    public static SplitResult splitFile(Path inputFile, Path outputParent,
                                        SplitOptions options) throws IOException {
        String originalText = Files.readString(inputFile, StandardCharsets.UTF_8);
        FrontmatterResult extracted = extractFrontmatter(originalText);
        List<String> parts = splitBody(extracted.body(), options);
        if (parts.isEmpty()) {
            throw new IOException("File không có nội dung để tách.");
        }

        Path outputDirectory = createOutputDirectory(inputFile, outputParent);
        List<Path> createdFiles = new ArrayList<>();
        for (int index = 1; index <= parts.size(); index++) {
            String part = parts.get(index - 1);
            String title = detectTitle(part, index);
            String filename = String.format("%03d-%s.md", index, slugifyFilename(title));
            Path childFile = uniquePath(outputDirectory.resolve(filename));
            String childContent = extracted.frontmatter()
                    + "# Câu hỏi " + index + "\n\n"
                    + part.strip() + "\n";
            Files.writeString(childFile, childContent, StandardCharsets.UTF_8);
            createdFiles.add(childFile);
        }

        Path backupFile = null;
        if (options.updateOriginal()) {
//            backupFile = createBackup(inputFile, originalText);
            updateOriginalAsObsidianIndex(inputFile, extracted.frontmatter(), createdFiles);
        }
        return new SplitResult(outputDirectory, createdFiles.size(), backupFile);
    }

    /** Tương đương extract_frontmatter trong Python. */
    static FrontmatterResult extractFrontmatter(String text) {
        Matcher matcher = FRONTMATTER.matcher(text);
        if (!matcher.find()) {
            return new FrontmatterResult("", text);
        }
        String frontmatter = matcher.group().strip() + "\n\n";
        String body = text.substring(matcher.end()).stripLeading();
        return new FrontmatterResult(frontmatter, body);
    }

    /** Kết hợp split_by_separator của cả hai file Python và giới hạn số dòng. */
    static List<String> splitBody(String body, SplitOptions options) {
        List<String> result = new ArrayList<>();
        List<String> current = new ArrayList<>();

        body.lines().forEach(line -> {
            boolean separator = options.splitOnDelimiter() && line.trim().equals("---");
            boolean pageHeading = options.splitOnPageHeading()
                    && PAGE_HEADING.matcher(line).matches();

            if (separator) {
                flushPart(result, current);
                return;
            }
            if (pageHeading && !current.isEmpty()) {
                flushPart(result, current);
            } else if (options.limitLines() && current.size() >= options.maxLines()) {
                flushPart(result, current);
            }
            current.add(line);
        });

        flushPart(result, current);
        return result;
    }

    private static void flushPart(List<String> result, List<String> current) {
        if (current.isEmpty()) {
            return;
        }
        String part = String.join("\n", current).strip();
        if (!part.isEmpty()) {
            result.add(part);
        }
        current.clear();
    }

    /** Tương đương detect_title: ưu tiên dòng chứa "câu hỏi". */
    static String detectTitle(String part, int index) {
        String firstMeaningfulLine = null;
        for (String line : part.lines().toList()) {
            String cleanLine = MARKDOWN_HEADING.matcher(line.strip()).replaceFirst("").strip();
            if (cleanLine.isEmpty()) {
                continue;
            }
            if (firstMeaningfulLine == null) {
                firstMeaningfulLine = cleanLine;
            }
            String normalized = removeVietnameseAccents(cleanLine).toLowerCase(Locale.ROOT);
            if (normalized.contains("cau hoi")) {
                return cleanLine;
            }
        }
        return firstMeaningfulLine != null ? firstMeaningfulLine : "cau hoi " + index;
    }

    /** Tương đương remove_vietnamese_accents. */
    static String removeVietnameseAccents(String text) {
        String replaced = text.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /** Tương đương slugify_filename. */
    static String slugifyFilename(String text) {
        String slug = removeVietnameseAccents(text)
                .strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}_\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            return "noi-dung";
        }
        return slug.length() > 90 ? slug.substring(0, 90) : slug;
    }

    private static Path createOutputDirectory(Path inputFile, Path outputParent)
            throws IOException {
        String folderName = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        return Files.createDirectories(
                outputParent.resolve(folderName)
        );
    }

    private static Path createBackup(Path inputFile, String originalText) throws IOException {
        String backupName = stripExtension(inputFile.getFileName().toString())
                + ".backup-" + LocalDateTime.now().format(BACKUP_TIME) + ".md";
        Path backupFile = uniquePath(inputFile.resolveSibling(backupName));
        Files.writeString(backupFile, originalText, StandardCharsets.UTF_8);
        return backupFile;
    }

    private static void updateOriginalAsObsidianIndex(Path inputFile, String frontmatter,
                                                       List<Path> createdFiles) throws IOException {
        StringBuilder index = new StringBuilder(frontmatter)
                .append("# Mục lục nội dung đã tách\n\n")
                .append("File này đã được tách thành ")
                .append(createdFiles.size()).append(" phần.\n\n")
                .append("## Danh sách file con\n\n");
        for (int i = 0; i < createdFiles.size(); i++) {
            index.append(i + 1).append(". ")
                    .append(makeObsidianLink(createdFiles.get(i))).append("\n");
        }
        index.append("\n---\n\n")
                .append("## Ghi chú\n\n")
                .append("Nội dung gốc đã được tách sang các file con.\n");
        Files.writeString(inputFile, index.toString(), StandardCharsets.UTF_8);
    }

    static String makeObsidianLink(Path childFile) {
        return "[[" + stripExtension(childFile.getFileName().toString()) + "]]";
    }

    private static Path uniquePath(Path desired) {
        if (!Files.exists(desired)) {
            return desired;
        }
        String name = desired.getFileName().toString();
        String stem = stripExtension(name);
        String extension = name.substring(stem.length());
        int counter = 2;
        Path candidate;
        do {
            candidate = desired.resolveSibling(stem + "-" + counter + extension);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    record FrontmatterResult(String frontmatter, String body) {
    }
}
