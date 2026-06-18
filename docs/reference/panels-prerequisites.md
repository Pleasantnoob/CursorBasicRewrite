# Panels Prerequisites (summary)

Source: [panels.bylazar.com/docs](https://panels.bylazar.com/docs/com.bylazar.docs/Prerequisites/)

## Quickstarts

- [Panels Kotlin Quickstart](https://github.com/ftcontrol/Panels-Quickstart-Kotlin)
- [Panels Java Quickstart](https://github.com/ftcontrol/Panels-Quickstart-Java)

## Manual setup

1. Clone [FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController)
2. Add Maven repo to `build.gradle` / `settings.gradle`:

```groovy
maven { url = "https://mymaven.bylazar.com/releases" }
```

3. Add dependency to TeamCode:

```groovy
implementation "com.bylazar:fullpanels:1.0.12"
```

4. Ensure `compileSdk 34+`, `minSdk 24`

## Presets

- `com.bylazar:fullpanels` — all core plugins bundled
- Or `com.bylazar:panels` + individual plugins

Current fullpanels version: **1.0.12** (pluginsCoreVersion **1.1.44**)

## Before updating

Check each plugin changelog for breaking changes at [panels.bylazar.com](https://panels.bylazar.com).
