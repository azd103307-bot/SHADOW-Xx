# Unreal Glow Balls (UE 5.4)

A clean Unreal Engine 5 C++ starter for a physics-driven glowing-ball game.

## Current starter features
- Physics-simulated rolling sphere with configurable drive force and mass.
- Follow camera with camera lag.
- Point-light glow attached to the ball.
- Keyboard movement (WASD) and jump (Space).
- Lumen GI and reflections enabled in project renderer settings.

## Open
1. Install Unreal Engine 5.4 through Epic Games Launcher.
2. Open `UnrealGlowBalls.uproject` and allow Unreal to generate project files.
3. Build the `UnrealGlowBallsEditor` target.
4. Create a level with a floor and add an instance of `GlowBallPawn`; set it as the default pawn in a GameMode/World Settings.

## Important
This is the C++ gameplay foundation, not a finished packaged game. It still needs a designed arena, collision obstacles, polished emissive materials, UI, mobile touch controls, sound, and Android packaging. Unreal Engine must be installed to compile and package it; GitHub-hosted Android runners do not include Unreal Engine.
