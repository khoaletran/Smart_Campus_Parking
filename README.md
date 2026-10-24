# Smart Campus Parking 🚗🎓

> **Hệ thống Quản lý Gửi xe Thông minh Khuôn viên Trường học bằng Thẻ Sinh viên (NFC/RFID)**

---

## 1. Giới thiệu

**Smart Campus Parking** là giải pháp quản lý gửi xe thông minh dành cho trường đại học. Điểm nổi bật của hệ thống là **kết hợp xác thực 2 yếu tố: Thẻ Sinh viên (NFC/RFID) và Camera quét nhận diện Biển số xe (ANPR)** giúp tối ưu tốc độ lưu thông và bảo vệ phương tiện chống tráo/trộm xe.

### Lợi ích cốt lõi
- **Một chạm đa năng (All-in-One):** Sinh viên dùng duy nhất thẻ sinh viên cho cả điểm danh, thư viện và gửi xe; khuyến khích văn hóa luôn đeo thẻ sinh viên.
- **An ninh 2 lớp (Thẻ + Biển số xe):** Tự động nhận diện biển số xe và chụp ảnh lúc vào/ra, tự động đối soát biển số để chặn đứng nguy cơ tráo xe hoặc nhặt thẻ lấy cắp xe.
- **Thanh toán linh hoạt:** Tự động kiểm tra gói cước (tuần, tháng, số lần) hoặc trừ số dư ví điện tử mô phỏng theo 3 cách tính phí (theo lượt, block 4 giờ, theo giờ thực tế).
- **An toàn giao dịch:** Cơ chế kiểm soát đồng thời (SQL Lock), chống trừ tiền/lượt 2 lần (Idempotency-Key) và lưu vết kiểm toán giao dịch trực tiếp trong cơ sở dữ liệu.

---

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5.x, Spring MVC | Xử lý REST API và logic nghiệp vụ Modular Monolith |
| **Bảo mật** | Spring Security, JJWT | Đăng nhập Stateless JWT, phân quyền theo vai trò |
| **Cơ sở dữ liệu** | MariaDB / MySQL 8 (InnoDB), Spring Data JPA | Quản lý quan hệ dữ liệu, ACID transactions |
| **Migration** | Flyway | Quản lý phiên bản schema cơ sở dữ liệu tự động |
| **Message Queue**| RabbitMQ | *(Tạm hoãn / Deferred)* Chuẩn bị sẵn sàng khi mở rộng quy mô |
| **Web Frontend** | React, TypeScript, Vite, Tailwind CSS | Giao diện Quản trị Desktop và Ứng dụng Sinh viên Mobile-first |
| **NFC (Mở rộng)**| SwiftUI, Apple Core NFC | Đọc mã UID thẻ sinh viên qua iPhone |
| **Môi trường/CI** | Docker Compose, Testcontainers, GitHub Actions | Đóng gói môi trường local và CI/CD tự động |

---

## 3. Cấu trúc thư mục dự án

```text
parking_student/
├── Agent/                     # Báo cáo phân tích (Chương 2, 3), sơ đồ UML Draw.io
│   ├── scripts/               # Scripts tiện ích tự động hóa
│   ├── Chuong2_PhanTich_SmartCampusParking.docx
│   ├── Chuong3_3.1_MoHinhLop_ClassDiagram.docx
│   ├── SmartCampusParking_ClassDiagram.drawio
│   └── SmartCampusParking_UseCase.drawio
├── ChuongTrinh/               # Mã nguồn triển khai
│   ├── backend/               # Spring Boot Modular Monolith
│   ├── web/                   # Ứng dụng React + TypeScript
│   └── compose.yaml           # Docker Compose cấu hình MariaDB & RabbitMQ
├── docs/                      # Tài liệu kỹ thuật chi tiết
│   ├── ARCHITECTURE.md        # Kiến trúc 5 module, thiết kế DB và lộ trình
│   └── CHANGELOG.md           # Lịch sử thay đổi hệ thống
├── TaiLieu/                   # Tài liệu gốc của đề tài (Read-Only)
├── GEMINI.md                  # Quy tắc vận hành AI Agent
└── README.md                  # Hướng dẫn tổng quan và chạy nhanh
```

---

## 4. Hướng dẫn cài đặt & Khởi chạy nhanh (Quick Start)

### Yêu cầu môi trường
- **Java 21** (JDK 21+)
- **Node.js** (v18+) & **npm**
- **Docker** & **Docker Compose**

### Bước 1: Khởi động Hạ tầng (Database & RabbitMQ)
```bash
cd ChuongTrinh
docker compose up -d
```
> Khởi chạy MariaDB trên cổng `3306` và RabbitMQ trên cổng `5672` (Management UI: `http://localhost:15672`).

### Bước 2: Chạy Backend (Spring Boot)
```bash
cd ChuongTrinh/backend
./mvnw spring-boot:run
```
> Backend API khởi động tại: `http://localhost:8080` (Health Check: `http://localhost:8080/api/health`).

### Bước 3: Chạy Web Frontend (React)
```bash
cd ChuongTrinh/web
npm install
npm run dev
```
> Web app khởi động tại: `http://localhost:5173`.

---

## 5. Tài khoản dùng thử (Demo)

| Tài khoản (Username) | Mật khẩu | Vai trò (Role) | Chức năng chính |
| :--- | :---: | :---: | :--- |
| `admin` | `password` | `ADMIN` | Quản lý sinh viên, kích hoạt/khóa thẻ, tạo gói cước, cài đặt biểu phí, nhật ký kiểm toán |
| `student01` | `password` | `STUDENT` | Tra cứu hồ sơ, số dư ví, nạp ví nhanh, mua gói cước, lịch sử xe & tự xác thực 2FA khi lệch biển |

---

## 6. Danh mục API cốt lõi

| Phương thức | Endpoint | Phân hệ | Mục đích |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/auth/login` | Identity | Đăng nhập hệ thống, cấp token JWT |
| `GET` | `/api/students/me` | Identity | Lấy thông tin cá nhân sinh viên hiện tại |
| `GET` | `/api/cards/me` | Card | Xem thông tin thẻ sinh viên và gói cước đang áp dụng |
| `POST` | `/api/cards/link` | Card | Kích hoạt thẻ sinh viên mới vào hệ thống (Admin) |
| `POST` | `/api/parking/entries` | Parking | Cổng tự động ghi nhận xe vào (Xác thực thẻ, quét/lưu biển số & ảnh chụp, mở barrier) |
| `POST` | `/api/parking/exits` | Parking | Cổng tự động ghi nhận xe ra (Quét biển số ra, đối chiếu biển số, tự động trừ gói cước hoặc ví) |
| `POST` | `/api/parking/sessions/{id}/self-verify-exit` | Parking | Sinh viên tự xác thực 2FA trên điện thoại để mở barrier tự động khi cảnh báo lệch biển số |
| `GET` | `/api/wallet/balance` | Wallet | Tra cứu số dư ví điện tử |
| `POST` | `/api/wallet/top-ups` | Wallet | Nạp tiền ví mô phỏng (Idempotency) |

---

## 7. Tài liệu chi tiết
- **Kiến trúc hệ thống & Lộ trình thực hiện:** Xem [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
- **Lịch sử thay đổi:** Xem [`docs/CHANGELOG.md`](docs/CHANGELOG.md).
