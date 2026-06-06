# Build and Deploy — FTC Robot (CursorBasicRewrite)

Canonical guide for humans and AI agents building/uploading this project.

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| **JDK 8** | FTC SDK targets Java 8. JDK 21 will fail. Run `.\scripts\setup-jdk8.ps1` from repo root. |
| **Android SDK** | Install via [Android Studio Ladybug (2024.2+)](https://developer.android.com/studio). |
| **local.properties** | Auto-created by Android Studio. Contains `sdk.dir=...` — **never commit**. |
| **Android Studio** | Required for Pedro Pathing (not OnBot Java). |

References: [FTC Android Studio tutorial](https://ftc-docs.firstinspires.org/programming_resources/android_studio_java/Android-Studio-Tutorial.html), [REV wireless ADB](https://docs.revrobotics.com/duo-control/managing-the-control-system/android-studio-using-wireless-adb).

## Build commands (from repo root)

```powershell
# Compile only (fast check)
.\gradlew :TeamCode:compileDebugJavaWithJavac

# Full debug APK
.\gradlew :TeamCode:assembleDebug
```

**APK output:** `TeamCode\build\outputs\apk\debug\TeamCode-debug.apk`

## Deploy to Robot Controller

### Option A — USB (phone or Control Hub via USB)

1. Enable **USB debugging** on the Robot Controller device.
2. Connect USB; verify: `adb devices` (install platform-tools if missing).
3. Run:

```powershell
.\deploy-to-robot.ps1
```

Or manually: `adb install -r TeamCode\build\outputs\apk\debug\TeamCode-debug.apk`

### Option B — Wireless (Control Hub)

1. Power Control Hub; connect PC to robot WiFi (or same network).
2. Use REV Hardware Client to enable wireless debugging, or:
   ```powershell
   adb connect <CONTROL_HUB_IP>:5555
   adb devices
   ```
3. Deploy from Android Studio (select device in dropdown → Run) or `adb install -r ...`

### Option C — Android Studio Run button

1. Open project root in Android Studio (import Gradle project).
2. Select module **TeamCode**.
3. Select Robot Controller in device dropdown.
4. Green **Run** builds and installs.

## Driver Station setup

1. On Robot Controller phone: configure hardware in **Configure Robot** — names must match `DeviceNames.java` / `AGENTS.md`.
2. On Driver Station: select TeleOp or Autonomous mode; pick OpMode from list.
3. OpModes are discovered via `@TeleOp` / `@Autonomous` annotations in TeamCode.

## FTC Dashboard

- Connect phone/Hub and PC to same network.
- Browse to `http://192.168.43.1:8080/dash` (default) or IP shown in logcat.
- `@Config` classes appear while matching OpMode is running.

## Common failures

| Error | Fix |
|-------|-----|
| `invalid Java home` / major version 65 | Use JDK 8; run `setup-jdk8.ps1` |
| `SDK location not found` | Create `local.properties` with `sdk.dir` |
| `device unauthorized` | Accept USB debugging prompt on RC phone |
| OpMode not in list | Check `@TeleOp` name; rebuild and reinstall APK |
| Webcam init fails | Config name must be `Webcam 1` (match Robot Configuration) |
| Pedro paths don't run | Pedro requires Android Studio build, not OnBot Java |

## Related docs

- **Code map (start here):** [`docs/CODE_GUIDE.md`](CODE_GUIDE.md) — where everything lives and how subsystems connect.
- **Field coordinates:** [`docs/FIELD_COORDINATES.md`](FIELD_COORDINATES.md) — official FTC frame (+X, +Y, heading). Use before adding waypoints.
- **AI package map:** [`TeamCode/AGENTS.md`](../TeamCode/AGENTS.md)

## AI agent checklist

Before claiming "build succeeded":

1. Run `.\gradlew :TeamCode:compileDebugJavaWithJavac` and confirm exit code 0.
2. Do not modify `gradle.properties` JDK path unless user asks.
3. Do not commit `local.properties` or APK files.
4. After subsystem changes, verify production OpModes still compile.
5. New poses/waypoints must use the official FTC frame — see `FIELD_COORDINATES.md`.
