# Mobile Test Setup Guide (Android on macOS)

This guide takes you from zero Appium setup to running `mvn test -Pmobile` successfully.

---

## 1. Prerequisites

Make sure you have these installed before starting.

```bash
java -version    # Java 21+
mvn -version     # Maven 3.9+
node -v          # Node.js 20+ (needed for Appium)
brew --version   # Homebrew (needed for Android Studio)
```

If you don't have Node.js, install it via nvm:

```bash
brew install nvm
export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && . "/opt/homebrew/opt/nvm/nvm.sh"
nvm install --lts
```

---

## 2. Install Android Studio

```bash
brew install --cask android-studio
```

After installation, open Android Studio once to complete the initial setup wizard. This downloads the Android SDK, platform tools, and emulator binaries to `~/Library/Android/sdk`.

```bash
open -a "Android Studio"
```

Accept the license agreements and let the setup wizard finish. You can close Android Studio after this.

---

## 3. Set Environment Variables

Add these to your `~/.zshrc` (or `~/.bashrc` if you use bash):

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk"
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

Apply the changes:

```bash
source ~/.zshrc
```

Verify:

```bash
echo $ANDROID_HOME    # Should print: /Users/<you>/Library/Android/sdk
echo $JAVA_HOME       # Should print your Java path
adb --version         # Should print adb version
```

---

## 4. Install Android System Image and Create an Emulator

Accept the SDK licenses first:

```bash
yes | sdkmanager --licenses
```

Install a system image (API 34 / Android 14):

```bash
sdkmanager "system-images;android-34;google_apis;arm64-v8a"
```

Create an AVD (Android Virtual Device) named `Pixel_9`:

```bash
avdmanager create avd \
  --name "Pixel_9" \
  --package "system-images;android-34;google_apis;arm64-v8a" \
  --device "pixel_9"
```

When prompted "Do you wish to create a custom hardware profile?", type `no` and press Enter.

Verify the emulator was created:

```bash
emulator -list-avds
```

You should see `Pixel_9` in the list.

---

## 5. Download the Test App (APK)

The framework tests run against the WebDriverIO Native Demo App. Download the APK and place it in the expected directory.

```bash
mkdir -p src/test/resources/apps
```

Download the APK from the releases page:
https://github.com/webdriverio/native-demo-app/releases/tag/v2.2.0

Look for `android.wdio.native.app.v2.2.0.apk` (or the latest version) and save it to:

```
src/test/resources/apps/android.wdio.native.app.v2.2.0.apk
```

If the APK filename differs, update the path in `src/test/resources/config.properties`:

```properties
android.app.path=src/test/resources/apps/<your-apk-filename>.apk
```

---

## 6. Install Appium and Drivers

Install Appium globally:

```bash
npm install -g appium
```

Install the UiAutomator2 driver (required for Android):

```bash
appium driver install uiautomator2
```

Verify the installation:

```bash
appium --version              # Should print 2.x or 3.x
appium driver list --installed # Should show uiautomator2
```

---

## 7. Start the Emulator

In a terminal window, start the emulator:

```bash
emulator -avd Pixel_9
```

Wait for the emulator to fully boot (you should see the Android home screen). In a separate terminal, verify the device is connected:

```bash
adb devices
```

Expected output:

```
List of devices attached
emulator-5554   device
```

The `emulator-5554` matches the `android.device.name` in `config.properties`.

---

## 8. Start the Appium Server

In a separate terminal (keep the emulator running):

```bash
appium
```

Expected output:

```
[Appium] Welcome to Appium v3.x.x
[Appium] Appium REST http interface listener started on http://0.0.0.0:4723
```

Verify the server is running:

```bash
curl http://127.0.0.1:4723/status
```

You should get a JSON response with `"ready": true`.

---

## 9. Run the Mobile Tests

In a third terminal (emulator + Appium server still running):

```bash
mvn test -Pmobile
```

This runs all 5 mobile test classes defined in `mobile-tests.xml`:
- LoginMobileTest
- SwipeGestureTest
- FormInteractionTest
- NavigationTest
- WebViewTest

---

## 10. Verification Checklist

Before running tests, confirm all of these:

| Check | Command | Expected |
|-------|---------|----------|
| APK exists | `ls src/test/resources/apps/*.apk` | Shows the APK file |
| Emulator running | `adb devices` | Shows `emulator-5554 device` |
| Appium server up | `curl http://127.0.0.1:4723/status` | JSON with `"ready": true` |
| ANDROID_HOME set | `echo $ANDROID_HOME` | `/Users/<you>/Library/Android/sdk` |
| JAVA_HOME set | `echo $JAVA_HOME` | Your Java 21 path |

---

## Troubleshooting

**"ANDROID_HOME not set" error during test run**

The terminal running Maven must have `ANDROID_HOME` exported. Run `source ~/.zshrc` in that terminal, or restart it. If you started Appium before setting the variable, restart Appium too.

**"Could not start a new session" / emulator not found**

Make sure `adb devices` shows your emulator as `device` (not `offline` or `unauthorized`). If the emulator just booted, wait for the home screen to appear before running tests.

**"App not found" error**

The `android.app.path` in `config.properties` is relative to the project root. Verify the APK exists at that exact path. If your APK has a different filename, update the property.

**Emulator won't start / "PANIC: Missing emulator engine program"**

Make sure `$ANDROID_HOME/emulator` is in your PATH (before `$ANDROID_HOME/tools` if that exists). Verify with `which emulator`.

**Tests pass locally but timeout on some screens**

Emulator performance varies. If tests fail due to slow screen transitions, increase the explicit wait timeout in `config.properties`:

```properties
explicit.wait=20
```
