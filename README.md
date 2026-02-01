# Acoustic Sentinel

Mobile application for 24/7 acoustic monitoring with machine learning sound detection.


## Functionality

### ✅ Implemented
- 🎯 **Recording audio samples** - Record and name 5-10s audio samples
- 🔬 **Audio analysis** - Button for processing audio into a mathematical formula (MFCC)
- 📊 **Display results** - Spectral centroid, dominant frequency, signal energy
- 📈 **MFCC Graphs** - Visualization of mathematical formula (fingerprints) for each sample
- 📱 **Modern UI** - Material Design 3 with an intuitive interface
- 📜 **Scrollability** - All screens support vertical scrolling
- 🎛️ **Pattern management** - List, activation/deactivation, deletion of learned patterns with graphs
- ⚙️ **Alert settings** - Email notification configuration
- 🔐 **Permissions** - Microphone, location and notification management
- 🏗️ **Architecture** - MVVM pattern with ViewModels and Compose UI
- 💾 **Data storage** - SharedPreferences with JSON serialization
- ℹ️ **Info panels** - Technical information about audio signal processing
- 🎯 **Action panels** - Direct display of permissions, statistics and alarm history on the home page
- 🔧 **Function switches** - Real-time activation/deactivation of patterns
- 📍 **GPS location** - Obtaining and attaching GPS coordinates to alerts and detection history
- 📋 **Alarm history** - Complete detection history with GPS coordinates, accuracy and timestamps
- 📈 **GPS statistics** - Number of detections with GPS, accuracy of locations in history
- ☑️ **Checkbox control** - Checkbox for including/excluding patterns from the list of active detected patterns
- 📊 **Real-time statistics** - Dynamic display of the number of active patterns in detection
- 🔊 **DSP components** - ✅ COMPLETED - Advanced MFCC processor, DTW matcher, FFT analyzer
- 📊 **Audio processing** - ✅ COMPLETED - Real-time analysis, circular buffer, fingerprinting
- 🤖 **Pattern Recognition** - ✅ COMPLETED - Advanced Sound Pattern Classifier
- 🎧 **Real-time Processor** - ✅ COMPLETED - RealTimeAudioProcessor with AudioRecord

### 🚧 Ready to implement
- 🤖 **Machine Learning** - Adaptive learning and pattern enhancement
- 📧 **Email system** - ✅ Partially implemented (with GPS)
- 📍 **GPS location** - ✅ COMPLETED
- 🔄 **Background service** - ✅ Advanced implementation with DSP integration
- 📤 **Export/Import** - Backup and restore patterns

## Technical specifications

- **Platform**: Android 14+ (API 34+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Dependencies**: Material 3, Location Services, JavaMail

## Project structure

```
app/src/main/java/com/example/acusen/
├── alert/ # Email alerting system
│ ├── AlertManager.kt # ✅ GPS integration
│ ├── EmailAlertService.kt
│ └── LocationService.kt # ✅ GPS service
├── audio/ # Audio recording and buffering
│ ├── AudioRecordingManager.kt
│ └── CircularAudioBuffer.kt
├── classifier/ # ML sound classification
│ └── SoundPatternClassifier.kt
├── data/ # Data models
│ ├── SoundPattern.kt # ✅ GPS coordinates
│ └── AlarmDetection.kt # ✅ GPS history
├── dsp/ # Digital Signal Processing
│ ├── MFCCProcessor.kt
│ ├── DTWMatcher.kt
│ └── FFTAnalyzer.kt
├── service/ # Background services
│ └── AcousticMonitoringService.kt
├── storage/ # Data persistence
│ ├── PatternStorageManager.kt
│ └── AlarmHistoryStorageManager.kt # ✅ GPS history
├── ui/components/ # UI components
│ └── MFCCGraph.kt # ✅ Graph component
├── ui/screens/ # UI screens
│ ├── MainScreen.kt
│ ├── MonitoringScreen.kt # ✅ GPS history
│ ├── PatternsListScreen.kt
│ ├── RecordingScreen.kt
│ └── SettingsScreen.kt # ✅ GPS settings
├── viewmodel/ # ViewModels
│ ├── AlertViewModel.kt
│ ├── MonitoringViewModel.kt # ✅ GPS history
│ └── SoundPatternViewModel.kt
└── MainActivity.kt
```

## Permissions

The application requires the following permissions:
- `RECORD_AUDIO` - Record audio
- `ACCESS_FINE_LOCATION` - GPS location for alerts
- `ACCESS_COARSE_LOCATION` - Approximate location
- `FOREGROUND_SERVICE` - Background monitoring
- `POST_NOTIFICATIONS` - Notifications

## Installation

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Run on a device with Android 14+

## Usage

1. **Upload a pattern**:
- Go to the "Upload" section
- Press the record button
- Record 5-10s of audio
- Press "PROCESS INTO PATTERN" for analysis
- **The MFCC graph** will be displayed showing the mathematical representation of the sound
- View the technical parameters in the info panel
- Name and save the pattern

2. **Monitoring**:
- Go to the "Monitoring" section
- Press "START MONITORING"
- On the home page you will see directly:
- **Application permissions** - the status of all required permissions with the total number
