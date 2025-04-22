document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const uploadButton = document.getElementById('uploadButton');
    const fileNameDisplay = document.getElementById('fileName');
    const fileSizeDisplay = document.getElementById('fileSize');
    const uploadProgress = document.getElementById('uploadProgress');
    const uploadResult = document.getElementById('uploadResult');
    const connectionCodeDisplay = document.getElementById('connectionCode');
    const uploadSection = document.getElementById('uploadSection');
    const resultSection = document.getElementById('resultSection');
    const copyButton = document.getElementById('copyButton');
    const errorMessage = document.getElementById('errorMessage');

    // Hide result section initially
    resultSection.classList.add('hidden');
    
    // Handle file selection
    fileInput.addEventListener('change', function() {
        if (this.files.length > 0) {
            const file = this.files[0];
            const maxSize = 2 * 1024 * 1024; // 2MB in bytes
            
            // Display file information
            fileNameDisplay.textContent = file.name;
            fileSizeDisplay.textContent = formatFileSize(file.size);
            
            // Validate file size
            if (file.size > maxSize) {
                errorMessage.textContent = "File size exceeds the limit of 2MB";
                errorMessage.classList.remove('hidden');
                uploadButton.disabled = true;
            } else {
                errorMessage.classList.add('hidden');
                uploadButton.disabled = false;
            }
        } else {
            fileNameDisplay.textContent = 'No file selected';
            fileSizeDisplay.textContent = '';
            errorMessage.classList.add('hidden');
            uploadButton.disabled = true;
        }
    });

    // Handle form submission
    uploadForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (fileInput.files.length === 0) {
            errorMessage.textContent = "Please select a file first";
            errorMessage.classList.remove('hidden');
            return;
        }
        
        const file = fileInput.files[0];
        const maxSize = 2 * 1024 * 1024; // 2MB in bytes
        
        if (file.size > maxSize) {
            errorMessage.textContent = "File size exceeds the limit of 2MB";
            errorMessage.classList.remove('hidden');
            return;
        }
        
        // Create FormData
        const formData = new FormData();
        formData.append('file', file);
        
        // Disable the upload button and show progress
        uploadButton.disabled = true;
        uploadButton.innerHTML = '<span class="spinner"></span> Uploading...';
        uploadProgress.classList.remove('hidden');
        
        // Send the upload request
        fetch('/api/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
            return response.json();
        })
        .then(data => {
            // Hide upload section and show result section
            uploadSection.classList.add('hidden');
            resultSection.classList.remove('hidden');
            
            // Display connection code
            connectionCodeDisplay.textContent = data.connectionCode;
            
            // Enable copy button
            copyButton.disabled = false;
        })
        .catch(error => {
            // Show error message
            errorMessage.textContent = error.message || "An error occurred during upload";
            errorMessage.classList.remove('hidden');
            
            // Reset button
            uploadButton.disabled = false;
            uploadButton.innerHTML = 'Upload';
        })
        .finally(() => {
            // Hide progress
            uploadProgress.classList.add('hidden');
        });
    });
    
    // Handle copy button
    copyButton.addEventListener('click', function() {
        const codeText = connectionCodeDisplay.textContent;
        navigator.clipboard.writeText(codeText)
            .then(() => {
                this.textContent = 'Copied!';
                setTimeout(() => {
                    this.textContent = 'Copy Code';
                }, 2000);
            })
            .catch(err => {
                console.error('Failed to copy code: ', err);
            });
    });
    
    // Format file size function
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
});
