# Acoustic Sentinel

**🇺🇸 English** | [🇨🇿 Čeština](README-cs.md)

*Version 1.0.0 - Updated January 31, 2026*

Mobile application for 24/7 acoustic monitoring with machine learning sound detection.

## Features

### ✅ Implemented
- 🎯 **Audio Pattern Recording** - Record and name 5-10s audio patterns
- 🔬 **Sound Analysis** - Button to process sound into mathematical pattern (MFCC)
- 📊 **Results Display** - Spectral centroid, dominant frequency, signal energy
- 📈 **MFCC Graphs** - Visualization of mathematical patterns (fingerprints) for each pattern
- 📱 **Modern UI** - Material Design 3 with intuitive interface
- 📜 **Scrollability** - All screens support vertical scrolling
- 🎛️ **Pattern Management** - List, activation/deactivation, deletion of learned patterns with graphs
- ⚙️ **Alert Settings** - Email notification configuration
- 🔐 **Permissions** - Microphone, location and notification management
- 🏗️ **Architecture** - MVVM pattern with ViewModels and Compose UI
- 💾 **Data Storage** - SharedPreferences with JSON serialization
- ℹ️ **Info Panels** - Technical information about audio signal processing
- 🎯 **Action Panels** - Direct display of permissions, statistics and alarm history on main screen
- 🔧 **Functional Switches** - Real-time pattern activation/deactivation
- 📍 **GPS Location** - Obtain and attach GPS coordinates to alerts and detection history
- 📋 **Alarm History** - Complete detection history with GPS coordinates, accuracy and timestamps
- 📈 **GPS Statistics** - Number of detections with GPS, location accuracy in history
- ☑️ **Checkbox Control** - Checkbox to include/exclude patterns from active detected patterns list
- 📊 **Real-time Statistics** - Dynamic display of active pattern count in detection
- 🔊 **DSP Components** - ✅ COMPLETED - Advanced MFCC processor, DTW matcher, FFT analyzer
- 📊 **Audio Processing** - ✅ COMPLETED - Real-time analysis, circular buffer, fingerprinting
- 🤖 **Pattern Recognition** - ✅ COMPLETED - Advanced Sound Pattern Classifier
- 🎧 **Real-time Processor** - ✅ COMPLETED - RealTimeAudioProcessor with AudioRecord
- 🚨 **Red Alert Screen** - ✅ COMPLETED - AlertActivity with blinking red screen on detection
- 📧 **Email System** - ✅ COMPLETED - Automatic email sending with GPS and audio attachment
- 🎯 **Detection Test** - ✅ COMPLETED - Button for detection simulation with history records
- 📤 **Export/Import** - ✅ READY - Complete implementation of pattern backup and restore

### 🚧 Ready for Activation
- 📤 **Pattern Export/Import** - ✅ Complete implementation finished, ready for activation

## Technical Specifications

- **Platform**: Android 14+ (API 34+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Dependencies**: Material 3, Location Services, JavaMail

## Project Structure

```
app/src/main/java/com/example/acusen/
├── alert/                  # Email alerting system
│   ├── AlertManager.kt     # ✅ GPS integration
│   ├── EmailAlertService.kt
│   └── LocationService.kt  # ✅ GPS service
├── audio/                  # Audio recording and buffer
│   ├── AudioRecordingManager.kt
│   └── CircularAudioBuffer.kt
├── classifier/             # ML sound classification
│   └── SoundPatternClassifier.kt
├── data/                   # Data models
│   ├── SoundPattern.kt     # ✅ GPS coordinates
│   └── AlarmDetection.kt   # ✅ History with GPS
├── dsp/                    # Digital signal processing
│   ├── MFCCProcessor.kt
│   ├── DTWMatcher.kt
│   └── FFTAnalyzer.kt
├── service/                # Background services
│   └── AcousticMonitoringService.kt
├── storage/                # Data persistence
│   ├── PatternStorageManager.kt
│   └── AlarmHistoryStorageManager.kt # ✅ GPS history
├── ui/components/          # UI components
│   └── MFCCGraph.kt       # ✅ Graph component
├── ui/screens/             # UI screens
│   ├── MainScreen.kt
│   ├── MonitoringScreen.kt # ✅ GPS in history
│   ├── PatternsListScreen.kt
│   ├── RecordingScreen.kt
│   └── SettingsScreen.kt   # ✅ GPS settings
├── viewmodel/              # ViewModels
│   ├── AlertViewModel.kt
│   ├── MonitoringViewModel.kt # ✅ GPS history
│   └── SoundPatternViewModel.kt
└── MainActivity.kt
```

## Permissions

The application requires the following permissions:
- `RECORD_AUDIO` - Audio recording
- `ACCESS_FINE_LOCATION` - GPS location for alerts  
- `ACCESS_COARSE_LOCATION` - Approximate location
- `FOREGROUND_SERVICE` - Background monitoring
- `POST_NOTIFICATIONS` - Notifications

## Installation

1. Open project in Android Studio
2. Sync Gradle dependencies
3. Run on device with Android 14+

## Usage

1. **Record Pattern**: 
   - Go to "Recording" section
   - Press recording button
   - Record 5-10s of sound
   - Press "PROCESS TO PATTERN" for analysis
   - **MFCC graph will be displayed** showing mathematical representation of sound
   - Review technical parameters in info panel
   - Name and save pattern

2. **Monitoring**:
   - Go to "Monitoring" section 
   - Press "START MONITORING"
   - On main screen you'll see directly:
     - **Application Permissions** - status of all required permissions with total count
     - **Monitoring Statistics** - active patterns, detection count, runtime, success rate, GPS coverage
     - **Detected Alarms History** - last 5 captured sounds with times, accuracy and GPS coordinates
   - Application will listen in background

3. **Alert Settings**:
   - In "Settings" enable email notifications
   - Fill in recipient and sender email
   - Set SMTP parameters
   - **Enable "Include GPS location"** to attach coordinates to alerts
   - Test functionality

4. **Pattern Management**:
   - In "Patterns" section you can activate/deactivate learned patterns using **checkbox**
   - **☑️ Checked checkbox** = pattern is included in active detected patterns list
   - **☐ Unchecked checkbox** = pattern is excluded from active detected patterns list
   - **Each pattern displays MFCC graph** of its sound sequence
   - **Visual distinction** - active patterns have highlighted card with border
   - **Real-time counter** - active pattern count is displayed in header
   - Delete unnecessary patterns
   - View mathematical patterns (fingerprints) of individual sounds

## Future Development

Priority tasks for next versions:
1. ✅ **Export/Import functionality** - COMPLETELY READY
   - PatternExportImportService - ZIP export with audio files and metadata
   - ExportImportViewModel - full functionality with UI states
   - Complex UI with import preview and duplicate management
   - Secure format compatibility checking
2. **Export/Import activation** - ready to enable in code
3. Battery consumption optimization
4. Extended machine learning algorithms
5. Cloud pattern synchronization

## License

Project created according to "MASTER SPECIFICATION: Acoustic Sentinel" specification.

## Advanced DSP Components

### 🔊 MFCC Processor
- **Mel-frequency cepstral coefficients** for converting sound to mathematical fingerprints
- **Pre-emphasis filter** for high-frequency enhancement  
- **Hamming windowing** and **FFT processing**
- **Mel filter bank** and **DCT transformation**
- **13 MFCC coefficients** for each sound pattern

### 📊 FFT Analyzer  
- **Cooley-Tukey FFT algorithm** for spectral analysis
- **Rumble detection** in 20-100 Hz band
- **Spectral characteristics** - centroid, spread, dominant frequency
- **Transient analysis** for sharp transition detection
- **Real-time processing** with optimized algorithms

### 🎯 DTW Matcher
- **Dynamic Time Warping** for sequence comparison at different tempos
- **Sakoe-Chiba band** constraints for optimization
- **Multi-metric comparison** - DTW, cosine similarity, correlation
- **Advanced confidence scoring** with combined algorithms

### 🔄 Real-time Audio Processing
- **CircularAudioBuffer** - maintains last 15 seconds of audio data
- **Noise gate** and **high-pass filtering**
- **Signal level monitoring** and **silence detection**
- **WAV export** functionality
- **Thread-safe operations** with optimized locking

### 🤖 Sound Pattern Classifier
- **Machine learning approach** to sound classification
- **Multi-feature fusion** - MFCC + spectral + temporal characteristics
- **Automatic type recognition** - sirens, alarms, mechanical failures
- **Adaptive learning** for accuracy improvement
- **Parallel processing** for real-time performance
