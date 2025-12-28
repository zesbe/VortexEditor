# VortexEditor

A professional video editing app for Android with a native C++ core engine for maximum performance.

## Features

- **Video Editing**: Trim, cut, split, merge video clips
- **Speed Control**: 0.1x to 10x speed adjustment
- **Filters & Effects**: Color filters, blur, sharpen, vignette, etc.
- **Audio**: Add music, sound effects, voiceover
- **Text Overlay**: Add text with various fonts and styles
- **Export**: Export to 720p, 1080p, or 4K resolution

## Architecture

```
┌─────────────────────────────────────────┐
│           Kotlin (UI Layer)             │
│  • Activities / Fragments               │
│  • ViewModels                           │
│  • RecyclerView (Timeline)              │
├─────────────────────────────────────────┤
│              JNI Bridge                 │
├─────────────────────────────────────────┤
│          C++ Core Engine                │
│  • Video Decoder (MediaCodec)           │
│  • Video Encoder (MediaCodec)           │
│  • Audio Engine (OpenSL ES)             │
│  • Filter Manager (OpenGL ES)           │
│  • Timeline Manager                     │
│  • Frame Buffer                         │
└─────────────────────────────────────────┘
```

## Tech Stack

- **Language**: Kotlin (UI) + C++17 (Core)
- **Video Processing**: Android MediaCodec NDK
- **Audio**: OpenSL ES
- **Graphics**: OpenGL ES 3.0
- **ML/AI**: ML Kit (Background Removal)
- **UI**: Material Design 3

## Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- NDK 25+
- CMake 3.22.1+
- Min SDK: 24 (Android 7.0)

## Building

1. Clone the repository:
```bash
git clone https://github.com/zesbe/VortexEditor.git
cd VortexEditor
```

2. Open in Android Studio

3. Sync Gradle

4. Build and run on device/emulator

## Project Structure

```
app/
├── src/main/
│   ├── cpp/                    # C++ Native Code
│   │   ├── engine/            # Core video engine
│   │   ├── filters/           # Video filters & effects
│   │   ├── utils/             # Utility classes
│   │   └── jni/               # JNI bridge
│   ├── java/.../              # Kotlin source code
│   │   ├── core/              # Native engine wrapper
│   │   └── ui/                # UI components
│   └── res/                   # Resources
```

## License

MIT License

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
