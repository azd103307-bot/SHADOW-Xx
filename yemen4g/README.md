# Yemen 4G

Android helper app for Huawei Y6 (ATU-L31) users configuring Yemen Mobile data.

## Features
- Opens Android wireless/network settings.
- Opens APN settings where the device exposes that settings screen.
- Copies the APN text `ymdata`.
- Provides Arabic setup guidance.

## Important limitations
This app does not unlock LTE bands, provision a SIM, change carrier-side settings, or force LTE mode. Network availability depends on the exact handset variant, carrier provisioning, SIM configuration, local coverage, and supported LTE bands. Confirm the correct APN and 4G provisioning with Yemen Mobile before changing carrier settings.

## Build
The GitHub Actions workflow at `.github/workflows/build-yemen4g.yml` builds a debug APK and uploads it as the `Yemen-4G-debug` artifact.
