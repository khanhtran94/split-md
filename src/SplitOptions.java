public record SplitOptions(
        boolean splitOnDelimiter,
        boolean splitOnPageHeading,
        boolean limitLines,
        int maxLines,
        boolean updateOriginal
) {
    public SplitOptions {
        if (limitLines && maxLines < 1) {
            throw new IllegalArgumentException("maxLines phải lớn hơn 0");
        }
    }
}
