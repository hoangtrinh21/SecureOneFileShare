# OneShot File Sharing App

Ứng dụng chia sẻ tệp tin một lần an toàn cho phép người dùng tải lên và chia sẻ tệp với cơ chế tải xuống một lần duy nhất, sử dụng xác thực Google để truy cập an toàn.

## Tính năng chính

- Xác thực OAuth2 của Google
- Tải xuống tệp tin một lần với tính năng xóa tệp tự động
- Chia sẻ tệp tin an toàn với mã kết nối được tạo
- Giao diện web đáp ứng
- Tải lên tệp tin với giới hạn kích thước

## Thiết lập Google OAuth2

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Tạo ID khách hàng OAuth 2.0 mới
3. Thêm các URL chuyển hướng được ủy quyền sau:
   - Môi trường phát triển: `https://[DEV_DOMAIN]/login/oauth2/code/google` 
   - Môi trường sản xuất: `https://[PROD_DOMAIN]/login/oauth2/code/google`

Thay thế `[DEV_DOMAIN]` và `[PROD_DOMAIN]` bằng tên miền thực tế của ứng dụng Replit của bạn.

URL chuyển hướng cho phiên bản hiện tại:
```
https://406ebfde-f841-4fef-babf-ecfd5bb1766c-00-25zqc6tgeci9y.picard.replit.dev/login/oauth2/code/google
```

Lưu ý: Nếu bạn triển khai ứng dụng với tên miền khác, bạn cần cập nhật URL chuyển hướng trong Google Cloud Console.

## Biến môi trường

Ứng dụng yêu cầu các biến môi trường sau:

- `GOOGLE_OAUTH_CLIENT_ID`: ID khách hàng OAuth từ Google Cloud Console
- `GOOGLE_OAUTH_CLIENT_SECRET`: Khóa bí mật OAuth từ Google Cloud Console

## Hạn chế tệp tin

- Kích thước tệp tối đa: 2MB
- Thời gian hết hạn của tệp: 10 phút sau khi tải lên

## Quy tắc mã kết nối

- Hệ thống sử dụng chỉ 1% tổng số mã có thể ở mỗi độ dài (ví dụ: 1 mã cho độ dài 1, 38 mã cho độ dài 2)
- Chỉ kéo dài mã khi cần thiết dựa trên số lượng tệp đang hoạt động

## Tính năng bảo mật

- Chặn người dùng nhập sai mã 3 lần với thời gian chờ tăng theo cấp số nhân (2 phút, 4 phút, 16 phút)
- Người dùng tải lên không thể tải xuống tệp của chính họ
- Liên kết tải xuống hết hạn sau 3 phút