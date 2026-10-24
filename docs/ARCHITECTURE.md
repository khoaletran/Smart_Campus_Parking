# Kiến Trúc Hệ Thống Smart Campus Parking

> **Kiến trúc:** Modular Monolith (Spring Boot 3.5.x, MariaDB, React; RabbitMQ tạm hoãn)  
> **Tiêu chuẩn thiết kế:** OMG UML 2.5.1, Domain-Driven Design (DDD) Bounded Contexts

---

## 1. Tổng quan Kiến trúc Modular Monolith

Hệ thống được thiết kế theo mô hình **Modular Monolith** nhằm tối ưu hóa chi phí vận hành và đơn giản hóa việc triển khai (single deployment unit), đồng thời phân chia ranh giới nghiệp vụ (bounded contexts) độc lập thông qua các API nội bộ và DTO:

```mermaid
graph TD
    Client["Client (Mobile Web Sinh viên / Admin Web Desktop)"] -->|REST API + JWT| Gateway["Spring MVC Security Gateway"]
    
    subgraph Modular Monolith [Backend Single Process]
        Gateway --> M1["Identity Module<br/>(Users, Students)"]
        Gateway --> M2["Card Module<br/>(Cards, Packages)"]
        Gateway --> M3["Parking Module<br/>(Headless Gates, Sessions, Rates)"]
        Gateway --> M4["Wallet Module<br/>(Wallets, Transactions)"]
        Gateway --> M5["Audit & Notification Module<br/>(Internal Event Logs)"]
        
        M3 -->|Validate Card & Deduct Trip| M2
        M3 -->|Deduct Parking Fee| M4
        M3 -->|Audit Session Event| M5
        M4 -->|Audit Wallet Event| M5
        M4 -->|Activate Package| M2
        M2 -->|Verify Student Profile| M1
    end
    
    subgraph Infrastructure
        Modular Monolith -->|ACID Transactions| DB[("MariaDB / MySQL (InnoDB)")]
        MQ["RabbitMQ Broker (TẠM HOÃN)"] -.->|Deferred| EmailService["SMTP / Push Worker"]
    end
```

---

## 2. Phân chia 5 Module nghiệp vụ (Bounded Contexts)

### 2.1 Module Định danh (`identity`)
- **Trách nhiệm:** Xác thực người dùng, cấp phát token JWT, phân quyền (`ADMIN`, `STUDENT`), quản lý hồ sơ sinh viên và biển số xe đăng ký mặc định (`licensePlate`).
- **Quy tắc bảo mật:** Không nhận `studentId` tùy ý từ client; danh tính sinh viên được trích xuất trực tiếp từ JWT.

### 2.2 Module Thẻ & Gói cước (`card`)
- **Trách nhiệm:** Quản lý vòng đời thẻ sinh viên NFC/RFID (kích hoạt, khóa khi mất, mở khóa), nhật ký bảo mật trạng thái thẻ, đăng ký và quản lý các gói cước gửi xe.
- **Gói dịch vụ hỗ trợ:**
  - *Gói Tuần / Tháng:* Hiệu lực theo thời gian (`startDate` $\rightarrow$ `endDate`).
  - *Gói định mức số lượt:* Hiệu lực theo số lần quẹt còn lại (`remainingTrips`, ví dụ: 30 lượt).

### 2.3 Module Bãi đỗ xe (`parking`)
- **Trách nhiệm:** Quản lý cổng kiểm soát vào/ra, kết nối phần cứng camera quét nhận diện biển số xe (`PlateRecognitionAdapter`), điều khiển barrier (`GateHardwareAdapter`), cấu hình bảng giá và quản lý phiên gửi xe (`ParkingSession`).
- **Mô hình Cổng Headless Tự Động 100% (Zero-Human Gate):**
  - Cột quẹt thẻ ngoài trời không có màn hình, không có nhân viên trực, không có luồng bảo vệ trung tâm. Giao tiếp tại cổng hoàn toàn bằng âm thanh còi Buzzer (1 bíp = mở; 3 bíp = lỗi) và đèn LED Xanh/Đỏ.
  - *Xe vào (Check-in):* Quẹt thẻ + Camera ANPR chụp và nhận diện biển số; kiểm tra trạng thái thẻ `ACTIVE` và quy tắc Anti-passback $\rightarrow$ Lưu `entryPlateNumber`, `entryImageUrl` vào `ParkingSession` $\rightarrow$ Mở barrier tự động.
  - *Xe ra (Check-out):* Quẹt thẻ + Camera ANPR chụp và nhận diện biển số ra (`exitPlateNumber`, `exitImageUrl`).
  - *Cơ chế đối soát chống trộm & Tự xác thực 2FA (Zero-Human Mismatch Handling):* Tự động so sánh biển số vào và ra. Nếu không khớp (`plateMismatch = true`), hệ thống khóa barrier tự động, còi báo động kêu. Sinh viên nhận thông báo trên điện thoại di động (Mobile Web/App), đối soát ảnh vào/ra và nhập **Mã PIN / OTP cá nhân** kèm cam kết trách nhiệm để tự động mở barrier. Hệ thống ghi nhận kiểm toán `verifiedBy = STUDENT_SELF_2FA`.
  - *Xử lý thiếu số dư ví:* Khi ví không đủ tiền trừ lượt, cột báo đèn đỏ. Điện thoại sinh viên bật ngay giao diện nạp ví nhanh bằng VietQR/MoMo mô phỏng $\rightarrow$ Nạp xong quẹt lại thẻ để mở barrier.
- **3 phương thức tính phí linh hoạt (do Admin cấu hình):**
  1. *Thu phí theo lượt (`PER_ENTRY`):* Giá cố định mỗi lần vào/ra.
  2. *Thu phí theo block giờ kết hợp (`BLOCK_4H`):* Giá cố định cho 4 giờ đầu + phụ trội mỗi giờ phát sinh.
  3. *Thu phí theo giờ thực tế (`PER_HOUR`):* Làm tròn lên theo giờ, tối thiểu 1 giờ.
- **Quy tắc ưu tiên trừ phí khi xe ra:**
  1. Ưu tiên 1: Gói thời gian (tuần/tháng) còn hạn $\rightarrow$ Miễn phí.
  2. Ưu tiên 2: Gói định mức lượt còn số lần $\rightarrow$ Trừ 1 lượt.
  3. Ưu tiên 3: Không có gói $\rightarrow$ Tính phí theo bảng giá và trừ trực tiếp vào số dư ví.

### 2.4 Module Ví điện tử (`wallet`)
- **Trách nhiệm:** Quản lý tài khoản ví mô phỏng của sinh viên, nạp tiền mô phỏng, thanh toán mua gói cước, trừ tiền gửi xe theo lượt.
- **Nguyên tắc kế toán:** Toàn bộ biến động số dư phải sinh bản ghi giao dịch (`WalletTransaction`), tính chất bất biến (Immutable Audit Log).

### 2.5 Module Kiểm toán & Thông báo (`notification`)
- **Trách nhiệm:** Ghi nhận sự kiện nghiệp vụ và nhật ký kiểm toán trực tiếp vào cơ sở dữ liệu MariaDB trong cùng Transaction SQL.
- **Trạng thái:** Tạm hoãn tích hợp RabbitMQ Broker và tiến trình nền `OutboxRelayWorker` để tinh giản hạ tầng.

---

## 3. Các quyết định kỹ thuật cốt lõi (Architecture Decision Records)

### ADR-01: Kiểm soát Concurrency & Toàn vẹn Transaction SQL
- **Vấn đề:** Tránh tình trạng quẹt thẻ vào 2 cổng cùng lúc hoặc quẹt ra 2 lần gây đóng trùng lượt / trừ tiền 2 lần.
- **Giải pháp:** Sử dụng cơ chế khóa bi quan (Pessimistic Locking / `SELECT ... FOR UPDATE`) trên bản ghi thẻ và phiên gửi xe trong cùng 1 Transaction SQL. Mọi thao tác kiểm tra gói, trừ lượt, trừ ví, đóng phiên được commit nguyên tử (ACID). Nếu số dư ví không đủ, transaction rollback và barrier không mở.

### ADR-02: Chống trùng lặp với Idempotency-Key
- **Vấn đề:** Sự cố mạng khiến client gửi lại yêu cầu quẹt thẻ hoặc nạp tiền nhiều lần.
- **Giải pháp:** Client gửi kèm `Idempotency-Key` (UUIDv4) qua Header. Server lưu key với chỉ mục UNIQUE. Khi gặp lại key đã xử lý thành công, server trả ngay kết quả cũ mà không trừ tiền hay sinh phiên gửi xe mới.

### ADR-03: Xử lý tiền tệ chuẩn xác (No Floating Point)
- **Vấn đề:** Sai số tính toán khi dùng kiểu số thực (`float`, `double`).
- **Giải pháp:** Bắt buộc sử dụng kiểu `DECIMAL(12, 2)` trong cơ sở dữ liệu và `BigDecimal` trong mã nguồn Java / Spring Boot.

### ADR-04: Tạm hoãn RabbitMQ (Deferred) & Ghi nhận Kiểm toán Trực tiếp trong MariaDB
- **Vấn đề:** Việc triển khai và duy trì cụm RabbitMQ broker tăng độ phức tạp vận hành trong giai đoạn phát triển cốt lõi.
- **Giải pháp:** Tạm hoãn tích hợp RabbitMQ. Các sự kiện phiên gửi xe, biến động ví và cảnh báo lệch biển số được ghi nhận trực tiếp vào các bảng kiểm toán (`audit_logs`, `parking_sessions`, `wallet_transactions`) trong cùng Transaction ACID của MariaDB. Kiến trúc vẫn giữ cấu trúc module phân tách để sẵn sàng cắm lại RabbitMQ khi mở rộng quy mô.

### ADR-05: Cổng Headless Tự Động 100% & Tự Xác Thực 2FA Trên Thiết Bị Di Động
- **Vấn đề:** Vận hành bãi xe không dùng nhân viên gác cổng, đồng thời tránh chi phí mua màn hình cảm ứng ngoài trời dễ hỏng hóc do thời tiết. Cần cơ chế ngăn chặn tráo xe / trộm xe khi sinh viên làm rơi thẻ.
- **Giải pháp:** Cột quẹt thẻ vật lý thiết kế dạng Headless (không màn hình, chỉ có đầu đọc thẻ, camera ANPR, còi buzzer và đèn LED chỉ thị). Khi camera phát hiện lệch biển số (`plateMismatch = true`), barrier tự động khóa và còi hú cảnh báo. Sinh viên tự mở ứng dụng trên điện thoại (Mobile Web), đối chiếu ảnh vào/ra và thực hiện xác thực 2FA (nhập Mã PIN hoặc OTP Email sinh viên) để hệ thống tự động mở barrier và lưu vết pháp lý (`verifiedBy = STUDENT_SELF_2FA`).

---

## 4. Lộ trình triển khai kỹ thuật (Milestones 0 – 7)

- [x] **Mốc 0 — Khởi tạo dự án:** Cài đặt Java 21, Maven, Node.js, Docker; cấu hình `compose.yaml` (MariaDB 11.4 trên cổng 3307).
- [x] **Mốc 1 — Backend & Migration:** Khởi tạo Spring Boot 3.4.x/4.1.x, kết nối MariaDB, thiết lập Flyway migration `V1__init_schema.sql` (10 bảng chuẩn theo EER, đã loại bỏ pin/license_plate/audit_logs), hoàn thiện package `common` và kiểm tra `GET /api/health`.
- [x] **Mốc 2 — Identity Module:** Tạo thực thể `User`, `Student` (khớp 100% Bảng 1 & 2 tài liệu Chương 3); cấu hình Spring Security Stateless, JWT Authentication; interface liên module `IdentityModuleApi`; hoàn thiện API `/api/auth/register`, `/api/auth/login`, `/api/auth/profile`.
- [ ] **Mốc 3 — Card Module (Thẻ sinh viên & Gói cước):** Đã hoàn thành Luồng 1 (Kích hoạt/liên kết thẻ) và Luồng 2 (Khóa/Mở thẻ & lưu vết `CardStatusHistory`); chuẩn bị triển khai Luồng 3 (Gói cước `CardPackage`) và Luồng 4 (`CardModuleApi` cho bãi xe).
- [ ] **Mốc 4 — Wallet & Bãi xe (Parking & Zero-Human Gate):** Hoàn thiện ví điện tử, Idempotency-Key chống trùng lặp; luồng xe vào (`/check-in`) lưu biển số & ảnh ANPR, xe ra (`/check-out`) đối soát biển số và trừ gói/ví trong cùng Transaction SQL; API tự xác thực 2FA khi lệch biển số.
- [ ] **Mốc 5 — Web Frontend (React + TS):** Xây dựng giao diện Vite + React gồm 2 phân hệ: Dashboard sinh viên Mobile-first và Trang Admin Desktop.
- [ ] **Mốc 6 — Tích hợp NFC (Mở rộng):** Phát triển ứng dụng iOS SwiftUI đọc mã UID thẻ sinh viên qua Core NFC gửi về REST API backend.
- [ ] **Mốc 7 — [TẠM HOÃN] RabbitMQ & Outbox Relay:** Tạm hoãn. Khi kích hoạt lại sẽ triển khai `OutboxRelayWorker` quét bảng sự kiện để chuyển tiếp qua RabbitMQ.
- [ ] **Mốc 8 — Kiểm thử & CI/CD:** Viết Unit Test và Integration Test với Testcontainers (MariaDB); cấu hình GitHub Actions tự động build & test.
