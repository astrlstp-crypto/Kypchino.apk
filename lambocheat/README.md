# LamboCheat

Android companion/overlay menu for **SchoolBoy Runaway**.

## Current build

- App name: **LamboCheat**
- `CHEAT` button starts a floating overlay and launches SchoolBoy Runaway.
- Floating `T` button stays over the game and opens the menu.
- Menu contains Levitation, Tyson Mode, item picker, Noclip + Fly, anger mother/father, secret door, safe, and front door actions.
- Item picker includes commonly documented SchoolBoy Runaway items.

### Technical note

The overlay UI is functional, but the game-state hook is deliberately isolated behind `runCommand(...)`. Android app sandboxing means a separate non-root overlay (including Shizuku shell access) cannot directly rewrite another app's Unity state. To make the actual in-game toggles functional, a version-specific game hook/mod is required.

The APK is built by GitHub Actions and published to the `lambocheat-latest` release.
