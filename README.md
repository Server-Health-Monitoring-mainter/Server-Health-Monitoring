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
| 2380601037 | Phạm Duy Khanh (Nhóm trưởng) | Lớp dùng chung Metrics, Protocol; tích hợp hệ thống | |
| | | Thu thập chỉ số CPU, RAM, số kết nối | |
| | | Agent Server TCP/UDP | |
| | Nguyễn Quang Khánh | Kết nối Client TCP/UDP | |
| | | Giao diện Client, cảnh báo, lưu trữ | |
