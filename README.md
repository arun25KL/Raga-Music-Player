# 🎵 Raga Music Player
<img width="350" height="800" alt="1" src="https://github.com/user-attachments/assets/070bca0b-1ce6-462b-bc1e-efb87325e837" />

<img width="350" height="800" alt="2" src="https://github.com/user-attachments/assets/a1005394-8ec7-4450-bd01-f92ea1845790" />



> A modern, high-performance, offline-first local audio player built with **Kotlin** and **Jetpack Compose (Material Design 3)**. Engineered for pure sound quality, comprehensive format support, smart playback continuity, and zero telemetry.

---

## 🌟 Highlights

- **Offline-First & Privacy-Focused**: No ads, no tracking, no analytics. Leaves zero junk files or orphan folders upon uninstallation.
- **Audiophile Format Support**: Plays **MP3, M4A, FLAC, ALAC, WAV, AIFF, AAC, OGG, OPUS, WMA, and MIDI**.
- **Smart Session Memory**: Tracks what you've played with the 60% rule, retaining memory across app closures and shutdowns until all tracks have finished playing.
- **Dynamic Artwork Palette & OLED Black**: Real-time palette extraction adapts interface colors to your music cover art, paired with a true **#000000 OLED Pure Black** mode.
- **5-Band Graphic Equalizer & Super Boost**: Hardware-accelerated equalizer, Bass Boost, 3D Virtualizer, and a high-gain volume amplifier up to 200%.
- **Multi-Source Album Art Search**: Automatically extracts embedded metadata and provides online art discovery across JioSaavn, Gaana, YouTube, iTunes, and Deezer.
- **Proper Shutdown Control**: Dedicated shutdown button to completely release audio hardware, free background services, and exit cleanly.

---

## 📑 Table of Contents

1. [Audio Playback Engine](#-audio-playback-engine)
2. [Smart Session Played Tracker](#-smart-session-played-tracker)
3. [Equalizer & Audio Effects](#-equalizer--audio-effects)
4. [Folder Management & Multi-Directory Indexing](#-folder-management--multi-directory-indexing)
5. [Sorting & Queue Management](#-sorting--queue-management)
6. [Visual Styling & Themes](#-visual-styling--themes)
7. [Online Album Artwork Search](#-online-album-artwork-search)
8. [System & Hardware Integration](#-system--hardware-integration)
9. [Settings & Customization](#-settings--customization)
10. [Clean Lifecycle & Uninstall Guarantee](#-clean-lifecycle--uninstall-guarantee)
11. [Tech Stack & Architecture](#-tech-stack--architecture)
12. [Author & Support](#-author--support)

---

## 🎧 Audio Playback Engine

### Supported Formats & Individual Format Toggles
Raga supports virtually all common and lossless audio formats. Every format has an independent toggle in the settings dialog so you can filter your library to exactly what you want:
- **MP3** (`.mp3`)
- **M4A / ALAC** (`.m4a`)
- **FLAC** (`.flac`)
- **WAV** (`.wav`)
- **AAC** (`.aac`)
- **OGG** (`.ogg`)
- **OPUS** (`.opus`)
- **WMA** (`.wma`)
- **AIFF / MIDI** (`.aiff`, `.mid`)

### Intelligent 5KB File Filter
Corrupt downloads, 0-byte placeholders, and junk ringtone snippets ($\le 5\text{ KB}$) are automatically filtered out during scanning, keeping your library pristine.

### Exact Millisecond Resume Engine
- **$\ge 5\%$ Progress**: If a track is stopped after reaching 5% of its duration, Raga remembers its exact millisecond position. When re-opened or re-selected, playback resumes seamlessly from that exact millisecond.
- **$< 5\%$ Progress**: If skipped or stopped before reaching 5%, it starts cleanly from `00:00`.
- **Completed Tracks ($\ge 98\%$)**: Once finished, the track resets to the beginning for its next play.

### Seamless Crossfade Transitions
Smoothly crossfade between consecutive tracks. Adjustable from **0s (instant transition)** up to **12 seconds** of gradual audio overlapping.

### Variable Speed & Pitch Control
- **Playback Speed**: Adjust tempo from **0.5x to 2.0x** in real-time.
- **Pitch Preservation**: Maintains natural pitch or adjust it for musical practice, transcription, or podcast listening.

### A-B Segment Looping
- Set **Point A** and **Point B** anywhere in the current track.
- The player will seamlessly loop the marked segment indefinitely.
- Quick 1-tap toggles to reset or clear loop points.

### Super Boost (Volume Amplifier)
For quiet recordings or low-impedance headphones, Super Boost amplifies gain above standard system limits up to **200%** with built-in limiter protection to minimize distortion.

### Repeat & Shuffle Controls
- **Repeat Off**: Plays sequentially and stops when the queue finishes.
- **Repeat All**: Cycles through your playlist continuously.
- **Repeat One**: Loops the current track endlessly.
- **Smart Shuffle**: Randomizes track selection while ensuring no track repeats until all unplayed tracks in the session are exhausted.

---

## 🧠 Smart Session Played Tracker

Raga solves the common annoyance of random shuffle repeating the same tracks:

1. **The 60% Rule**: Once a track reaches **60%** of its duration, it is permanently marked as **Played** in the current session.
2. **Uninterrupted Across Shutdowns**: Pressing the shutdown button or restarting the app **never interrupts** this memory. It remains safely committed to disk.
3. **Full Cycle Preservation**: The session memory stays intact until **all tracks** in the active playlist have been played. Only after every song is heard does a new cycle begin.
4. **Visual Session Card**:
   - Live progress indicator showing session completion percentage.
   - Clear fraction counter (e.g. `24 / 50 played`).
   - Unplayed badge count (`26 unplayed remaining`).
   - Manual **Reset Session** button if you want to start fresh anytime.

---

## 🎛️ Equalizer & Audio Effects

- **5-Band Graphic Equalizer**: Real-time frequency band adjustments ($\pm 12\text{ dB}$) with interactive sliders.
- **Built-In Audio Presets**:
  - *Flat*
  - *Bass Boost*
  - *Rock*
  - *Pop*
  - *Vocal*
  - *Classical*
  - *Jazz*
  - *Electronic*
  - *Custom* (remembers your manual band tweaks)
- **Hardware Bass Boost**: Dedicated low-frequency boost slider for punchy sub-bass.
- **3D Virtualizer**: Expands the soundstage for spatial audio immersion over headphones and stereo speakers.

---

## 📁 Folder Management & Multi-Directory Indexing

### Storage Access Framework (SAF)
Pick individual folders from internal memory or external SD cards using Android's native directory picker. Permissions are persisted across device reboots.

### Device Audio Auto-Scan
Instantly discover all music stored across Android MediaStore with a single tap.

### Duplicate Detection
Intelligent URI decoding and path normalization ensures duplicate folders cannot be accidentally added.

### Subfolder Precision
- Toggle subfolder inclusion on or off with a single tap.
- Individual subfolder checklist allows you to exclude specific subdirectories (e.g. ringtones, voice memos, WhatsApp audio).

---

## 🔀 Sorting & Queue Management

Organize your library with instant client-side sorting:
- **Folder-Wise**: Keeps albums and directory hierarchies together in their original album order.
- **Alphabetical (A–Z)**: Sorts alphabetically by track title.
- **Newest First**: Displays recently modified or newly added songs first.
- **Oldest First**: Chronological order by file creation/modification timestamp.

---

## 🎨 Visual Styling & Themes

- **Material Design 3 (M3)**: Fluid layouts, adaptive surface elevations, responsive bottom sheets, and edge-to-edge system insets.
- **Dynamic Artwork Palette**: Dynamically extracts vibrant, muted, and dominant tones from the current song's album art using `androidx.palette` to tint backgrounds, cards, and sliders.
- **OLED Pure Black Mode**: True `#000000` deep black theme designed for maximum battery saving on AMOLED displays.
- **Color Presets**:
  - *Dynamic Artwork Accent*
  - *Indigo*
  - *Emerald*
  - *Crimson*
  - *Sunset*
  - *Purple*
  - *OLED Pure Black*
- **Real-Time Audio Visualizer**: Animated equalizer visualizer bars reflecting active playback.

---

## 🖼️ Online Album Artwork Search

Missing cover art? Raga features an embedded album art search service:
- Pulls high-resolution cover art across **JioSaavn, Gaana, YouTube Music, iTunes, and Deezer**.
- Displays interactive art results with thumbnail previews.
- 1-tap cover art application assigns the artwork directly to the track and caches it locally.

---

## 📲 System & Hardware Integration

- **Android MediaStyle Notifications**: Lockscreen and status bar controls with interactive timeline scrub bar, play/pause, skip, and artwork display.
- **Audio Focus & Call Ducking**: Automatically pauses or lowers volume when receiving incoming calls.
- **"Only Calls Interrupt" Toggle**: Filters out non-essential notification chime interruptions so your music stays uninterrupted.
- **Noisy Audio Broadcast Receiver**: Automatically pauses playback the instant headphones or Bluetooth devices disconnect (`ACTION_AUDIO_BECOMING_NOISY`), preventing accidental loudspeaker playback.
- **Sleep Timer with Fade-Out**: Choose between **5, 10, 15, 30, 45, 60 minutes**, or **End of Track**. Features gentle volume ramping before pausing to avoid abrupt awakenings.

---

## 🛑 Clean Shutdown & Uninstall Guarantee

### Dedicated Shutdown Button
Located directly in the top app bar next to settings:
- Commits all playback positions and session played states synchronously to storage.
- Completely releases hardware `MediaPlayer` and audio effects.
- Stops background services and clears notification channels.
- Releases system audio focus.
- Terminates the app process and removes it cleanly from Android's Recents stack (`finishAndRemoveTask`).

### Zero Leftover Traces
- Uses strictly scoped internal storage and standard `SharedPreferences`.
- Does **NOT** create hidden root directories, telemetry caches, or external tracking files.
- When uninstalled, the Android OS removes 100% of application data with zero orphaned files remaining on your device.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture / MVVM
- **State Management**: `StateFlow`, `SharedFlow`, Kotlin Coroutines
- **Media Engine**: Android Open Source Platform `MediaPlayer`, `MediaSessionCompat`, `Visualizer`, `Equalizer`, `BassBoost`, `Virtualizer`
- **Image Pipeline**: Coil Compose with disk-caching and high-resolution thumbnail decoders
- **Palette**: `androidx.palette.ktx` for real-time luminance and color swatching
- **Testing**: Robolectric JVM unit tests & Roborazzi screenshot verification

---

## 🇮🇳 Author & Support

Crafted with dedication by **Arun**:

- **Origin**: Made in India 🇮🇳
- **Created**: With Help of Google AI Studio By Arun
- **Support the Project**: If you enjoy using Raga Music Player, consider supporting development:
  👉 **[buymeachai.in/arun25](https://buymeachai.in/arun25)**

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute with attribution.
