const express = require('express');
const path = require('path');
const fs = require('fs');
const os = require('os');
const app = express();
const PORT = 8000;

// Get local IP address
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return 'localhost';
}

const localIP = getLocalIP();

// Serve static files
app.use(express.static('.'));

// Serve the APK file
app.get('/app-debug.apk', (req, res) => {
  const apkPath = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
  res.download(apkPath);
});

// Serve management console
app.get('/manage', (req, res) => {
  const manageHtml = fs.readFileSync(path.join(__dirname, 'manage-app.html'), 'utf8');
  res.send(manageHtml);
});

// Serve a simple HTML page with QR code
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
        <title>Vibe App Installer</title>
        <style>
            body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
            .container { max-width: 600px; margin: 0 auto; }
            .qr-code { margin: 30px 0; }
            .download-link { 
                display: inline-block; 
                padding: 15px 30px; 
                background-color: #4CAF50; 
                color: white; 
                text-decoration: none; 
                border-radius: 5px; 
                font-size: 18px;
                margin: 10px;
            }
            .download-link:hover { background-color: #45a049; }
            .ip-address { 
                background-color: #f0f0f0; 
                padding: 10px; 
                border-radius: 5px; 
                font-family: monospace;
                margin: 20px 0;
            }
            .nav-link {
                display: inline-block;
                padding: 10px 20px;
                background-color: #2196F3;
                color: white;
                text-decoration: none;
                border-radius: 5px;
                margin: 10px;
            }
            .nav-link:hover {
                background-color: #1976D2;
            }
            .qr-container {
                display: flex;
                justify-content: center;
                margin: 20px 0;
            }
            .qr-code img {
                max-width: 200px;
                height: auto;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>Vibe App Distribution Server</h1>
            <p>Your local IP address is: <span class="ip-address">${localIP}:${PORT}</span></p>
            
            <div>
                <a href="/manage" class="nav-link">Open Management Console</a>
                <a href="http://localhost:8001/" class="nav-link">Open Admin Panel</a>
            </div>
            
            <h2>Install Vibe App</h2>
            <p>Scan this QR code with your Android device to download and install the Vibe app:</p>
            <div class="qr-container">
                <div class="qr-code">
                    <img src="https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=http://${localIP}:${PORT}/app-debug.apk" alt="QR Code">
                </div>
            </div>
            <p>Or click the button below:</p>
            <a href="/app-debug.apk" class="download-link">Download APK</a>
            <h2>Installation Instructions</h2>
            <ol style="text-align: left;">
                <li>On your Android device, open the browser and go to: <strong>http://${localIP}:${PORT}</strong></li>
                <li>Tap the "Download APK" button or scan the QR code</li>
                <li>If prompted, allow downloads from this browser</li>
                <li>Once downloaded, open the APK file from your notifications or file manager</li>
                <li>If prompted about security, enable "Install from unknown sources" for your browser</li>
                <li>Follow the installation prompts</li>
            </ol>
            <p><small>Note: You may need to enable "Install from unknown sources" on your Android device.</small></p>
        </div>
    </body>
    </html>
  `);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`APK server running on http://0.0.0.0:${PORT}`);
  console.log(`Direct APK download: http://${localIP}:${PORT}/app-debug.apk`);
  console.log(`Management Console: http://${localIP}:${PORT}/manage`);
  console.log(`Local IP Address: ${localIP}`);
});