# Server Health Monitoring – Hệ thống giám sát server từ xa

Monitoring Client kết nối đến nhiều Agent Server (tự xây dựng). Agent Server gửi các chỉ số
sức khỏe (**CPU Load**, **Memory Usage**, **Số lượng kết nối**) qua **TCP/UDP** cho Client
để hiển thị, cảnh báo và lưu trữ.

- Ngôn ngữ: Java (JDK 22 trở lên, theo cấu hình project), Swing, Socket TCP/UDP
- Công cụ: Apache NetBeans (Java with Ant)

## Kiến trúc

```
 Agent Server 1 ─┐
 Agent Server 2 ─┼── TCP / UDP (cổng 6000) ──► Monitoring Client ──► Bảng + cảnh báo
 Agent Server 3 ─┘                                     │
                                                       └──► Lưu trữ CSV (data/)
```

| Thành phần | Lớp chính | Vai trò |
|---|---|---|
| Khởi động | `ServerHealthMonitoring` | Chọn chạy Client hoặc Agent theo tham số |
| Dùng chung | `Metrics`, `Protocol` | Định dạng dữ liệu và quy ước giao tiếp |
| Agent Server | `MetricsCollector`, `AgentServer` | Đo CPU/RAM thật, đếm kết nối, mở TCP + UDP |
| Kết nối Client | `TcpConnection`, `UdpConnection`, `AgentManager` | Kết nối và quản lý nhiều Agent |
| Giao diện | `ClientFrame` | Bảng Agent, tô màu cảnh báo, nhật ký |
| Lưu trữ | `MetricsLogger` | Ghi mỗi lần nhận số liệu vào `data/metrics-YYYY-MM-DD.csv` |

## Giao thức

Mỗi gói dữ liệu là **một dòng văn bản**:

```
METRICS|name=web-01|cpu=23.5|memUsed=4096|memTotal=8192|conn=3
```

- **TCP:** Client kết nối tới Agent; Agent tự gửi 1 dòng `METRICS` mỗi 2 giây (`Protocol.INTERVAL_MS`). Client gửi `QUIT` để ngắt.
- **UDP:** Client gửi `GET`, Agent trả về 1 gói chứa 1 dòng `METRICS`.
- Cổng mặc định: **6000** (`Protocol.DEFAULT_PORT`), dùng chung cho TCP và UDP.

## Hướng dẫn chạy

### Cách 1 – Trong NetBeans
1. Mở project, bấm **Clean and Build** (Shift + F11).
2. **Chạy Client:** bấm **Run Project** (F6) → mở cửa sổ Monitoring Client.
3. **Chạy Agent:** chuột phải project → **Properties → Run → Arguments** nhập `agent --name web-01` → bấm F6. (Hoặc chuột phải `AgentServer.java` → **Run File**.)
4. Trên Client nhập **Host** (vd. `localhost` hoặc IP máy chạy Agent), **Cổng** `6000`, chọn **TCP** hoặc **UDP** → **Thêm agent**.

### Cách 2 – Chạy file jar (sau khi Clean and Build)
```
# Chạy Client
java -jar dist/ServerHealthMonitoring.jar

# Chạy Agent (mỗi máy cần giám sát chạy 1 Agent; không có --name thì lấy tên máy)
java -jar dist/ServerHealthMonitoring.jar agent --port 6000 --name web-01

# Xem hướng dẫn
java -jar dist/ServerHealthMonitoring.jar --help
```

### Chạy trên nhiều máy
- Các máy cùng mạng LAN; trên máy chạy Agent mở cổng **6000 TCP và UDP** trong Windows Firewall.
- Lấy IP máy Agent bằng lệnh `ipconfig` (dòng IPv4), nhập IP đó vào ô Host của Client.

### Cảnh báo và lưu trữ
- Dòng trong bảng được tô màu khi **CPU > 80 %**, **RAM > 90 %** hoặc **mất kết nối**; nhật ký ghi `[CẢNH BÁO]` / `[PHỤC HỒI]`.
- Số liệu được lưu tự động vào `data/metrics-YYYY-MM-DD.csv` (mở được bằng Excel). Thư mục `data/` không đưa lên Git.

## Kiểm thử

Chuột phải từng file → **Run File**, kết quả mong đợi `KET QUA: DUNG`:

| File test | Kiểm tra |
|---|---|
| `MetricsTest` | Đóng gói / đọc lại dòng `METRICS`, từ chối dữ liệu sai |
| `MetricsCollectorTest` | CPU 0–100 %, RAM hợp lệ |
| `AgentServerTest` | Agent trả dữ liệu qua TCP, UDP và xử lý `QUIT` |
| `TcpConnectionTest` | Client nhận dữ liệu TCP, báo mất kết nối |
| `AgentManagerTest` | Quản lý 2 Agent cùng lúc (TCP + UDP) |
| `MetricsLoggerTest` | Ghi CSV đúng tiêu đề, nội dung, nhiều luồng |
| `ClientFrameTest` | Giao diện hiển thị và tô màu với dữ liệu mẫu |

## Quy trình làm việc nhóm

1. Mỗi việc là một **Issue** (gán đúng 1 người, có Label và Milestone *Tuần 1* / *Tuần 2*).
2. Tạo nhánh từ `main`: `feature/<MSSV>-issue-<số>-<tên-ngắn>`.
3. Đặt `git config user.name "<MSSV>_<HoTen>"`; commit theo chuẩn `feat:`, `fix:`, `refactor:`, `docs:`, `test:` kèm số Issue.
4. Push nhánh, mở **Pull Request** (mô tả, ảnh chụp chạy thành công, `Closes #<số>`).
5. Ít nhất **1 thành viên khác** review và Approve rồi mới Merge; nhánh `main` được bảo vệ.

## Bảng đóng góp

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests |
|---|---|---|---|
| 2380601037 | Phạm Duy Khanh (Nhóm trưởng) | Lớp dùng chung `Metrics`, `Protocol`; quản lý repo, Issue, Milestone; review và merge PR; kiểm tra dữ liệu trong `Metrics.parse()`; `main()` tích hợp chạy Agent/Client; README | #2, #23, #24, #25 |
| 2380601519 | Lê Hữu Nhân | `MetricsCollector` đo CPU, RAM thật; `MetricsLogger` lưu dữ liệu ra file CSV | #9, #13, #19 |
| 2380600722 | Ngô Ngọc Quốc Hoàng | `AgentServer` mở TCP + UDP cổng 6000, đếm kết nối, xử lý `GET`/`QUIT`, tích hợp `MetricsCollector` | #10, #22 |
| 2380601031 | Nguyễn Quang Khánh | `TcpConnection`, `UdpConnection`, `AgentManager` kết nối Client tới nhiều Agent | #7, #12, #21 |
| 2380600962 | Trần Quang Khải | Giao diện `ClientFrame`: bảng Agent, thêm/xóa Agent, cảnh báo màu, nhật ký, ghép với `AgentManager` và `MetricsLogger` | #11, #20 |
