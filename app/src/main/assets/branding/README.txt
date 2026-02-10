Branding assets
----------------

- Place your PNG logo at: branding_logo.png
  Recommended size: 512x512 px (square), transparent background.

- Optional: provide a Lottie animation at splash_anim.json
  You can get sample JSONs from https://lottiefiles.com/

- Server base URL is configured via server_base_url.txt
  Default for Android emulator: http://10.0.2.2:4000
  For real device, put your machine IP, e.g., http://192.168.1.10:4000

The app first tries remote config at {server}/config/app and logo at
{server}/uploads/splash-logo.png. If unavailable, it falls back to
branding_logo.png in this folder, otherwise uses @drawable/ic_splash_logo.