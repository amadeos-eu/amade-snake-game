# amade-snake-game

**amade-snake-game** is the classic snake on the [amadeOS](https://github.com/amadeos-eu/amadeos) desktop: steer it to the apples, let it grow, and never bite the wall or yourself. Built on [amade-game-engine](https://github.com/amadeos-eu/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **Enter**, **Space** or a **click** (or F2) starts a game. The snake waits a moment; an arrow key starts it at once.
- Steer with the **arrow keys** (or W, A, S, D). Two quick presses before the snake moves on make two turns, so a tight U-turn works.
- Every **apple** gives 10 points and makes the snake one cell longer and a little faster, from 6.7 up to 12.5 cells a second. After every fifth apple a **golden apple** appears for a few seconds: 50 points, and it blinks before it goes.
- Running into the **wall** or the snake's own **body** ends the game. Its tail is free, because it moves on.
- **P** or **Esc** pauses, and so does leaving the window.
- **Best scores**, ten of them. The **title screen replays your best game**, cell by cell, and lets the computer play in between.
- **F11** switches to full screen and back. *Game → Graphics* chooses how the field reaches the screen: **Direct** (sharp, at the screen's full size) or **Pixels** (the field at 640 × 400, enlarged with hard square pixels). **F12** shows how long each frame takes.
- Sound effects can be switched off under *Game → Sound*.

The keys can be changed in `~/.config/amadeos/snake/snake.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.up=UP W
keys.down=DOWN S
keys.left=LEFT A
keys.right=RIGHT D
keys.pause=P ESCAPE
```

Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (graphics, sound, keys) | `~/.config/amadeos/snake/snake.properties` |
| Name for high scores | `~/.config/amadeos/snake/player.properties` |
| Best scores | `~/.local/share/amadeos/games/snake/classic.scores` |
| The best game, as a replay | `~/.local/share/amadeos/games/snake/best.replay` |

A replay is text: the seed of the game, then each turn with the tick it was given at. Delete the file to forget the best game.

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-snake-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

All pictures of the game are drawn in code, as sprites. The icon is drawn by `tools/MakeIcon.java` (see the comment at its top). `./gradlew screenshots -Pshots=<folder>` draws the handbook's screenshots.

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface or time of their own. `Game` (the snake, the turns waiting for the next ticks, apples placed by a seeded random generator, score, pace; it reports `Event`s), `Cell`, `Direction`, `Field` (sizes, pace and points), `Autopilot` (plays the title screen's demo), `GameResult`.
- `repository`: `SnakeSettings` (graphics, sound, keys), `ReplayFile` (the best game).
- `ui.game`: the running game on the loop thread. `SnakeGame` (loop, ticker, controls, scenes: title, game, pause, end; it records each game as a replay and plays the best one back), `FieldPainter` (lawn, wall, apples and the snake, moving smoothly between ticks), `Hud` (score, length and pace, and the texts), `Effects` (events become juice, splinters and sounds), `SoundBank`, `Arena` (what the presenter may ask of the game).
- `ui.main`: Model-View-Presenter for everything around the field: `SnakePresenter`, `SnakeView`, `SnakeWindow`.

From the engine: `GameLoop` and `Ticker`, `SeededRandom`, `Replay`, `GameCanvas`, `Sprite`, `Particles`, `Sound` and `Mixer`, `Keyboard`, `Mouse` and `Controls`, `SceneStack`, `FullScreen`, high scores.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://docs.amadeos.eu/amade-game-engine/) quotes; chapter 22 is about this game.

## Licence

[EUPL-1.2](LICENSE)
