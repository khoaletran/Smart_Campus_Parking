# Kiến Trúc Hệ Thống Smart Campus Parking

> **Kiến trúc:** Modular Monolith (Spring Boot 3.5.x, MariaDB, React, RabbitMQ)  
> **Tiêu chuẩn thiết kế:** OMG UML 2.5.1, Domain-Driven Design (DDD) Bounded Contexts

---

## 1. Tổng quan Kiến trúc Modular Monolith

Hệ thống được thiết kế theo mô hình **Modular Monolith** nhằm tối ưu hóa chi phí vận hành và đơn giản hóa việc triển khai (single deployment unit), đồng thời phân chia ranh giới nghiệp vụ (bounded contexts) độc lập thông qua các API nội bộ và DTO:

```mermaid
graph TD
    Client["Client (Web React / iPhone NFC)"] -->|REST API + JWT| Gateway["Spring MVC Security Gateway"]
    
    subgraph Modular Monolith [Backend Single Process]
        Gateway --> M1["Identity Module<br/>(Users, Students)"]
        Gateway --> M2["Card Module<br/>(Cards, Packages)"]
        Gateway --> M3["Parking Module<br/>(Gates, Sessions, Rates)"]
        Gateway --> M4["Wallet Module<br/>(Wallets, Transactions)"]
        Gateway --> M5["Notification Module<br/>(Outbox Events)"]
        
        M3 -->|Validate Card & Deduct Trip| M2
        M3 -->|Deduct Parking Fee| M4
        M3 -->|Publish Session Event| M5
        M4 -->|Publish Wallet Event| M5
        M4 -->|Activate Package| M2
        M2 -->|Verify Student Profile| M1
    end
    
    subgraph Infrastructure
        Modular Monolith -->|ACID Transactions| DB[("MariaDB / MySQL (InnoDB)")]
        M5 -->|Outbox Relay Worker| MQ["RabbitMQ Broker"]
        MQ -->|Consumer| EmailService["Notification Logs / SMTP"]
    end
```

---

## 2. Phân chia 5 Module nghiệp vụ (Bounded Contexts)

### 2.1 Module Định danh (`identity`)
- **Trách nhiệm:** Xác thực người dùng, cấp phát token JWT, phân quyền (`ADMIN`, `STUDENT`, `GATE_OPERATOR`), quản lý hồ sơ sinh viên.
- **Quy tắc bảo mật:** Không nhận `studentId` tùy ý từ client; danh tính sinh viên được trích xuất trực tiếp từ JWT.

### 2.2 Module Thẻ & Gói cước (`card`)
- **Trách nhiệm:** Quản lý vòng đời thẻ sinh viên NFC/RFID (kích hoạt, khóa khi mất, mở khóa), nhật ký bảo mật trạng thái thẻ, đăng ký và quản lý các gói cước gửi xe.
- **Gói dịch vụ hỗ trợ:**
  - *Gói Tuần / Tháng:* Hiệu lực theo thời gian (`startDate` $\rightarrow$ `endDate`).
  - *Gói định mức số lượt:* Hiệu lực theo số lần quẹt còn lại (`remainingTrips`, ví dụ: 30 lượt).

### 2.3 Module Bãi đỗ xe (`parking`)
- **Trách nhiệm:** Quản lý cổng kiểm soát vào/ra, cấu hình bảng giá, xử lý phiên gửi xe (`ParkingSession`).
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

### 2.5 Module Thông báo (`notification`)
- **Trách nhiệm:** Ghi nhận sự kiện nghiệp vụ vào bảng `outbox_events`, chuyển tiếp sự kiện qua RabbitMQ bằng tiến trình định kỳ (`OutboxRelayWorker`) để gửi email SMTP hoặc lưu vết.

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

### ADR-04: Outbox Pattern kết hợp RabbitMQ
- **Vấn đề:** Nếu gửi tin nhắn RabbitMQ trực tiếp trong transaction SQL, khi RabbitMQ gặp sự cố sẽ làm rollback cả lượt gửi xe; hoặc khi commit DB thất bại thì tin nhắn đã gửi đi không thể thu hồi.
- **Giải pháp:** Ghi sự kiện vào bảng `outbox_events` cùng transaction với nghiệp vụ gửi xe. Một worker nền (`OutboxRelayWorker`) quét các bản ghi `PENDING`, đẩy vào RabbitMQ và cập nhật `PROCESSED`. Nếu broker lỗi, giao dịch gửi xe vẫn thành công và sự kiện sẽ được retry tự động.

---

## 4. Lộ trình triển khai kỹ thuật (Milestones 0 – 7)

- [ ] **Mốc 0 — Khởi tạo dự án:** Cài đặt Java 21, Maven, Node.js, Docker; cấu hình `compose.yaml` (MariaDB & RabbitMQ).
- [ ] **Mốc 1 — Backend & Migration:** Khởi tạo Spring Boot 3.5.x, kết nối MariaDB, thiết lập Flyway migration đầu tiên, kiểm tra `GET /api/health`.
- [ ] **Mốc 2 — Identity & Thẻ sinh viên:** Tạo bảng users, students, cards; cài đặt Spring Security, JWT; phân quyền Admin/Student; API kích hoạt/khóa thẻ.
- [ ] **Mốc 3 — Gửi xe & Ví mô phỏng:** Hoàn thiện luồng xe vào (`/entries`), xe ra (`/exits`), trừ gói/ví trong cùng Transaction SQL; tích hợp Idempotency-Key và khóa bản ghi.
- [ ] **Mốc 4 — Web Frontend (React + TS):** Xây dựng giao diện Vite + React gồm 3 phân hệ: Màn hình cổng (quẹt thẻ), Dashboard sinh viên (ví & gói cước), Trang Admin (quản lý thẻ & bảng giá).
- [ ] **Mốc 5 — Tích hợp NFC (Mở rộng):** Phát triển ứng dụng iOS SwiftUI đọc mã UID thẻ sinh viên qua Core NFC gửi về REST API backend.
- [ ] **Mốc 6 — RabbitMQ & Outbox Relay:** Hiện thực `OutboxRelayWorker` quét bảng `outbox_events`, đẩy sang RabbitMQ consumer để ghi nhận `notification_logs`.
- [ ] **Mốc 7 — Kiểm thử & CI/CD:** Viết Unit Test và Integration Test với Testcontainers (MariaDB); cấu hình GitHub Actions tự động build & test.
