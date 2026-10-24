# Nhật Ký Thay Đổi (Changelog)

Toàn bộ các mốc thay đổi và cập nhật quan trọng của dự án Smart Campus Parking được ghi nhận tại đây.

---

## [v1.4.0-dev] - 2026-10-25

### Hoàn thành Luồng 1 & Luồng 2 Module Thẻ (Card) & Chuẩn hóa Tri thức Nghiệp vụ
- **Module 1 (Identity) — Đồng nhất Tên đăng nhập & MSSV:**
  - Đồng nhất `username = studentCode` (MSSV) ở tầng logic đăng ký, sinh viên dùng trực tiếp MSSV để đăng nhập.
  - Bổ sung hàm công khai `getStudentByCode(studentCode)` trên `IdentityModuleApi` và hiện thực trong `AuthService` phục vụ giao tiếp liên module.
- **Module 2 (Card) — Hoàn thành Luồng 1 (Kích hoạt & Gán thẻ NFC/RFID):**
  - Entity `Card`: Khớp Bảng 3 tài liệu (`cardUid`, `studentId`, `status`, `issueDate`), các phương thức nghiệp vụ nội tại (`linkStudent`, `lockCard`, `unlockCard`, `isActive`).
  - Repository `CardRepository`: Tìm kiếm theo `cardUid`, kiểm tra tồn tại và tìm danh sách thẻ của sinh viên.
  - DTO: `LinkCardRequest`, `CardDto` (khai báo trong Sơ đồ lớp).
  - Service `CardService`: Đảm bảo ranh giới Modular Monolith (Strict Bounded Contexts) — chỉ inject `IdentityModuleApi`, tuyệt đối không chọc vào `StudentRepository`.
  - Controller `CardController`: Endpoint `POST /api/cards/link`.
- **Module 2 (Card) — Hoàn thành Luồng 2 (Khóa/Mở thẻ & Tự động lưu vết kiểm toán):**
  - Entity `CardStatusHistory`: Khớp Bảng 4 tài liệu (`cardId`, `previousStatus`, `newStatus`, `reason`, `changedBy`, `changedAt`). Bản ghi bất biến (Immutable Audit Trail).
  - Repository `CardStatusHistoryRepository`: Truy vấn lịch sử thẻ theo thời gian giảm dần.
  - DTO: `ChangeCardStatusRequest`, `CardStatusHistoryDto`.
  - Service: `lockCard()`, `unlockCard()` tự động ghi vết trong cùng `@Transactional`; `getCardHistory()`.
  - Controller: `PATCH /api/cards/{id}/lock`, `PATCH /api/cards/{id}/unlock`, `GET /api/cards/{id}/history`.
  - Nâng cấp chính sách: Cho phép 1 sinh viên sở hữu nhiều thẻ Active (khớp Sơ đồ EER `(1, n)`), thêm API tra cứu danh sách thẻ `GET /api/cards/student/{studentId}`.
- **Quản lý Tri thức Nghiệp vụ (`docs/VanDe_GiaiPhap.md`):**
  - Khởi tạo tài liệu lưu trữ chuyên sâu 7 vấn đề nghiệp vụ và quyết định kiến trúc cốt lõi.
  - Thống nhất quyết định kiến trúc: **Chính sách 1 Sinh Viên — 1 Suất Để Xe (Single-Vehicle Policy with Backup Cards)** — chặn gửi xe thứ hai tại cổng vào, hỗ trợ quẹt bất kỳ thẻ phụ nào để lấy xe ra khi quên thẻ trong cốp.
  - Cập nhật quy tắc bắt buộc vào `GEMINI.md` và `.agents/rules/file_policy.md`.
  - Thiết lập cơ chế tự động kết thúc phiên (End-of-Session Trigger `done`): Tự động kiểm thử, cập nhật tài liệu phiên, kiểm tra an toàn git và commit/push lên repository.

---

## [v1.3.0-dev] - 2026-10-24

### Hoàn thành Khởi tạo Hạ tầng, Nền tảng Common & Module 1: Định danh (Identity)
- **Hạ tầng Docker MariaDB:**
  - Cấu hình và khởi chạy MariaDB 11.4 trên cổng `3307` (tránh xung đột với dịch vụ MariaDB/MySQL chạy sẵn trên Windows).
  - Chuẩn hóa file `compose.yaml` và kết nối JDBC từ Spring Boot `application.yml`.
- **Cơ sở dữ liệu & Flyway Migration (`V1__init_schema.sql`):**
  - Khởi tạo đúng **10 bảng chuẩn** khớp 100% với Sơ đồ EER và Sơ đồ lớp OMG UML.
  - Loại bỏ các trường không có trong tài liệu chính thức: `pin_code_hash`, `default_license_plate` và bảng thừa `audit_logs`.
  - Kiểm soát toàn vẹn dữ liệu: Tiền tệ `DECIMAL(12, 2)`, chỉ mục `UNIQUE` cho `idempotency_key`, trạng thái tài khoản `active BOOLEAN`.
- **Gói Dùng chung (`common`):**
  - `ApiResponse<T>`: Chuẩn hóa định dạng JSON phản hồi cho toàn hệ thống.
  - `GlobalExceptionHandler`: Xử lý ngoại lệ tập trung (`@RestControllerAdvice`), bắt lỗi `@Valid` và ném thông báo nghiệp vụ an toàn.
  - `HealthController`: Endpoint `GET /api/health` kiểm tra trạng thái hoạt động của hệ thống.
  - `WebSecurityConfig`: Cấu hình Spring Security Stateless, kích hoạt mã hóa BCrypt, cấu hình CORS cho phép Frontend gọi API, tích hợp bộ lọc JWT.
- **Module 1: Định danh (`identity`):**
  - Entity: `User` (Bảng 1 trong tài liệu, các phương thức `verifyPassword`, `changePassword`) và `Student` (Bảng 2 trong tài liệu, phương thức `updateProfile`).
  - Enum: `Role` (`ADMIN`, `STUDENT`).
  - Repository: `UserRepository`, `StudentRepository`.
  - DTO: `StudentDto` (khai báo trong Sơ đồ lớp), `LoginRequest`, `RegisterStudentRequest`, `AuthResponse`.
  - Interface liên module: `IdentityModuleApi` (`authenticate`, `verifyToken`, `getStudentById`, `getStudentByUserId`).
  - Service & Security: `AuthService` quản lý `@Transactional` đăng ký/đăng nhập; `JwtService` sinh và kiểm tra HMAC-SHA256 Token; `JwtAuthenticationFilter` bảo vệ API.
  - Controller: `AuthController` (`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/profile`).

---

## [v1.2.0-design] - 2026-10-23

### Tối ưu Cổng Headless Tự Động 100% (Zero-Human) & Tạm hoãn RabbitMQ
- **Cổng Kiểm Soát Không Màn Hình & Tự Động Hoàn Toàn (Headless Gate):**
  - Cột quẹt thẻ vật lý loại bỏ màn hình và người canh gác; giao tiếp bằng còi Buzzer (1 bíp mở, 3 bíp lỗi) và đèn LED Xanh/Đỏ.
  - Toàn bộ cơ chế phản hồi lỗi chuyển lên **Ứng dụng di động của Sinh viên (Mobile Web/App)**.
  - Xử lý thiếu số dư ví: Sinh viên tự nạp nhanh bằng VietQR/MoMo trên điện thoại $\rightarrow$ Quẹt lại thẻ tại cột để mở barrier.
  - Cơ chế tự xác thực 2FA khi lệch biển số (`plateMismatch = true`): Sinh viên nhận thông báo, tự đối chiếu ảnh vào/ra và nhập **Mã PIN / OTP Sinh viên** kèm cam kết trách nhiệm để mở barrier tự động (`POST /api/parking/sessions/{id}/self-verify-exit`, `verifiedBy = STUDENT_SELF_2FA`).
- **Tạm hoãn sử dụng RabbitMQ (RabbitMQ Deferred):**
  - Tạm hoãn cấu hình RabbitMQ broker và worker nền `OutboxRelayWorker` để tập trung hoàn thiện khối Modular Monolith cốt lõi (Spring Boot + MariaDB).
  - Toàn bộ sự kiện phiên gửi xe và biến động ví được ghi nhận trực tiếp vào bảng kiểm toán trong MariaDB trong cùng Transaction ACID.
- **Sơ đồ Luồng Màn hình Chuẩn:**
  - Thiết kế và xuất bản sơ đồ màn hình 1:1 theo mẫu chuẩn tại [`SmartCampusParking_SoDoLuongManHinh.drawio`](file:///d:/project/parking_student/Agent/SmartCampusParking_SoDoLuongManHinh.drawio).

---

## [v1.1.0-design] - 2026-10-22

### Bổ sung Quét & Lưu Biển số xe và Đối soát Chống trộm (License Plate Verification)
- **Nâng cấp an ninh 2 yếu tố (2FA Gate Control):**
  - Kết hợp thẻ sinh viên (NFC/RFID) với camera nhận diện biển số xe (ANPR/OCR).
  - Bổ sung quy trình lưu `entryPlateNumber` và `entryImageUrl` khi xe vào (`POST /api/parking/entries`).
  - Bổ sung quy trình chụp/quét `exitPlateNumber`, `exitImageUrl` và đối chiếu tự động với biển số vào khi xe ra (`POST /api/parking/exits`).
  - Hiện thực cơ chế cảnh báo lệch biển số (`plateMismatch = true`): Khóa barrier tự động, hiển thị cảnh báo đỏ trên màn hình trực cổng, yêu cầu nhân viên trực cổng đối chiếu và xác nhận thủ công (`POST /api/parking/sessions/{id}/confirm-exit`) có lưu nhật ký kiểm toán.
- **Cập nhật Quyết định Kiến trúc & Mô hình Dữ liệu:**
  - Bổ sung ADR-05: *Xác thực 2 yếu tố Cổng gửi xe & Đối soát Biển số xe chống trộm*.
  - Mở rộng bảng `parking_sessions` với các trường: `entry_plate_number`, `entry_image_url`, `exit_plate_number`, `exit_image_url`, `plate_mismatch`, `manual_confirmed_by`, `operator_note`.
  - Bổ sung thuộc tính `license_plate` trong bảng `students` để lưu biển số xe sinh viên đăng ký mặc định.
  - Cập nhật mô hình lớp UML (Chương 3): Thêm interface `PlateRecognitionAdapter`, DTO `PlateRecognitionResult`, mở rộng phương thức trong `ParkingSession` và `ParkingApplicationService`.
  - Cập nhật tài liệu phân tích Use Case (Chương 2): Điều chỉnh UC002 (Gửi xe vào) và UC003 (Lấy xe ra).

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
