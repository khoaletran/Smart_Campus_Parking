# Nhật Ký Thay Đổi (Changelog)

Toàn bộ các mốc thay đổi và cập nhật quan trọng của dự án Smart Campus Parking được ghi nhận tại đây.

---

## [v1.0.0-design] - 2026-10-20

### Đã hoàn thành (Milestone: Specification & Architecture Design)
- **Đặc tả nghiệp vụ chuẩn Black-Box (Chương 2):**
  - Tinh giản 8 Use Case nghiệp vụ theo chuẩn hộp đen (loại bỏ chi tiết kỹ thuật nội bộ như SQL Transaction, RabbitMQ khỏi Use Case).
  - Điều chỉnh nghiệp vụ cốt lõi: Tận dụng trực tiếp **Thẻ Sinh Viên** tích hợp chip NFC/RFID làm phương tiện nhận diện khi gửi xe.
  - Xuất bản tài liệu: `Agent/Chuong2_PhanTich_SmartCampusParking.docx` và sơ đồ `Agent/SmartCampusParking_UseCase.drawio`.

- **Thiết kế kiến trúc & Mô hình lớp chuẩn OMG UML 2.5.1 (Chương 3):**
  - Hiện thực sơ đồ Class Diagram gồm 21 lớp thuộc 5 module Modular Monolith: `Identity`, `Card`, `Parking`, `Wallet`, `Notification`.
  - Áp dụng 4 hiệu chỉnh chuẩn quốc tế OMG:
    1. Chuẩn hóa quan hệ Shared Aggregation (`o--`) cho các thực thể kế toán & nhật ký bảo mật (`WalletTransaction`, `CardStatusHistory`, `CardPackage`).
    2. Phân tách rõ ràng giữa Domain Entity và Tầng Hạ tầng (`GateHardwareAdapter` điều khiển barrier, `OutboxRelayWorker` kết nối RabbitMQ).
    3. Chuẩn hóa cú pháp phương thức `visibility name(param: Type): ReturnType` và tường minh hóa các lớp DTO.
    4. Loại bỏ thuộc tính liên kết trùng lặp, chuẩn hóa nhãn vai trò và bội số trên Association.
  - Xuất bản sơ đồ Draw.io Native Component: `Agent/SmartCampusParking_ClassDiagram.drawio`.
  - Xuất bản tài liệu: `Agent/Chuong3_3.1_MoHinhLop_ClassDiagram.docx`.

- **Tinh gọn & Tái cấu trúc tài liệu dự án:**
  - Hợp nhất các tài liệu Markdown phân tán thành `README.md` (root) và `docs/ARCHITECTURE.md`.
  - Dọn dẹp toàn bộ dữ liệu văn bản bóc tách nháp và tài liệu tham khảo ngoại lai dung lượng lớn (>11 MB).
