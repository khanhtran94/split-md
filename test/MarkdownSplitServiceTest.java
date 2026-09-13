import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MarkdownSplitServiceTest {
    public static void main(String[] args) throws Exception {
        testCombinedRulesAndFrontmatter();
        testVietnameseSlug();
        System.out.println("All tests passed.");
    }

    private static void testCombinedRulesAndFrontmatter() throws Exception {
        Path temp = Files.createTempDirectory("markdown-split-test-");
        Path input = temp.resolve("ghi-chu.md");
        Files.writeString(input, """
                ---
                tags: [java]
                ---
                # Mở đầu
                Nội dung chung
                ---
                # Page1: Java
                ## Câu hỏi Java là gì?
                Trả lời
                Page 2
                Nội dung trang hai
                """, StandardCharsets.UTF_8);

        SplitOptions options = new SplitOptions(true, true, false, 500, false);
        SplitResult result = MarkdownSplitService.splitFile(input, temp, options);
        assertEquals(3, result.fileCount(), "Số file con");

        List<Path> files;
        try (var stream = Files.list(result.outputDirectory())) {
            files = stream.sorted().toList();
        }
        String second = Files.readString(files.get(1), StandardCharsets.UTF_8);
        assertTrue(second.startsWith("---\ntags: [java]\n---\n\n# Câu hỏi 2"),
                "Mỗi file phải giữ YAML frontmatter");
        assertTrue(second.contains("# Page1: Java"), "Phải giữ tiêu đề Page");
    }

    private static void testVietnameseSlug() {
        assertEquals("cau-hoi-lap-trinh-roblox",
                MarkdownSplitService.slugifyFilename("Câu hỏi lập trình Roblox"),
                "Slug tiếng Việt");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
