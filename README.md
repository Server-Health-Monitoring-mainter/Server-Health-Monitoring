# Server Health Monitoring – Hệ thống giám sát server từ xa

Monitoring Client kết nối đến nhiều Agent Server (tự xây dựng). Agent Server gửi các chỉ số
sức khỏe (**CPU Load**, **Memory Usage**, **Số lượng kết nối**) qua **TCP/UDP** cho Client
để hiển thị và lưu trữ.

- Ngôn ngữ: Java (Swing, Socket TCP/UDP)
- Công cụ: Apache NetBeans (Java with Ant)

## Kiến trúc

```
 Agent Server 1 ─┐
 Agent Server 2 ─┼── TCP / UDP ──► Monitoring Client ──► Hiển thị + Lưu trữ (CSV)
 Agent Server 3 ─┘
```

## Quy trình làm việc nhóm

1. Mỗi việc là một **Issue** (gán đúng 1 người, có Label và Milestone).
2. Tạo nhánh từ `main`: `feature/<MSSV>-issue-<số>-<tên-ngắn>`
3. Commit theo chuẩn: `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`
4. Push nhánh, mở **Pull Request** (mô tả + ảnh chụp chạy thành công).
5. Ít nhất **1 thành viên khác** review và Approve rồi mới Merge.

## Bảng đóng góp

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests |
|---|---|---|---|
| 2380601037 | Phạm Duy Khanh (Nhóm trưởng) | Lớp dùng chung `Metrics`, `Protocol`; quản lý repo, Issue, review; tích hợp hệ thống | #2 |
| 2380601519 | Lê Hữu Nhân | Thu thập chỉ số CPU, RAM, số kết nối (`MetricsCollector`) | |
| 2380600722 | Ngô Ngọc Quốc Hoàng | Agent Server: mở cổng TCP + UDP 6000, gửi `metrics.toLine()` cho Client | |
| 2380601031 | Nguyễn Quang Khánh | Kết nối Client tới nhiều Agent qua TCP/UDP | |
| 2380600962 | Trần Quang Khải | Giao diện Monitoring Client: bảng, cảnh báo, lưu trữ | |
