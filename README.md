# Polar H10 Realtime ECG Recorder & Clinical Holter Analysis Android App

A production-grade native Android application (`com.polar.recorder`) built in Kotlin for real-time 130Hz raw ECG streaming, RR interval tracking, 3-axis accelerometer motion artifact scoring, background session recording, Room database storage, native PDF report generation, and interactive clinical ECG inspection matching all reference specifications.

---

## Key Features

1. **Polar Device Source Abstraction (`PolarDeviceSource`)**:
   - `PolarBleDeviceSource`: Connects to physical Polar H10 sensors over GATT (130Hz raw ECG PMD service `fb005c80...`, 1024Hz RR intervals, HR service `0x180D`).
   - `PolarSimulatedDeviceSource`: Built-in simulator feeding authentic synthetic 130Hz P-QRS-T ECG waveforms, R-peak variations, and movement vectors through the exact same data pipeline so the app is fully functional without physical hardware.

2. **Background Foreground Recording Service (`PolarRecordingService`)**:
   - Runs a persistent notification foreground service to record multi-hour Holter ECG sessions without interruption when the phone screen is off or app is minimized.

3. **Room Database & Raw Sample Storage**:
   - Stores session records (`SessionEntity`), calculated metrics (`meanHR`, `RMSSD`, `SDNN`, `pNN50`, `pNN200`, `meanQRS`, `meanQTc`), and flagged clinical events (`EventEntity`) in SQLite Room database alongside PolarRecorder-compatible `.txt` raw data files.

4. **Clinical ECG Screens (Screenshots 1-4 Parity)**:
   - **Screen 1**: Red header, `WITHIN LIMITS` banner, reference table (`meanHR`, `pNN50`, `pNN200`, `meanQRS`, `meanQTc`, `meanTPositive`, `meanJPoint`, `mean2-8Hz`), signal sample plot, sample event navigator.
   - **Screen 2**: `HRV — Heart rate variability` card, `Respiratory rate:` graph card ("Breathes per minute"), bottom navigation bar.
   - **Screen 3**: `Physical activity` steps (6833), distance (5169 m), speed (3.89 km/h), dual-axis Velocity vs. Cadence line chart, `Heart zones` extrasystole distribution.
   - **Screen 4**: Header `VEB - Couplet (12/19)`, custom QRS waveform canvas displaying red highlighted ectopic couplet beats and green R-peak dots for normal beats, filter badges, amplitude buttons `x`, `^`, `v`, and temporal navigation buttons (`<<`, `<`, `>`, `>>`, zoom).

5. **Native PDF Report Exporter (`PdfReportExporter`)**:
   - Generates multi-page PDF Holter clinical reports containing patient/session metadata, summary parameters, and ECG waveform strips using `android.graphics.pdf.PdfDocument`.

6. **Offline H10 Reference Guide (`H10InfoActivity`)**:
   - Bundled offline HTML document (`app/src/main/assets/h10_info.html`) rendered in an offline native WebView detailing H10 specifications, electrode placement tips, glossary, and clinical decision-support disclaimers.

---

## How to Build & Install

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (minSdk 26)

### Building via Command Line
```bash
./gradlew assembleDebug
```
The compiled APK will be created at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Automated GitHub Actions CI/CD Pipeline

Every commit to `main` triggers `.github/workflows/build-apk.yml`:
- Compiles the project using JDK 17 and Gradle 8.4.
- Automatically builds the Debug APK artifact.
- Publishes a public **GitHub Release** with the compiled `app-debug.apk` attached directly for one-click downloading!
