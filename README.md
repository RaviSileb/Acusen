# Acoustic Sentinel

Mobilní aplikace pro 24/7 akustický monitoring s machine learning detekcí zvuků.

## Donate
<img width="309" height="306" alt="image" src="https://github.com/user-attachments/assets/ebca3be0-74ff-4cc7-bc88-797b3ea6ee59" />


## Funkcionalita

### ✅ Implementováno
- 🎯 **Nahrávání zvukových vzorů** - Nahrání a pojmenování 5-10s audio vzorů
- 🔬 **Analýza zvuku** - Tlačítko pro zpracování zvuku do matematického vzorce (MFCC)
- 📊 **Zobrazení výsledků** - Spektrální centroid, dominantní frekvence, energie signálu
- 📈 **MFCC Grafy** - Vizualizace matematického vzorce (fingerprints) u každého vzoru
- 📱 **Moderní UI** - Material Design 3 s intuitivním rozhraním
- 📜 **Scrollovatelnost** - Všechny obrazovky podporují vertikální scrollování
- 🎛️ **Správa vzorů** - Seznam, aktivace/deaktivace, mazání naučených vzorů s grafy
- ⚙️ **Nastavení alertů** - Konfigurace e-mailového upozorňování
- 🔐 **Oprávnění** - Správa mikrofonu, lokace a notifikací
- 🏗️ **Architektura** - MVVM pattern s ViewModely a Compose UI
- 💾 **Data storage** - SharedPreferences s JSON serializací
- ℹ️ **Info panely** - Technické informace o zpracování audio signálu
- 🎯 **Akční panely** - Přímé zobrazení oprávnění, statistik a historie alarmů na úvodní stránce
- 🔧 **Funkční přepínače** - Aktivace/deaktivace vzorů v real-time
- 📍 **GPS lokace** - Získání a přiložení GPS souřadnic k alertům a historii detekčí
- 📋 **Historie alarmů** - Úplná historie detekčí s GPS souřadnicemi, přesností a časovými značkami
- 📈 **GPS statistiky** - Počet detekčí s GPS, přesnost lokací v historii
- ☑️ **Checkbox ovládání** - Checkbox pro zařazení/vyřazení vzorů ze seznamu aktivních detekovaných vzorů
- 📊 **Real-time statistiky** - Dynamické zobrazení počtu aktivních vzorů v detekci
- 🔊 **DSP komponenty** - ✅ DOKONČENO - Pokročilý MFCC processor, DTW matcher, FFT analyzer
- 📊 **Audio zpracování** - ✅ DOKONČENO - Real-time analýza, circular buffer, fingerprinting
- 🤖 **Pattern Recognition** - ✅ DOKONČENO - Pokročilý Sound Pattern Classifier
- 🎧 **Real-time Processor** - ✅ DOKONČENO - RealTimeAudioProcessor s AudioRecord

### 🚧 Připraveno k implementaci
- 🤖 **Machine Learning** - Adaptivní učení a pattern enhancement
- 📧 **Email systém** - ✅ Částečně implementováno (s GPS)
- 📍 **GPS lokace** - ✅ DOKONČENO
- 🔄 **Background služba** - ✅ Pokročilá implementace s DSP integrací
- 📤 **Export/Import** - Záloha a obnovení vzorů

## Technické specifikace

- **Platform**: Android 14+ (API 34+)
- **Jazyk**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architektura**: MVVM
- **Dependencies**: Material 3, Location Services, JavaMail

## Struktura projektu

```
app/src/main/java/com/example/acusen/
├── alert/                  # E-mail alerting systém
│   ├── AlertManager.kt     # ✅ GPS integrace
│   ├── EmailAlertService.kt
│   └── LocationService.kt  # ✅ GPS služba
├── audio/                  # Audio nahrávání a buffer
│   ├── AudioRecordingManager.kt
│   └── CircularAudioBuffer.kt
├── classifier/             # ML klasifikace zvuků
│   └── SoundPatternClassifier.kt
├── data/                   # Data modely
│   ├── SoundPattern.kt     # ✅ GPS souřadnice
│   └── AlarmDetection.kt   # ✅ Historie s GPS
├── dsp/                    # Digitální zpracování signálu
│   ├── MFCCProcessor.kt
│   ├── DTWMatcher.kt
│   └── FFTAnalyzer.kt
├── service/                # Background služby
│   └── AcousticMonitoringService.kt
├── storage/                # Data persistence
│   ├── PatternStorageManager.kt
│   └── AlarmHistoryStorageManager.kt # ✅ GPS historie
├── ui/components/          # UI komponenty
│   └── MFCCGraph.kt       # ✅ Graf komponenta
├── ui/screens/             # UI obrazovky
│   ├── MainScreen.kt
│   ├── MonitoringScreen.kt # ✅ GPS v historii
│   ├── PatternsListScreen.kt
│   ├── RecordingScreen.kt
│   └── SettingsScreen.kt   # ✅ GPS nastavení
├── viewmodel/              # ViewModely
│   ├── AlertViewModel.kt
│   ├── MonitoringViewModel.kt # ✅ GPS historie
│   └── SoundPatternViewModel.kt
└── MainActivity.kt
```

## Oprávnění

Aplikace vyžaduje následující oprávnění:
- `RECORD_AUDIO` - Nahrávání zvuku
- `ACCESS_FINE_LOCATION` - GPS lokace pro alerty  
- `ACCESS_COARSE_LOCATION` - Přibližná lokace
- `FOREGROUND_SERVICE` - Background monitoring
- `POST_NOTIFICATIONS` - Notifikace

## Instalace

1. Otevřete projekt v Android Studio
2. Synchronizujte Gradle dependencies
3. Spusťte na zařízení s Android 14+

## Použití

1. **Nahrání vzoru**: 
   - Přejděte do sekce "Nahrání"
   - Stiskněte tlačítko nahrávání
   - Nahrajte 5-10s zvuku
   - Stiskněte "ZPRACOVAT DO VZORCE" pro analýzu
   - **Zobrazí se MFCC graf** ukazující matematickou reprezentaci zvuku
   - Prohlédněte si technické parametry v info panelu
   - Pojmenujte a uložte vzor

2. **Monitoring**:
   - Přejděte do sekce "Monitoring" 
   - Stiskněte "SPUSTIT MONITORING"
   - Na úvodní stránce uvidíte přímo:
     - **Oprávnění aplikace** - status všech potřebných oprávnění s celkovým počtem
     - **Statistiky monitoringu** - aktivní vzory, počet detekí, doba běhu, úspěšnost, GPS pokrytí
     - **Historie detekovaných alarmů** - posledních 5 zachycených zvuků s časy, přesností a GPS souřadnicemi
   - Aplikace bude poslouchat na pozadí

3. **Nastavení alertů**:
   - V "Nastavení" povolte e-mailové upozornění
   - Vyplňte e-mail příjemce a odesílatele
   - Nastavte SMTP parametry
   - **Povolte "Zahrnout GPS lokaci"** pro přiložení souřadnic k alertům
   - Otestujte funkčnost

4. **Správa vzorů**:
   - V sekci "Vzory" můžete aktivovat/deaktivovat naučené vzory pomocí **checkboxu**
   - **☑️ Zaškrtnutý checkbox** = vzor je zařazen do seznamu aktivních detekovaných vzorů
   - **☐ Nezaškrtnutý checkbox** = vzor je vyřazen ze seznamu aktivních detekovaných vzorů
   - **Každý vzor zobrazuje MFCC graf** jeho zvukové sekvence
   - **Vizuální rozlišení** - aktivní vzory mají zvýrazněnou kartu s orámováním
   - **Real-time počítadlo** - v záhlaví je zobrazen počet aktivních vzorů
   - Smazat nepotřebné vzory
   - Prohlédnout si matematické vzorce (fingerprints) jednotlivých zvuků

## Další vývoj

Prioritní úkoly pro dokončení:
1. Implementace skutečného audio nahrávání (AudioRecord)
2. Dokončení DSP algoritmů (MFCC, DTW, FFT)
3. Aktivace background monitoring služby
4. Testování e-mail alerting systému
5. Optimalizace battery consumption
6. Přidání export/import funkcionalit

## Licence

Projekt vytvořen podle specifikace "MASTER SPECIFICATION: Acoustic Sentinel".

## Pokročilé DSP komponenty

### 🔊 MFCC Processor
- **Mel-frequency cepstral coefficients** pro převod zvuku na matematické otisky
- **Pre-emphasis filtr** pro zvýraznění vyšších frekvencí  
- **Hamming windowing** a **FFT zpracování**
- **Mel filter bank** a **DCT transformace**
- **13 MFCC koeficientů** pro každý zvukový vzor

### 📊 FFT Analyzer  
- **Cooley-Tukey FFT algoritmus** pro spektrální analýzu
- **Detekce dunění** v pásmu 20-100 Hz
- **Spektrální charakteristiky** - centroid, spread, dominantní frekvence
- **Transient analýza** pro detekci ostrých přechodů
- **Real-time zpracování** s optimalizovanými algoritmy

### 🎯 DTW Matcher
- **Dynamic Time Warping** pro porovnání sekvencí v různém tempu
- **Sakoe-Chiba band** omezení pro optimalizaci
- **Multi-metrické porovnání** - DTW, cosine similarity, correlation
- **Pokročilé confidence scoring** s kombinovanými algoritmy

### 🔄 Real-time Audio Processing
- **CircularAudioBuffer** - uchovává posledních 15 sekund audio dat
- **Noise gate** a **high-pass filtering**
- **Signal level monitoring** a **silence detection**
- **WAV export** funkcionalita
- **Thread-safe operace** s optimalizovaným locking

### 🤖 Sound Pattern Classifier
- **Machine learning přístup** ke klasifikaci zvuků
- **Multi-feature fusion** - MFCC + spektrální + časové charakteristiky
- **Automatické rozpoznání typu** - sirén, alarm, mechanické poruchy
- **Adaptivní learning** pro zlepšení přesnosti
- **Paralelní zpracování** pro real-time performance
