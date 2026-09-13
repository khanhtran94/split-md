import java.nio.file.Path;

public record SplitResult(Path outputDirectory, int fileCount, Path backupFile) {
}
