# ⚡ Overlay Clicker — Android Auto-Click App

A fully featured screen overlay auto-clicker for Android. A draggable floating button sits
over all other apps. A red crosshair marks the exact point to tap. One press starts
auto-clicking that point at your chosen speed.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🟢 Draggable floating button | Drag anywhere on screen, never in your way |
| 🎯 Draggable target crosshair | Drag the red crosshair to the exact pixel to tap |
| 👆 Tap-to-set target | Tap anywhere on screen to place the target instantly |
| ⚡ Auto-click | Repeating taps at adjustable speed (100ms – 5s interval) |
| 📐 Button size | Adjust from 50px to 250px |
| 🌫 Transparency | 10% – 100% opacity for the floating button |
| 👆 Single-tap test | Fire one click without starting auto mode |
| 🔔 Persistent notification | Stop the service from the notification shade |

---

## 📋 Requirements

- Android **8.0 (API 26)** or higher
- Two special permissions (granted at first launch)

---

## 🚀 How to Build

### Prerequisites
- Android Studio **Hedgehog (2023.1)** or newer
- JDK 17+
- Android SDK 34

### Steps

1. **Open the project**
   ```
   File → Open → select the OverlayClicker folder
   ```

2. **Let Gradle sync** — it will download all dependencies automatically.

3. **Connect your device** (USB debugging on) or start an emulator.

4. **Run** — press ▶ or `Shift+F10`.

---

## 📱 First-Time Setup (on the device)

### Step 1 — Grant Overlay Permission
Tap **"Grant Overlay Permission"** → find **Overlay Clicker** → toggle **ON**.

### Step 2 — Enable Accessibility Service *(for real taps)*
Go to:
```
Settings → Accessibility → Installed Services → Overlay Clicker → Enable
```
This allows the app to inject actual touch events into other apps.

### Step 3 — Start the Overlay
Back in the app, press **▶ Start Overlay**.  
You can now minimize / home out — the overlay stays.

---

## 🕹 Using the Overlay

### Floating Green Button
| Gesture | Action |
|---|---|
| **Drag** | Move the button anywhere |
| **Single tap** (idle) | Open settings panel |
| **Single tap** (running) | Pause auto-click |

### Red Crosshair (🎯 target)
| Gesture | Action |
|---|---|
| **Drag** | Move the click target anywhere |

### Settings Panel (appears next to the button)
| Control | What it does |
|---|---|
| 📍 Tap Screen to Set Target | Dims the screen; tap once to place target |
| BUTTON SIZE −/+ | Shrink or grow the floating button |
| TRANSPARENCY −/+ | Make the button more or less see-through |
| CLICK SPEED −/+ | Faster (−) or slower (+) repeat interval |
| 👆 Click Once Now | Fire a single tap immediately |
| ▶ Start Auto-Click | Begin repeating taps at the set speed |

---

## 🏗 Project Structure

```
OverlayClicker/
├── app/src/main/
│   ├── java/com/overlayapp/
│   │   ├── MainActivity.kt            ← Permissions + Start/Stop UI
│   │   ├── OverlayService.kt          ← Core overlay logic (foreground service)
│   │   └── AccessibilityClickService.kt ← Touch injection via Accessibility API
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── overlay_floating_button.xml
│   │   │   ├── overlay_target_point.xml
│   │   │   └── overlay_control_panel.xml
│   │   ├── drawable/  (fab_bg_idle/active, ic_target, panel_bg, …)
│   │   └── xml/accessibility_service_config.xml
│   └── AndroidManifest.xml
└── build.gradle
```

---

## ⚠️ Notes

- **Without Accessibility Service** the crosshair still animates as feedback but taps
  aren't injected into other apps. Enable it for full functionality.
- The app does **not** run on Android 5/6 (API < 26) — overlay type changed in Oreo.
- Some banking / DRM apps detect overlay windows and may refuse to open while the
  service is running. Stop the service before using those apps.
