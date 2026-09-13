# Hướng dẫn cập nhật project trong IDE

## 1. Thay file main cũ

Trong project đã giải nén, mở thư mục `src` và thay `MarkdownSplitter.java` cũ bằng file mới.

File này giờ chỉ phụ trách:

- Nhận đường dẫn file và thư mục output.
- Đọc các checkbox trên giao diện.
- Gọi `MarkdownSplitService.splitFile(...)`.
- Hiển thị kết quả hoặc lỗi.

Không đặt lại logic xử lý nội dung vào class giao diện.

## 2. Thêm ba file Java mới vào cùng thư mục `src`

```text
src/
├── MarkdownSplitter.java
├── MarkdownSplitService.java
├── SplitOptions.java
└── SplitResult.java
```

Các file hiện không dùng `package`, vì vậy phải nằm cùng source root. Nếu bạn tự thêm `package com.example...`, hãy thêm cùng một package vào cả bốn file.

## 3. Cấu hình IDE

Trong IntelliJ IDEA:

1. Vào **File → Project Structure → Project**.
2. Chọn **Project SDK: JDK 17** hoặc mới hơn.
3. Nhấp phải thư mục `src` → **Mark Directory as → Sources Root**.
4. Mở `MarkdownSplitter.java`.
5. Bấm nút Run tại hàm `main`.

Không cần cài thư viện ngoài; toàn bộ ứng dụng dùng Java chuẩn.

## 4. Logic Python đã được đặt ở đâu?

| Python | Java |
| --- | --- |
| `extract_frontmatter` | `MarkdownSplitService.extractFrontmatter` |
| `split_by_separator` của file thứ nhất | `splitBody` với `splitOnDelimiter=true` |
| `split_by_separator` của file thứ hai | `splitBody` với `splitOnPageHeading=true` |
| `detect_title` | `detectTitle` |
| `remove_vietnamese_accents` | `removeVietnameseAccents` |
| `slugify_filename` | `slugifyFilename` |
| `make_obsidian_link` | `makeObsidianLink` |
| `split_markdown_file` | `splitFile` |

Hai kiểu tách có thể bật cùng lúc. Khi đó, gặp `---` hoặc tiêu đề `Page...` đều tạo phần mới. YAML frontmatter được lấy ra trước nên dấu `---` bao quanh YAML không làm phát sinh file rỗng.

## 5. Cập nhật file gốc

Checkbox cập nhật file gốc mặc định tắt. Khi bật, thứ tự xử lý là:

```text
Đọc file gốc
→ tạo các file con
→ tạo file .backup-<timestamp>.md
→ thay file gốc bằng mục lục Obsidian
```

Hai script Python cũ đang đặt `origin_dir = input_file.parent`, nên lệnh `rename` thực tế vẫn thao tác trong cùng thư mục. Bản Java không dùng đoạn đó; nó giữ file backup cạnh file gốc để có thể phục hồi.

## 6. Build trên Windows

`build.bat` đã được sửa để compile tất cả class:

```bat
javac -encoding UTF-8 -d out src\*.java
```

Sau khi sửa code, chạy `build.bat`. Để mở ứng dụng, chạy `run.bat`.
