# Smart Campus Parking 🚗🎓

> **Hệ thống Quản lý Gửi xe Thông minh Khuôn viên Trường học bằng Thẻ Sinh viên (NFC/RFID)**  
> **Sinh viên thực hiện:** Trần Lê Khoa  
> **Kiến trúc:** Modular Monolith (Spring Boot 3.5.x, MariaDB, React TypeScript)

---

## 1. Giới thiệu

**Smart Campus Parking** là giải pháp quản lý gửi xe thông minh dành riêng cho khuôn viên trường đại học. Hệ thống hoạt động theo mô hình **Cổng Headless Tự Động 100% (Zero-Human Gate)**, kết hợp xác thực 2 yếu tố: **Thẻ Sinh viên (NFC/RFID)** và **Camera AI nhận diện Biển số xe (ANPR)** giúp tối ưu lưu thông giờ cao điểm và bảo vệ phương tiện chống tráo xe, mất trộm.

### Lợi ích cốt lõi
- **Một chạm đa năng (All-in-One):** Sinh viên sử dụng thẻ sinh viên tích hợp gửi xe, điểm danh và thư viện; hỗ trợ 1 sinh viên sở hữu nhiều thẻ active (thẻ cứng chính, thẻ phụ dự phòng, NFC di động).
- **Chính sách 1 Sinh viên — 1 Suất để xe:** Ngăn chặn gian lận cho người ngoài mượn thẻ phụ gửi xe ké giá rẻ; đồng thời cứu trợ sinh viên khi lỡ để quên thẻ chính trong cốp xe (cho phép quẹt thẻ phụ để lấy xe ra).
- **An ninh 2 lớp (Thẻ + Biển số xe):** Camera ANPR chụp và lưu ảnh lúc vào/ra, tự động đối soát biển số xe.
- **Tự xử lý sự cố qua Điện thoại (Mobile-First):** Cổng vật lý ngoài trời không cần màn hình và nhân viên trực chốt. Mọi sự cố (lệch biển số, nạp ví nhanh) được sinh viên chủ động xử lý trực tiếp trên ứng dụng di động qua cơ chế xác thực trách nhiệm 2FA.
- **Thanh toán linh hoạt:** Tự động kiểm tra gói cước (tuần, tháng, số lần) hoặc trừ số dư ví điện tử theo bảng phí (theo lượt, block 4 giờ, theo giờ thực tế).

---

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Backend** | Java 21/23, Spring Boot 3.5.x, Spring MVC | Xử lý REST API và logic nghiệp vụ Modular Monolith |
| **Bảo mật** | Spring Security 6.x, JJWT (0.12.x), BCrypt | Xác thực Stateless JWT, phân quyền `ADMIN` / `STUDENT` |
| **Cơ sở dữ liệu** | MariaDB 11.4 (Docker), Spring Data JPA | Quản lý quan hệ dữ liệu 10 bảng chuẩn EER, ACID Transactions |
| **Migration** | Flyway | Quản lý phiên bản schema CSDL tự động |
| **Message Queue**| RabbitMQ | *(Tạm hoãn / Deferred)* Sẵn sàng kích hoạt khi mở rộng quy mô |
| **Web Frontend** | React 18/19, TypeScript, Vite, Tailwind CSS | Giao diện Quản trị Desktop và Ứng dụng Sinh viên Mobile-first |
| **NFC (Mở rộng)**| SwiftUI, Apple Core NFC | Đọc mã UID thẻ sinh viên qua iPhone |
| **Môi trường/CI** | Docker Compose, Testcontainers, GitHub Actions | Đóng gói môi trường local và CI/CD tự động |

---

## 3. Cấu trúc thư mục dự án

```text
parking_student/
├── ChuongTrinh/               # Mã nguồn triển khai
│   ├── backend/               # Spring Boot Modular Monolith (Spring Boot 3.5.x)
│   ├── web/                   # Ứng dụng React + TypeScript (Vite)
│   └── compose.yaml           # Docker Compose cấu hình MariaDB 11.4 (Port 3307)
├── docs/                      # Tài liệu kỹ thuật dự án
│   ├── ARCHITECTURE.md        # Kiến trúc 5 module, thiết kế CSDL và lộ trình
│   ├── CHANGELOG.md           # Nhật ký thay đổi và phiên bản hệ thống
│   └── VanDe_GiaiPhap.md      # Nhật ký vấn đề nghiệp vụ & giải pháp kiến trúc cốt lõi
├── TaiLieu/                   # Tài liệu gốc của đề tài
└── README.md                  # Hướng dẫn tổng quan và khởi chạy
```

---

## 4. Hướng dẫn cài đặt & Khởi chạy nhanh (Quick Start)

### Yêu cầu môi trường
- **Java 21** hoặc **Java 23** (JDK 21+)
- **Node.js** (v18+) & **npm**
- **Docker** & **Docker Compose**

### Bước 1: Khởi động Cơ sở dữ liệu (MariaDB Docker)
```bash
cd ChuongTrinh
docker compose up -d
```
> MariaDB 11.4 khởi chạy trên cổng `3307` (Database: `parking_db`, User: `parking_user`, Password: `parking_secret`).

### Bước 2: Chạy Backend (Spring Boot)
```powershell
cd ChuongTrinh/backend
.\mvnw.cmd spring-boot:run
```
> Backend API khởi động tại: `http://localhost:8080`  
> Kiểm tra trạng thái hệ thống: `http://localhost:8080/api/health`

### Bước 3: Chạy Web Frontend (React)
```bash
cd ChuongTrinh/web
npm install
npm run dev
```
> Giao diện web khởi động tại: `http://localhost:5173`

---

## 5. Tài khoản thử nghiệm (Demo)

| Tài khoản (Username) | Mật khẩu | Vai trò (Role) | Chức năng chính |
| :--- | :---: | :---: | :--- |
| `admin` | `password` | `ADMIN` | Quản lý sinh viên, kích hoạt/khóa thẻ, tạo gói cước, cài đặt biểu phí |
| *Mã số SV* (ví dụ: `SV20260001`) | `password` | `STUDENT` | Tra cứu thông tin, số dư ví, nạp ví nhanh, danh sách thẻ, tự xác thực 2FA khi lệch biển |

> **Lưu ý nghiệp vụ:** Hệ thống đồng nhất `username = studentCode` (MSSV) cho tài khoản sinh viên. Sinh viên đăng ký và đăng nhập trực tiếp bằng MSSV của mình.

---

## 6. Danh mục API cốt lõi

| Phương thức | Endpoint | Module | Mục đích |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Identity | Đăng ký tài khoản sinh viên mới (đồng nhất MSSV) |
| `POST` | `/api/auth/login` | Identity | Đăng nhập hệ thống, cấp token JWT |
| `GET` | `/api/auth/profile` | Identity | Lấy thông tin hồ sơ người dùng đang đăng nhập |
| `POST` | `/api/cards/link` | Card | Kích hoạt và liên kết thẻ RFID/NFC cho sinh viên |
| `GET` | `/api/cards/student/{studentId}` | Card | Tra cứu danh sách toàn bộ thẻ của sinh viên |
| `PATCH` | `/api/cards/{id}/lock` | Card | Khóa thẻ khẩn cấp khi bị mất (lưu vết kiểm toán) |
| `PATCH` | `/api/cards/{id}/unlock` | Card | Mở khóa lại thẻ (lưu vết kiểm toán) |
| `GET` | `/api/cards/{id}/history` | Card | Xem lịch sử thay đổi trạng thái thẻ |
| `POST` | `/api/parking/check-in` | Parking | Cổng tự động nhận diện xe vào (xác thực thẻ, chụp biển số, mở barrier) |
| `POST` | `/api/parking/check-out` | Parking | Cổng tự động nhận diện xe ra (đối soát biển số, trừ gói/ví, mở barrier) |
| `POST` | `/api/parking/sessions/{id}/self-verify-exit` | Parking | Sinh viên tự xác thực 2FA trên điện thoại mở barrier khi lệch biển số |
| `GET` | `/api/wallet/balance` | Wallet | Tra cứu số dư ví điện tử mô phỏng |
| `POST` | `/api/wallet/top-up` | Wallet | Nạp tiền ví mô phỏng (hỗ trợ Idempotency-Key) |

---

## 7. Tài liệu chi tiết của dự án

- **Kiến trúc hệ thống & Lộ trình triển khai:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Nhật ký giải quyết vấn đề nghiệp vụ & kiến trúc:** [`docs/VanDe_GiaiPhap.md`](docs/VanDe_GiaiPhap.md)
- **Lịch sử thay đổi hệ thống (Changelog):** [`docs/CHANGELOG.md`](docs/CHANGELOG.md)
