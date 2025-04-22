document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('upload-form');
    const fileUpload = document.getElementById('file-upload');
    const fileName = document.getElementById('file-name');
    const progressContainer = document.querySelector('.progress-container');
    const progressBar = document.getElementById('upload-progress');
    const progressText = document.querySelector('.progress-text');
    const uploadResult = document.getElementById('upload-result');
    const connectionCode = document.getElementById('connection-code');
    const copyCode = document.getElementById('copy-code');
    const expirationTimer = document.getElementById('expiration-timer');
    const uploadAnother = document.getElementById('upload-another');

    // Xử lý hiển thị tên tệp khi người dùng chọn tệp
    fileUpload.addEventListener('change', function() {
        if (this.files.length > 0) {
            const file = this.files[0];
            fileName.textContent = `${file.name} (${formatFileSize(file.size)})`;
            
            // Kiểm tra kích thước tệp
            if (file.size > 2 * 1024 * 1024) { // 2MB
                alert('Tệp quá lớn. Vui lòng chọn tệp nhỏ hơn 2MB.');
                this.value = '';
                fileName.textContent = 'Chọn tệp để tải lên (tối đa 2MB)';
            }
        } else {
            fileName.textContent = 'Chọn tệp để tải lên (tối đa 2MB)';
        }
    });

    // Xử lý form tải lên
    uploadForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (!fileUpload.files.length) {
            alert('Vui lòng chọn tệp để tải lên.');
            return;
        }
        
        const file = fileUpload.files[0];
        const formData = new FormData();
        formData.append('file', file);
        
        // Hiển thị progress bar
        progressContainer.classList.remove('hidden');
        progressBar.style.width = '0%';
        progressText.textContent = '0%';
        
        // Gửi yêu cầu tải lên
        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/upload', true);
        
        // Theo dõi tiến trình tải lên
        xhr.upload.addEventListener('progress', function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                progressBar.style.width = percentComplete + '%';
                progressText.textContent = percentComplete + '%';
            }
        });
        
        // Xử lý khi tải lên hoàn tất
        xhr.addEventListener('load', function() {
            if (xhr.status === 200) {
                const response = JSON.parse(xhr.responseText);
                
                if (response.success) {
                    // Ẩn form và hiển thị kết quả
                    uploadForm.classList.add('hidden');
                    uploadResult.classList.remove('hidden');
                    
                    // Hiển thị mã kết nối
                    connectionCode.textContent = response.connectionCode;
                    
                    // Bắt đầu đếm ngược thời gian hết hạn
                    startExpirationCountdown(new Date(response.expiresAt));
                } else {
                    alert('Lỗi: ' + response.error);
                }
            } else {
                alert('Có lỗi xảy ra khi tải lên. Vui lòng thử lại.');
            }
            
            // Ẩn progress bar
            progressContainer.classList.add('hidden');
        });
        
        // Xử lý lỗi
        xhr.addEventListener('error', function() {
            alert('Lỗi kết nối. Vui lòng kiểm tra kết nối internet và thử lại.');
            progressContainer.classList.add('hidden');
        });
        
        // Gửi yêu cầu
        xhr.send(formData);
    });
    
    // Xử lý sao chép mã kết nối
    copyCode.addEventListener('click', function() {
        const textToCopy = connectionCode.textContent;
        
        // Sử dụng Clipboard API nếu được hỗ trợ
        if (navigator.clipboard) {
            navigator.clipboard.writeText(textToCopy)
                .then(() => {
                    // Thay đổi nút tạm thời để thông báo đã sao chép
                    const originalText = this.textContent;
                    this.textContent = 'Đã sao chép!';
                    setTimeout(() => {
                        this.textContent = originalText;
                    }, 2000);
                })
                .catch(err => {
                    console.error('Lỗi khi sao chép: ', err);
                    fallbackCopyTextToClipboard(textToCopy);
                });
        } else {
            // Fallback cho trình duyệt không hỗ trợ Clipboard API
            fallbackCopyTextToClipboard(textToCopy);
        }
    });
    
    // Phương thức fallback sao chép vào clipboard
    function fallbackCopyTextToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            const successful = document.execCommand('copy');
            const msg = successful ? 'thành công' : 'thất bại';
            console.log('Sao chép fallback ' + msg);
            
            // Thay đổi nút tạm thời để thông báo đã sao chép
            const originalText = copyCode.textContent;
            copyCode.textContent = 'Đã sao chép!';
            setTimeout(() => {
                copyCode.textContent = originalText;
            }, 2000);
        } catch (err) {
            console.error('Fallback: Không thể sao chép', err);
        }
        
        document.body.removeChild(textArea);
    }
    
    // Xử lý nút tải lên tệp khác
    uploadAnother.addEventListener('click', function() {
        // Reset form
        uploadForm.reset();
        fileName.textContent = 'Chọn tệp để tải lên (tối đa 2MB)';
        
        // Ẩn kết quả và hiển thị form
        uploadResult.classList.add('hidden');
        uploadForm.classList.remove('hidden');
    });
    
    // Hàm định dạng kích thước tệp
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    // Hàm đếm ngược thời gian hết hạn
    function startExpirationCountdown(expirationDate) {
        function updateTimer() {
            const now = new Date();
            const diff = expirationDate - now;
            
            if (diff <= 0) {
                // Hết hạn
                expirationTimer.textContent = '00:00';
                return;
            }
            
            // Tính toán phút và giây
            const minutes = Math.floor(diff / 60000);
            const seconds = Math.floor((diff % 60000) / 1000);
            
            // Định dạng thời gian
            expirationTimer.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            
            // Cập nhật mỗi giây
            setTimeout(updateTimer, 1000);
        }
        
        // Bắt đầu đếm ngược
        updateTimer();
    }
});