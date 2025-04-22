document.addEventListener('DOMContentLoaded', function() {
    const downloadForm = document.getElementById('download-form');
    const downloadFormContainer = document.getElementById('download-form-container');
    const lockoutContainer = document.getElementById('lockout-container');
    const lockoutTimer = document.getElementById('lockout-timer');
    const fileInfoContainer = document.getElementById('file-info-container');
    const fileName = document.getElementById('file-name');
    const fileSize = document.getElementById('file-size');
    const expirationTimer = document.getElementById('expiration-timer');
    const downloadButton = document.getElementById('download-button');
    const downloadError = document.getElementById('download-error');
    const errorMessage = document.getElementById('error-message');
    const tryAgain = document.getElementById('try-again');
    
    let downloadToken = null;
    let connectionCode = null;
    
    // Xử lý form nhập mã kết nối
    downloadForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(downloadForm);
        connectionCode = formData.get('connectionCode');
        
        if (!connectionCode) {
            showError('Vui lòng nhập mã kết nối.');
            return;
        }
        
        // Gửi yêu cầu kiểm tra mã kết nối
        fetch('/api/download', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                connectionCode: connectionCode
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                // Xử lý lỗi
                if (data.lockoutRemaining) {
                    // Hiển thị thời gian khóa
                    showLockoutTimer(data.lockoutRemaining);
                } else {
                    showError(data.error);
                }
                return;
            }
            
            // Hiển thị thông tin tệp
            fileName.textContent = data.filename;
            fileSize.textContent = formatFileSize(data.size);
            downloadToken = data.downloadToken;
            
            // Ẩn form và hiển thị thông tin tệp
            downloadFormContainer.classList.add('hidden');
            fileInfoContainer.classList.remove('hidden');
            
            // Bắt đầu đếm ngược thời gian hết hạn tải xuống
            startExpirationCountdown(new Date(data.downloadExpiresAt));
        })
        .catch(error => {
            console.error('Lỗi:', error);
            showError('Có lỗi xảy ra khi kết nối đến máy chủ. Vui lòng thử lại sau.');
        });
    });
    
    // Xử lý nút tải xuống
    downloadButton.addEventListener('click', function() {
        if (!downloadToken || !connectionCode) {
            showError('Không có thông tin tải xuống hợp lệ.');
            return;
        }
        
        // Chuyển hướng đến đường dẫn tải xuống
        window.location.href = `/api/download/${downloadToken}/${connectionCode}`;
    });
    
    // Xử lý nút thử lại
    tryAgain.addEventListener('click', function() {
        // Đặt lại trạng thái
        downloadForm.reset();
        
        // Ẩn thông báo lỗi và hiển thị form
        downloadError.classList.add('hidden');
        downloadFormContainer.classList.remove('hidden');
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
                showError('Liên kết tải xuống đã hết hạn. Vui lòng yêu cầu một mã kết nối mới.');
                fileInfoContainer.classList.add('hidden');
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
    
    // Hàm hiển thị thời gian khóa
    function showLockoutTimer(seconds) {
        downloadFormContainer.classList.add('hidden');
        lockoutContainer.classList.remove('hidden');
        
        updateLockoutTimer(seconds);
    }
    
    // Hàm cập nhật thời gian khóa
    function updateLockoutTimer(seconds) {
        if (seconds <= 0) {
            // Hết thời gian khóa
            lockoutContainer.classList.add('hidden');
            downloadFormContainer.classList.remove('hidden');
            return;
        }
        
        // Tính toán phút và giây
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        
        // Định dạng thời gian
        lockoutTimer.textContent = `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
        
        // Cập nhật mỗi giây
        setTimeout(() => {
            updateLockoutTimer(seconds - 1);
        }, 1000);
    }
    
    // Hàm hiển thị lỗi
    function showError(message) {
        errorMessage.textContent = message;
        
        downloadFormContainer.classList.add('hidden');
        fileInfoContainer.classList.add('hidden');
        lockoutContainer.classList.add('hidden');
        downloadError.classList.remove('hidden');
    }
});