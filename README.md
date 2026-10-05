# Crownfall: Legends of the Realm

A medieval chess game for Android. Command the Kingdom of Dawn against the Empire of
Dusk on a carved stone battlefield where every capture plays out as a small duel —
charging knights, holy strikes, shield bashes, dust clouds and screen rumble.

Built with Kotlin and Jetpack Compose, with a pure-Kotlin chess engine and AI that are
fully unit tested on the JVM.

## Features

- **Complete chess rules** — legal-move generation, check/checkmate, stalemate,
  castling, en passant, promotion, fifty-move and insufficient-material draws.
- **Capture cinematics** — each piece has its own attack, death, idle and movement
  animations, with camera push-in, particle effects and impact shake.
- **AI opponent** — four difficulties (Beginner, Hard, Expert, Master) with alpha-beta
  search, move ordering and a transposition table.
- **Local two-player** — pass-and-play on one device, with an optional board rotation
  for the active player.
- **Progress tracking** — match history, statistics, piece statistics and achievements.
- **Settings** — music and sound volume, combat sounds, capture cinematics, animation
  speed, board rotation, move hints, coordinates, **screen shake** on/off, and default
  AI difficulty.

The game is played in landscape orientation.

## Requirements

- **Android Studio** (latest stable) — recommended, or a command-line setup with:
  - **JDK 17**
  - **Android SDK** with **Platform 36** installed (compileSdk 36)
- An Android device or emulator running **Android 7.0 (API 24)** or newer.

## Install and run

### Option A — Android Studio

1. Clone the repository:
   ```bash
   git clone https://github.com/KiroOoo-arch/CrownFall.git
   ```
2. Open the `CrownFall` folder in Android Studio
   (`File → Open…` and select the project root).
3. Let Gradle sync finish (it downloads Gradle 9.5 and the Android/Kotlin plugins on
   first run).
4. Connect a device over USB (with USB debugging enabled) or start an emulator, then
   press **Run ▶**.

Android Studio creates `local.properties` for you. If it does not, see step 2 of
Option B.

### Option B — Command line

1. Make sure `JAVA_HOME` points at a JDK 17 and the Android SDK platform-tools are on
   your `PATH`.
2. Point the build at your Android SDK by creating `local.properties` in the project
   root (this file is machine-specific and is not committed):
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
   On Windows, escape the separators, for example:
   ```properties
   sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
   ```
3. Build, install and launch on a connected device or running emulator:
   ```bash
   ./gradlew installDebug
   ```
   On Windows use `gradlew.bat installDebug`.

### Build an APK manually

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds are unsigned; produce one with `./gradlew assembleRelease` and sign it
with your own keystore before distributing.

## Running the tests

All game logic (chess rules, engine, AI, animation timing, repositories) is covered by
JVM unit tests — no emulator needed:

```bash
./gradlew testDebugUnitTest
```

## How to play

- Tap one of your pieces, then tap a highlighted square to move.
- A crown marks the king in check; the gold pulse shows the square under threat.
- When a pawn reaches the far rank, choose its new role.
- Use **Undo Move**, **Offer Truce** and **Resign** on the right rail, or **Pause** for the
  full menu.
- Pinch to zoom the battlefield, drag to pan while zoomed, and double-tap to reset the
  view.

## Project structure

```
app/src/main/java/com/crownfall/realm/
├── data/          Room database, DAOs, entities and repositories
├── domain/
│   ├── ai/        Search, evaluation, move ordering, transposition table
│   ├── animation/ Piece poses, cinematics and sampling
│   ├── audio/     Sound effect catalogue
│   └── chess/     Board, rules, moves, FEN, game engine
├── ui/            Compose screens (game, board, menus, settings, stats, history)
└── navigation/    Single Compose navigation graph
```

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2 |
| UI | Jetpack Compose (Material 3) |
| Persistence | Room |
| Async | Kotlin Coroutines / Flow |
| Build | Gradle 9.5, Android Gradle Plugin 8.13 |
| Min / target SDK | 24 / 36 |
