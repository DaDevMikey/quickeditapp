# QuickEdit

QuickEdit is a specialized Android application designed for Samsung devices to provide advanced visual and functional tweaks to the Quick Panel. It leverages **Shizuku** to interact with system settings securely without requiring root access.

## Features

- **Edit More:** Unlock additional editing options within your Quick Panel.
- **Landscape Edit:** Enable the ability to edit the Quick Panel while in landscape mode.
- **Volume & Brightness %:** Display precise percentage indicators for volume and brightness sliders.
- **Instant Application:** All tweaks take effect immediately without the need for a device reboot.
- **One UI Inspired Design:** A clean, modern interface that matches the Samsung ecosystem aesthetics.

## Prerequisites

To use QuickEdit, you must have:
1. A Samsung device running Android 12 (API 31) or higher.
2. The [Shizuku](https://shizuku.rikka.app/) app installed and running on your device.

## Installation & Setup

1. **Download and Install:** Sideload the APK onto your device.
2. **Authorize Shizuku:** Open QuickEdit and follow the onboarding steps to grant Shizuku permission.
3. **Grant Secure Settings:** Use the built-in setup button to automatically grant the `WRITE_SECURE_SETTINGS` permission via Shizuku.

## Development

### Built With
- **Jetpack Compose:** For a modern, reactive UI.
- **Material 3:** Following the latest Android design standards.
- **Shizuku API:** For secure system-level interactions.

### Building from Source
To build the project, run:
```bash
./gradlew :app:assembleDebug
```

## Troubleshooting
- **Crashes on Launch:** Ensure Shizuku is running before opening the app.
- **Tweaks Not Applying:** Double-check that the "Secure Settings" permission was successfully granted during setup.

## License
[Insert License Here]
