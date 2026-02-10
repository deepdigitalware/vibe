document.addEventListener('DOMContentLoaded', function() {
    // Initialize the app
    initializeApp();
});

function initializeApp() {
    console.log('Vibe App initialized');
    
    // Check if we're running in Capacitor
    if (window.Capacitor) {
        console.log('Running in Capacitor environment');
    } else {
        console.log('Running in web browser');
    }
    
    // Setup UI event listeners
    setupEventListeners();
}

function setupEventListeners() {
    // Login form submission
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleLogin();
        });
    }
    
    // Call button
    const callButton = document.getElementById('startCall');
    if (callButton) {
        callButton.addEventListener('click', startVideoCall);
    }
    
    // Wallet button
    const walletButton = document.getElementById('openWallet');
    if (walletButton) {
        walletButton.addEventListener('click', openWallet);
    }
}

function handleLogin() {
    const phoneInput = document.getElementById('phone');
    const phone = phoneInput.value;
    
    if (!phone) {
        showMessage('Please enter a phone number', 'error');
        return;
    }
    
    // Simulate login process
    showMessage('Sending OTP to ' + phone, 'info');
    
    setTimeout(() => {
        showMessage('OTP sent successfully! Please check your phone.', 'success');
        // Show OTP input field
        document.getElementById('otpSection').style.display = 'block';
    }, 1500);
}

function verifyOTP() {
    const otpInput = document.getElementById('otp');
    const otp = otpInput.value;
    
    if (!otp) {
        showMessage('Please enter the OTP', 'error');
        return;
    }
    
    // Simulate OTP verification
    showMessage('Verifying OTP...', 'info');
    
    setTimeout(() => {
        showMessage('Login successful!', 'success');
        // Navigate to home screen
        showScreen('home');
    }, 1500);
}

function startVideoCall() {
    showMessage('Starting video call...', 'info');
    
    setTimeout(() => {
        showMessage('Connected to call!', 'success');
        // This would integrate with WebRTC in a real implementation
    }, 1000);
}

function openWallet() {
    showMessage('Opening wallet...', 'info');
    
    setTimeout(() => {
        showMessage('Wallet balance: ₹100', 'success');
        // Show wallet details
        showScreen('wallet');
    }, 1000);
}

function showMessage(text, type) {
    const messageDiv = document.getElementById('message');
    messageDiv.textContent = text;
    messageDiv.className = 'message ' + type;
    messageDiv.style.display = 'block';
    
    // Hide message after 3 seconds
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 3000);
}

function showScreen(screenName) {
    // Hide all screens
    document.querySelectorAll('.screen').forEach(screen => {
        screen.style.display = 'none';
    });
    
    // Show requested screen
    document.getElementById(screenName + 'Screen').style.display = 'block';
}