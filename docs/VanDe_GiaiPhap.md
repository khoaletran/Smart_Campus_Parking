# Nhật Ký Vấn Đề Nghiệp Vụ & Giải Pháp Kiến Trúc (Business Problem-Solving Log)

Tài liệu này lưu trữ toàn bộ các **vấn đề nghiệp vụ thực tế**, phân tích nguyên nhân và **giải pháp kiến trúc chuẩn xác** đã được thống nhất trong quá trình phát triển hệ thống **Smart Campus Parking**.  
Tài liệu dùng làm cơ sở vững chắc để sinh viên bảo vệ trước Hội đồng chấm đồ án tốt nghiệp.

---

## 1. Nghiệp vụ Quản lý Phương tiện & Biển số xe Sinh viên

### Vấn đề:
Thiết kế ban đầu định gắn trường `default_license_plate` cố định vào bảng hồ sơ sinh viên (`students`), ép mỗi sinh viên chỉ được đăng ký 1 biển số xe duy nhất.

### Phân tích nghiệp vụ:
Trong thực tế đời sống đại học:
- Sinh viên có thể hôm nay đi xe của mình, ngày mai đi xe của phụ huynh, mượn xe người thân hoặc đổi xe mới.
- Việc ép sinh viên chỉ gửi đúng 1 biển số xe cố định gây bất tiện lớn và không phản ánh đúng thực tế bãi xe trường học.
- Mục tiêu an ninh cốt lõi của bãi xe là: **Chống tráo xe và chống mất trộm xe trong từng lượt gửi**, nghĩa là chiếc xe lấy ra lúc Check-out phải là chính chiếc xe mang vào lúc Check-in.

### Giải pháp kiến trúc:
1. **Loại bỏ hoàn toàn** thuộc tính `default_license_plate` khỏi lớp `Student` và bảng `students`.
2. Chuyển toàn bộ cơ chế đối soát sang **từng phiên gửi xe độc lập (`ParkingSession`)**:
   - Khi xe vào (Check-in): Camera ANPR chụp ảnh và lưu `entry_plate_number`, `entry_image_url`.
   - Khi xe ra (Check-out): Camera ANPR chụp ảnh và lưu `exit_plate_number`, `exit_image_url`.
   - Hệ thống tự động so sánh: `entry_plate_number` == `exit_plate_number`. Nếu trùng khớp $\rightarrow$ Mở barrier tự động.

---

## 2. Xác thực 2FA khi Lệch biển số tại Cổng Headless mà không dùng Mã PIN riêng

### Vấn đề:
Khi camera phát hiện lệch biển số xe vào và ra (`plateMismatch = true`), hệ thống cần cơ chế bảo mật để sinh viên tự xác thực trách nhiệm cá nhân mở barrier. Ý tưởng ban đầu định thêm trường `pin_code_hash` vào bảng `students`. Tuy nhiên, trong Sơ đồ lớp (Class Diagram) và Sơ đồ EER chính thức của đề tài **hoàn toàn không có thuộc tính PIN**.

### Phân tích nghiệp vụ:
- Cổng bãi xe được thiết kế theo mô hình **Headless (Zero-Human)**: Không có bảo vệ trực chốt, không có màn hình cảm ứng ngoài trời.
- Nếu tự ý thêm mã PIN vào code, khi chấm đồ án Hội đồng sẽ bắt bẻ: *"Tại sao Sơ đồ lớp đã báo cáo không có PIN mà trong code lại có?"*
- Sinh viên khi dùng điện thoại di động vốn **đã đăng nhập vào hệ thống bằng tài khoản và có Token JWT hợp lệ**.

### Giải pháp kiến trúc:
1. **Không thêm mã PIN** vào Entity hay Database để giữ tài liệu và code khớp 100%.
2. Tận dụng chính cơ chế xác thực hiện có:
   - Khi lệch biển số, barrier khóa tự động và còi báo động kêu.
   - Sinh viên mở điện thoại (Mobile Web/App), đối chiếu trực quan ảnh chụp lúc vào và ảnh chụp lúc ra.
   - Sinh viên xác nhận trách nhiệm bằng **chính Mật khẩu tài khoản cá nhân (`password`)** hoặc **bấm nút cam kết trách nhiệm đã được bảo vệ qua Token JWT**.
   - Hệ thống lưu vết pháp lý vào phiên gửi xe: `verified_by = 'STUDENT_SELF_2FA'` và tự động mở barrier.

---

## 3. Đồng nhất Tên đăng nhập (`username`) và Mã số sinh viên (`studentCode`)

### Vấn đề:
Lớp `User` có `username`, lớp `Student` có `studentCode` (MSSV). Nếu để người dùng tự tạo `username` tùy ý (ví dụ: `hoangtuech123`) thì sinh viên phải nhớ cả 2 thông tin, dễ quên và gây phiền hà.

### Phân tích nghiệp vụ:
Ở các trường đại học, sinh viên luôn có thói quen dùng duy nhất **Mã số sinh viên (MSSV)** làm tài khoản đăng nhập vào mọi cổng thông tin đào tạo, thư viện và dịch vụ.

### Giải pháp kiến trúc:
1. **Bảo toàn Sơ đồ lớp và CSDL:** Giữ nguyên 2 lớp `User` (có `username`) và `Student` (có `studentCode`) để tài khoản Quản trị viên (`admin`) vẫn sử dụng được username `admin` bình thường.
2. **Đồng nhất ở tầng nghiệp vụ:**
   - Trong form đăng ký của sinh viên (`RegisterStudentRequest`): Bỏ trường `username`, sinh viên chỉ cần điền `studentCode` (MSSV).
   - Tầng Service tự động gán: `user.setUsername(req.getStudentCode())`.
   - Khi đăng nhập: Sinh viên gõ trực tiếp MSSV vào ô đăng nhập.

---

## 4. Cho phép Một Sinh viên Sở hữu Nhiều Thẻ Sinh Viên Active

### Vấn đề:
Ban đầu hệ thống định giới hạn mỗi sinh viên chỉ được sở hữu duy nhất 1 thẻ ở trạng thái `ACTIVE`.

### Phân tích nghiệp vụ:
- Trong Sơ đồ EER chính thức của đề tài, mối quan hệ giữa `SINHVIEN` và `THESINHVIEN` là **`(1, n)`** (Một sinh viên có thể sở hữu nhiều thẻ).
- Trong thực tế: Sinh viên có thể sở hữu 1 thẻ cứng chính, 1 thẻ phụ dự phòng hoặc thẻ NFC tích hợp trên thiết bị di động.

### Giải pháp kiến trúc:
1. Cho phép 1 sinh viên kích hoạt nhiều thẻ `ACTIVE` cùng lúc. Bảng `cards` chỉ đặt chỉ mục `UNIQUE` trên `card_uid` (mỗi phôi chip chỉ gắn cho 1 thẻ), không đặt `UNIQUE` trên `student_id`.
2. Khi sinh viên làm mất một thẻ, họ chỉ cần vào app khóa đúng chiếc thẻ đó (`LOCKED`). Các thẻ còn lại của sinh viên vẫn hoạt động bình thường để gửi xe.

---

## 5. Quản lý Phiên gửi xe khi Sinh viên có Nhiều Thẻ: Chính sách "1 Sinh Viên — 1 Suất Để Xe" (Single-Vehicle Policy with Backup Cards)

### Vấn đề:
Khi một sinh viên sở hữu 2-3 thẻ active (thẻ cứng chính, thẻ phụ, thẻ điện thoại), cần xác định rõ quy tắc nghiệp vụ: Sinh viên có được gửi nhiều xe cùng lúc không, hay các thẻ phụ chỉ đóng vai trò là "chìa khóa dự phòng"?

### Phân tích nghiệp vụ đại học:
- **Chống gian lận & Quá tải bãi xe:** Bãi xe trường đại học phục vụ sinh viên trong giờ học và thường có chính sách giá ưu đãi/gói cước tháng. Nếu cho phép 1 sinh viên gửi nhiều xe cùng lúc, sinh viên có thể đưa các thẻ phụ cho người ngoài hoặc bạn bè mượn để gửi ké xe giá rẻ, làm thất thoát nguồn thu và quá tải sức chứa của bãi xe.
- **Nỗi đau quên thẻ trong cốp xe:** Tình huống xảy ra thường xuyên nhất là sinh viên quẹt Thẻ 1 vào bãi, cất ví vào cốp xe rồi khóa lại. Đến giờ về, sinh viên không mở được cốp để lấy thẻ quẹt ra.

### Giải pháp kiến trúc thống nhất: **Chính sách 1 Sinh Viên — 1 Suất Để Xe (Single-Vehicle Policy)**
1. **Lúc vào (Check-in) — Khóa chặt xe thứ 2:**
   - Khi quẹt bất kỳ thẻ nào vào cổng, hệ thống lấy `studentId` của thẻ và kiểm tra: *"Sinh viên này đã có phiên xe nào đang `IN_PROGRESS` trong bãi chưa?"*.
   - Nếu **ĐÃ CÓ XE TRONG BÃI** $\rightarrow$ Cổng lập tức phát 3 tiếng bíp dài, đèn đỏ, **từ chối mở barrier** (Ngăn chặn triệt để hành vi đưa thẻ phụ cho người khác gửi xe thứ 2).
2. **Lúc ra (Check-out) — Cứu nguy khi quên thẻ:**
   - Khi xe ra, sinh viên có thể dùng **BẤT KỲ THẺ NÀO (Thẻ 1, Thẻ 2, Thẻ 3)** thuộc quyền sở hữu của chính sinh viên đó để quẹt.
   - Hệ thống tìm phiên xe `IN_PROGRESS` của `studentId` đó $\rightarrow$ Camera ANPR đối soát biển số xe ra với biển số xe vào của phiên đó.
   - Trùng khớp biển số $\rightarrow$ Tự động trừ phí/gói và **mở barrier ngay lập tức**.
   - Sinh viên quên Thẻ 1 trong cốp xe chỉ cần rút Thẻ phụ 2 hoặc mở điện thoại ra quẹt là lấy xe ra an toàn, mượt mà!

---

## 6. Kiểm toán Nghiệp vụ (Audit Trail) mà Không Làm Thừa Bảng CSDL

### Vấn đề:
Ban đầu dự kiến tạo thêm bảng `audit_logs` để ghi nhật ký hệ thống. Tuy nhiên, Sơ đồ EER chính thức đã duyệt chỉ có đúng **10 thực thể (10 bảng)**. Việc tự ý thêm bảng thứ 11 sẽ làm sai lệch tài liệu đồ án.

### Phân tích nghiệp vụ:
Hệ thống cần lưu vết các hành động nhạy cảm (đổi trạng thái thẻ, biến động số dư ví, đối soát lệch biển số xe) để phục vụ kiểm toán minh bạch.

### Giải pháp kiến trúc:
Tích hợp tính chất lưu vết kiểm toán (Audit Trail) trực tiếp vào chính **3 bảng nghiệp vụ cốt lõi**:
1. **`card_status_history`**: Ghi vết bất biến mọi lần thay đổi trạng thái thẻ (`old_status`, `new_status`, `reason`, `changed_by`, `changed_at`).
2. **`wallet_transactions`**: Ghi vết bất biến mọi giao dịch ví (`amount`, `balance_before`, `balance_after`, `idempotency_key`, `tx_type`).
3. **`parking_sessions`**: Ghi vết an ninh bãi xe (`plate_mismatch`, `verified_by = 'STUDENT_SELF_2FA'`).  
$\rightarrow$ Giữ đúng chuẩn 10 bảng CSDL theo Sơ đồ EER, không phát sinh bảng thừa.

---

## 7. Ranh giới Module Độc lập theo Chuẩn DDD (Modular Monolith)

### Vấn đề:
Khi viết mã nguồn, lập trình viên thường có thói quen inject trực tiếp Repository của module khác (ví dụ: `CardService` inject thẳng `StudentRepository` của Module `identity`).

### Phân tích kiến trúc:
Cách làm này vi phạm nghiêm trọng ranh giới module (Bounded Contexts), gây thắt nút chặt (Tight Coupling). Nếu sau này muốn tách một module thành Microservice, toàn bộ code sẽ bị vỡ.

### Giải pháp kiến trúc:
1. **Tuyệt đối không inject Repository xuyên module.**
2. Mọi giao tiếp liên module phải đi qua **Interface Module API công khai** (`IdentityModuleApi`, `CardModuleApi`, `WalletModuleApi`) và trao đổi dữ liệu thuần túy qua **DTO** (`StudentDto`, `CardDto`).
