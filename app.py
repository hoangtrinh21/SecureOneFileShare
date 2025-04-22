import os
import json
import requests
from flask import Flask, redirect, request, url_for, session, render_template
from flask_login import LoginManager, login_user, logout_user, login_required, current_user, UserMixin
from oauthlib.oauth2 import WebApplicationClient
import secrets
from datetime import datetime, timedelta
import os

# Thông tin Google OAuth
GOOGLE_CLIENT_ID = os.environ.get("GOOGLE_OAUTH_CLIENT_ID")
GOOGLE_CLIENT_SECRET = os.environ.get("GOOGLE_OAUTH_CLIENT_SECRET")
GOOGLE_DISCOVERY_URL = "https://accounts.google.com/.well-known/openid-configuration"

# Lấy URL redirect từ biến môi trường của Replit
REPLIT_DOMAIN = os.environ.get("REPL_SLUG") + "." + os.environ.get("REPL_OWNER") + ".repl.co"
REDIRECT_URI = f"https://{os.environ.get('REPLIT_DEV_DOMAIN')}/google_login/callback"

print(f"""
Google OAuth Setup Instructions:
-------------------------------
1. Go to https://console.cloud.google.com/apis/credentials
2. Create a new OAuth 2.0 Client ID
3. Add the following URL to Authorized redirect URIs:
   {REDIRECT_URI}
""")

app = Flask(__name__)
app.secret_key = secrets.token_hex(16)
app.config['MAX_CONTENT_LENGTH'] = 2 * 1024 * 1024  # 2MB max file size
app.config['UPLOAD_FOLDER'] = 'uploads'

# Đảm bảo thư mục uploads tồn tại
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# Cấu hình login manager
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'index'

# Tạo client để giao tiếp với Google OAuth2
client = WebApplicationClient(GOOGLE_CLIENT_ID)

# Danh sách lưu trữ tệp đã tải lên và mã kết nối
class FileData:
    def __init__(self, filename, uploader, original_filename, size):
        self.filename = filename
        self.uploader = uploader
        self.original_filename = original_filename
        self.size = size
        self.created_at = datetime.now()
        self.connection_code = None
        self.expires_at = datetime.now() + timedelta(minutes=10)
        self.download_expires_at = None

# Lưu trữ thông tin người dùng
class User(UserMixin):
    def __init__(self, id, name, email):
        self.id = id
        self.name = name
        self.email = email

# Lưu trữ tạm người dùng và file trong bộ nhớ (trong ứng dụng thực tế, đây sẽ là cơ sở dữ liệu)
users_db = {}
files_db = {}
connection_codes = {}
failed_attempts = {}

@login_manager.user_loader
def load_user(user_id):
    return users_db.get(user_id)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/google_login')
def google_login():
    # Tìm endpoint URL xác thực của Google
    google_provider_cfg = requests.get(GOOGLE_DISCOVERY_URL).json()
    authorization_endpoint = google_provider_cfg["authorization_endpoint"]

    # Tạo URL redirect đến Google OAuth
    request_uri = client.prepare_request_uri(
        authorization_endpoint,
        redirect_uri=REDIRECT_URI,
        scope=["openid", "email", "profile"],
    )
    
    return redirect(request_uri)

@app.route('/google_login/callback')
def google_callback():
    # Lấy authorization code từ Google
    code = request.args.get("code")
    
    # Lấy các endpoints từ Google
    google_provider_cfg = requests.get(GOOGLE_DISCOVERY_URL).json()
    token_endpoint = google_provider_cfg["token_endpoint"]
    
    # Chuẩn bị và gửi yêu cầu để lấy token
    token_url, headers, body = client.prepare_token_request(
        token_endpoint,
        authorization_response=request.url,
        redirect_url=REDIRECT_URI,
        code=code
    )
    token_response = requests.post(
        token_url,
        headers=headers,
        data=body,
        auth=(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET),
    )
    
    # Parse token response
    client.parse_request_body_response(json.dumps(token_response.json()))
    
    # Lấy thông tin người dùng từ Google
    userinfo_endpoint = google_provider_cfg["userinfo_endpoint"]
    uri, headers, body = client.add_token(userinfo_endpoint)
    userinfo_response = requests.get(uri, headers=headers, data=body)
    
    # Kiểm tra email đã được xác minh bởi Google
    if userinfo_response.json().get("email_verified"):
        unique_id = userinfo_response.json()["sub"]
        users_email = userinfo_response.json()["email"]
        users_name = userinfo_response.json()["given_name"]
    else:
        return "Email của người dùng không khả dụng hoặc không được xác minh bởi Google.", 400
    
    # Tạo một đối tượng người dùng
    user = User(id=unique_id, name=users_name, email=users_email)
    
    # Lưu trữ người dùng
    users_db[unique_id] = user
    
    # Đăng nhập người dùng
    login_user(user)
    
    # Chuyển hướng đến trang upload
    return redirect(url_for("upload"))

@app.route('/upload')
@login_required
def upload():
    # Hiển thị form tải lên file
    return render_template('upload.html')

@app.route('/api/upload', methods=['POST'])
@login_required
def api_upload():
    # Xử lý tệp tải lên
    if 'file' not in request.files:
        return {"error": "Không có tệp nào được chọn"}, 400
    
    file = request.files['file']
    
    if file.filename == '':
        return {"error": "Không có tệp nào được chọn"}, 400
    
    # Tạo tên tệp duy nhất
    original_filename = file.filename
    unique_filename = f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{secrets.token_hex(8)}_{original_filename}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    
    # Lưu tệp
    file.save(file_path)
    
    # Lưu thông tin tệp
    file_size = os.path.getsize(file_path)
    file_data = FileData(
        unique_filename, 
        current_user.id, 
        original_filename,
        file_size
    )
    
    # Tạo mã kết nối
    connection_code = generate_connection_code()
    file_data.connection_code = connection_code
    
    # Lưu vào "cơ sở dữ liệu"
    files_db[unique_filename] = file_data
    connection_codes[connection_code] = unique_filename
    
    return {
        "success": True,
        "connectionCode": connection_code,
        "expiresAt": file_data.expires_at.isoformat()
    }

@app.route('/download')
def download_page():
    # Hiển thị trang nhập mã kết nối
    return render_template('download.html')

@app.route('/api/download', methods=['POST'])
def api_download():
    data = request.get_json()
    connection_code = data.get('connectionCode')
    
    if not connection_code:
        return {"error": "Vui lòng nhập mã kết nối"}, 400
    
    # Kiểm tra mã kết nối
    if connection_code not in connection_codes:
        # Xử lý nếu mã không hợp lệ
        client_ip = request.remote_addr
        
        if client_ip not in failed_attempts:
            failed_attempts[client_ip] = {
                "count": 1,
                "locked_until": None
            }
        else:
            # Kiểm tra xem có đang bị khóa không
            if failed_attempts[client_ip]["locked_until"] and datetime.now() < failed_attempts[client_ip]["locked_until"]:
                remaining = (failed_attempts[client_ip]["locked_until"] - datetime.now()).total_seconds()
                return {
                    "error": "Quá nhiều lần thử không thành công",
                    "lockoutRemaining": int(remaining)
                }, 429
            
            # Tăng số lần thử không thành công
            failed_attempts[client_ip]["count"] += 1
            
            # Nếu đã thử 3 lần không thành công, khóa trong một khoảng thời gian
            if failed_attempts[client_ip]["count"] >= 3:
                # Thời gian khóa tăng theo cấp số nhân: 2 phút, 4 phút, 16 phút
                lockout_minutes = 2 ** (failed_attempts[client_ip]["count"] - 2)
                failed_attempts[client_ip]["locked_until"] = datetime.now() + timedelta(minutes=lockout_minutes)
                
                return {
                    "error": "Quá nhiều lần thử không thành công",
                    "lockoutRemaining": lockout_minutes * 60
                }, 429
        
        return {"error": "Mã kết nối không hợp lệ"}, 404
    
    # Lấy thông tin tệp
    filename = connection_codes[connection_code]
    file_data = files_db.get(filename)
    
    if not file_data:
        return {"error": "Không tìm thấy tệp"}, 404
    
    # Kiểm tra xem tệp có hết hạn không
    if datetime.now() > file_data.expires_at:
        # Xóa tệp hết hạn
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        if os.path.exists(file_path):
            os.remove(file_path)
        
        # Xóa khỏi "cơ sở dữ liệu"
        del files_db[filename]
        del connection_codes[connection_code]
        
        return {"error": "Mã kết nối đã hết hạn"}, 410
    
    # Kiểm tra xem người tải lên có đang cố gắng tải xuống tệp của chính họ không
    if current_user.is_authenticated and current_user.id == file_data.uploader:
        return {"error": "Bạn không thể tải xuống tệp do chính mình tải lên"}, 403
    
    # Đặt thời gian hết hạn tải xuống (3 phút)
    file_data.download_expires_at = datetime.now() + timedelta(minutes=3)
    
    # Trả về thông tin tệp
    return {
        "success": True,
        "filename": file_data.original_filename,
        "size": file_data.size,
        "downloadToken": secrets.token_hex(16),  # Token tạm thời cho việc tải xuống
        "downloadExpiresAt": file_data.download_expires_at.isoformat()
    }

@app.route('/api/download/<download_token>/<connection_code>', methods=['GET'])
def download_file(download_token, connection_code):
    # Kiểm tra mã kết nối
    if connection_code not in connection_codes:
        return "Mã kết nối không hợp lệ", 404
    
    # Lấy thông tin tệp
    filename = connection_codes[connection_code]
    file_data = files_db.get(filename)
    
    if not file_data:
        return "Không tìm thấy tệp", 404
    
    # Kiểm tra xem thời gian tải xuống có hết hạn không
    if not file_data.download_expires_at or datetime.now() > file_data.download_expires_at:
        return "Liên kết tải xuống đã hết hạn", 410
    
    # Kiểm tra xem người tải lên có đang cố gắng tải xuống tệp của chính họ không
    if current_user.is_authenticated and current_user.id == file_data.uploader:
        return "Bạn không thể tải xuống tệp do chính mình tải lên", 403
    
    # Đường dẫn đến tệp
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    
    if not os.path.exists(file_path):
        return "Không tìm thấy tệp", 404
    
    # Xóa tệp và thông tin sau khi tải xuống
    from flask import send_file, after_this_request
    
    @after_this_request
    def delete_file(response):
        try:
            os.remove(file_path)
            del files_db[filename]
            del connection_codes[connection_code]
        except Exception as e:
            app.logger.error(f"Lỗi khi xóa tệp: {e}")
        return response
    
    return send_file(
        file_path,
        as_attachment=True,
        download_name=file_data.original_filename
    )

@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

def generate_connection_code():
    """Tạo mã kết nối duy nhất."""
    # Tính toán độ dài mã dựa trên số lượng tệp đang chờ
    pending_files_count = len(files_db)
    
    # Bắt đầu với mã độ dài 1
    code_length = 1
    
    # Tính toán tổng số mã có thể có ở mỗi độ dài
    # Chỉ sử dụng 36 ký tự (a-z, 0-9)
    total_possible_codes = 36 ** code_length
    
    # Chỉ sử dụng 1% số mã có thể có ở mỗi độ dài
    available_codes = max(1, int(total_possible_codes * 0.01))
    
    # Tăng độ dài mã cho đến khi chúng ta có đủ mã
    while pending_files_count >= available_codes:
        code_length += 1
        total_possible_codes = 36 ** code_length
        available_codes = max(1, int(total_possible_codes * 0.01))
    
    # Tạo mã ngẫu nhiên với độ dài đã tính
    while True:
        code = ''.join(secrets.choice('abcdefghijklmnopqrstuvwxyz0123456789') for _ in range(code_length))
        if code not in connection_codes:
            return code

# Tác vụ dọn dẹp chạy định kỳ để xóa các tệp đã hết hạn
def cleanup_expired_files():
    """Xóa các tệp đã hết hạn."""
    now = datetime.now()
    expired_files = []
    
    for filename, file_data in list(files_db.items()):
        if now > file_data.expires_at:
            expired_files.append(filename)
    
    for filename in expired_files:
        # Xóa tệp
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        if os.path.exists(file_path):
            os.remove(file_path)
        
        # Xóa mã kết nối
        connection_code = files_db[filename].connection_code
        if connection_code in connection_codes:
            del connection_codes[connection_code]
        
        # Xóa thông tin tệp
        del files_db[filename]
    
    return f"Đã dọn dẹp {len(expired_files)} tệp hết hạn"

@app.route('/api/cleanup', methods=['POST'])
def api_cleanup():
    """API để kích hoạt dọn dẹp thủ công."""
    result = cleanup_expired_files()
    return {"message": result}

if __name__ == '__main__':
    # Tạo thư mục uploads nếu chưa tồn tại
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    
    # Chạy ứng dụng trên host 0.0.0.0 để có thể truy cập từ bên ngoài
    app.run(host='0.0.0.0', port=5000, debug=True)