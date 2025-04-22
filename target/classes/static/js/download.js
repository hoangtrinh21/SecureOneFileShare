document.addEventListener('DOMContentLoaded', function() {
    const codeForm = document.getElementById('codeForm');
    const connectionCodeInput = document.getElementById('connectionCodeInput');
    const verifyButton = document.getElementById('verifyButton');
    const fileInfoSection = document.getElementById('fileInfoSection');
    const fileName = document.getElementById('fileName');
    const fileSize = document.getElementById('fileSize');
    const downloadButton = document.getElementById('downloadButton');
    const errorMessage = document.getElementById('errorMessage');
    const lockoutMessage = document.getElementById('lockoutMessage');
    const lockoutTimer = document.getElementById('lockoutTimer');
    
    // Check URL for connectionCode parameter
    const urlParams = new URLSearchParams(window.location.search);
    const codeParam = urlParams.get('code');
    if (codeParam) {
        connectionCodeInput.value = codeParam;
    }
    
    // Hide file info section initially
    fileInfoSection.classList.add('hidden');
    
    let downloadToken = null;
    let countdownInterval = null;
    
    // Handle form submission
    codeForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const code = connectionCodeInput.value.trim();
        if (!code) {
            errorMessage.textContent = "Please enter a connection code";
            errorMessage.classList.remove('hidden');
            return;
        }
        
        // Disable button and show loading
        verifyButton.disabled = true;
        verifyButton.innerHTML = '<span class="spinner"></span> Verifying...';
        errorMessage.classList.add('hidden');
        
        // Verify the connection code
        fetch('/api/download/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `connectionCode=${encodeURIComponent(code)}`
        })
        .then(response => {
            if (!response.ok) {
                if (response.status === 429) {
                    // Handle rate limiting (too many attempts)
                    return response.json().then(data => {
                        throw new Error(`too_many_requests:${data.lockedOutFor}`);
                    });
                }
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
            return response.json();
        })
        .then(data => {
            // Hide error message
            errorMessage.classList.add('hidden');
            
            // Show file info section
            fileInfoSection.classList.remove('hidden');
            
            // Display file information
            fileName.textContent = data.fileName;
            fileSize.textContent = formatFileSize(data.fileSize);
            
            // Store download token
            downloadToken = data.downloadToken;
            
            // Start download token expiration countdown (3 minutes)
            startExpirationCountdown();
            
            // Enable download button
            downloadButton.disabled = false;
        })
        .catch(error => {
            // Handle errors
            if (error.message.startsWith('too_many_requests:')) {
                // Handle lockout
                const lockoutSeconds = error.message.split(':')[1];
                showLockoutTimer(parseInt(lockoutSeconds));
            } else {
                // Show regular error message
                errorMessage.textContent = error.message || "Invalid connection code";
                errorMessage.classList.remove('hidden');
            }
            
            // Reset button
            verifyButton.disabled = false;
            verifyButton.innerHTML = 'Verify';
        });
    });
    
    // Handle download button
    downloadButton.addEventListener('click', function() {
        if (!downloadToken) {
            errorMessage.textContent = "Download token is missing or expired";
            errorMessage.classList.remove('hidden');
            return;
        }
        
        // Initiate download
        window.location.href = `/api/download?token=${encodeURIComponent(downloadToken)}`;
        
        // Disable download after clicking
        downloadButton.disabled = true;
        downloadButton.innerHTML = 'Downloaded';
        
        // Clear token after download initiated
        downloadToken = null;
        
        // Clear countdown
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
    });
    
    // Format file size function
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    // Start the expiration countdown for download token (3 minutes)
    function startExpirationCountdown() {
        let secondsLeft = 3 * 60; // 3 minutes
        
        // Clear any existing interval
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
        
        countdownInterval = setInterval(() => {
            secondsLeft--;
            
            if (secondsLeft <= 0) {
                clearInterval(countdownInterval);
                downloadToken = null;
                downloadButton.disabled = true;
                downloadButton.innerHTML = 'Link Expired';
                errorMessage.textContent = "Download link has expired";
                errorMessage.classList.remove('hidden');
            }
        }, 1000);
    }
    
    // Show lockout timer
    function showLockoutTimer(seconds) {
        lockoutMessage.classList.remove('hidden');
        errorMessage.classList.add('hidden');
        
        updateLockoutTimer(seconds);
        
        const lockoutInterval = setInterval(() => {
            seconds--;
            
            if (seconds <= 0) {
                clearInterval(lockoutInterval);
                lockoutMessage.classList.add('hidden');
                verifyButton.disabled = false;
                verifyButton.innerHTML = 'Verify';
            } else {
                updateLockoutTimer(seconds);
            }
        }, 1000);
    }
    
    // Update lockout timer display
    function updateLockoutTimer(seconds) {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        lockoutTimer.textContent = `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    }
    
    // If code parameter was in URL, automatically submit the form
    if (codeParam) {
        codeForm.dispatchEvent(new Event('submit'));
    }
});
