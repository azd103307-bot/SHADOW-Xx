# Unreal Glow Balls — Unreal Engine 5.4

A C++ Unreal Engine 5 starter for a physics-based glowing-ball arena.

## Implemented
- Physics-driven rolling player ball with configurable mass, force, damping, and speed cap.
- Follow camera with camera lag.
- Blue point light and bloom post-process around the player.
- Jump with grounded-state detection.
- Runtime-built arena floor, perimeter walls, pillars, and low barriers.
- Twelve floating cyan energy pickups with overlap collection and a completion message.
- Four illuminated arena beacons.
- Mobile virtual joystick configuration and gamepad jump binding.
- Mobile-friendlier renderer settings: mobile HDR, bloom, and no Lumen dependency.

## Project location
Open `UnrealGlowBalls/UnrealGlowBalls.uproject` with Unreal Engine 5.4 or a compatible UE5 version.

## Build
1. Install Unreal Engine 5.4 and the C++ build toolchain on a Windows development machine.
2. Open the project and allow Unreal to generate project files.
3. Compile the editor target.
4. Create or open a level, set the project's GameMode to `GlowBallGameMode`, then press Play.
5. For Android, configure Android SDK/NDK in Unreal's Project Settings, enable Android support, set a supported minimum SDK, then package for Android.

## Important limits
This repository contains the C++ gameplay foundation, not a finished APK. The arena is spawned at runtime by the GameMode. The project has not been compiled with Unreal Engine in this environment, so it still requires an Unreal Editor build and device testing. The default Unreal mobile virtual joystick is enabled; a custom on-screen jump button and a dedicated mobile HUD remain to be added and tested.
