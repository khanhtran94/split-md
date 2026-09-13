# Markdown File Splitter cho Windows

Ứng dụng Java Swing nhỏ để chọn một file `.md` bằng đường dẫn hoặc nút **Browse**, rồi tách thành nhiều file Markdown. Logic đã được chuyển từ `split_obsidian_md.py` và `spliet_with_page.py`.

## Quy tắc tách

Ba quy tắc có thể bật/tắt độc lập và được áp dụng đồng thời:

1. **Dòng `---`**: kết thúc phần hiện tại; dòng `---` không được ghi vào file con. YAML frontmatter ở đầu tài liệu không bị dùng làm dấu tách.
2. **Tiêu đề Page**: nhận cả `Page 1`, `PAGE 2`, `# Page3`, `## Page8: Tiêu đề`... Tiêu đề được giữ trong file mới.
3. **Giới hạn số dòng**: mặc định 500 dòng/file và có thể sửa trên giao diện.

Giống hai script Python, ứng dụng còn:

- Sao chép YAML frontmatter vào từng file con.
- Ưu tiên dòng chứa `Câu hỏi` để đặt tên file.
- Chuyển tên tiếng Việt thành slug không dấu.
- Đặt tên file theo dạng `001-ten-noi-dung.md`.
- Thêm `# Câu hỏi N` vào đầu nội dung mỗi file con.
- Có tùy chọn cập nhật file gốc thành mục lục Obsidian `[[file-con]]`; app luôn backup trước.

Mỗi lần chạy, app tạo thư mục mới cạnh nơi lưu đã chọn, ví dụ:

```text
TaiLieu_split_20260913_153012_123
```

Việc này giúp tránh ghi đè kết quả của lần chạy trước.

## Chạy trên Windows

Yêu cầu: cài **JDK 17 trở lên**, sau đó kiểm tra bằng lệnh:

```bat
java -version
javac -version
```

Giải nén dự án, sau đó nhấp đúp:

```text
run.bat
```

Lần đầu, `run.bat` sẽ tự gọi `build.bat` để tạo `MarkdownFileSplitter.jar` rồi mở ứng dụng.

## Build lại

Nhấp đúp `build.bat`, hoặc chạy:

```bat
build.bat
```

File tạo ra là `MarkdownFileSplitter.jar`.

## Lưu ý

- File Markdown được đọc và ghi bằng UTF-8, hỗ trợ tiếng Việt.
- Có thể dán trực tiếp đường dẫn Windows vào ô **File .md**.
- Nếu bỏ trống **Nơi lưu**, app dùng thư mục chứa file gốc.
- Mặc định app không sửa file gốc. Chỉ khi bật **Cập nhật file gốc thành mục lục Obsidian**, app mới backup rồi cập nhật file gốc.

## Cấu trúc mã nguồn

- `MarkdownSplitter.java`: giao diện và sự kiện nút bấm.
- `MarkdownSplitService.java`: toàn bộ logic tách từ hai script Python.
- `SplitOptions.java`: các lựa chọn lấy từ giao diện.
- `SplitResult.java`: kết quả trả về cho giao diện.
- `test/MarkdownSplitServiceTest.java`: kiểm thử frontmatter, Page và slug tiếng Việt.
