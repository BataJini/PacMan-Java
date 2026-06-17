# NEON PAC-MAN (Java)

A premium, neon-arcade Pac-Man built in **pure Java 17 + Swing** — no external
libraries, no image files, no audio files. Every sprite is drawn procedurally
with `Graphics2D`; every sound is synthesized at runtime with
`javax.sound.sampled`.

![Neon Pac-Man](out/menu_clean.png)

## ✨ Highlights

- **Single Java2D render surface** with antialiasing, glow/bloom, and a cached
  neon-tube maze image — smooth, resolution-independent, resize-safe.
- **One fixed-timestep (60 Hz) game loop on the EDT.** All game state mutates in
  one place, on one thread — the old per-ghost threads and their race conditions
  are gone. The only background activity is audio playback.
- **Smooth pixel-interpolated movement** on a grid, with turn buffering, instant
  reverse, and side-tunnel wrap-around.
- **Authentic 4-personality ghost AI** — Blinky / Pinky / Inky / Clyde with their
  classic targeting, a scatter ⇄ chase schedule, frightened mode (with flashing
  warning), and eyes-only ghosts that return home and revive.
- **Procedural neon vector art** — animated Pac-Man mouth, ghosts with eyes that
  track movement, pulsing power pellets, eight bonus fruits, score popups, and a
  death animation.
- **Synthesized retro audio** — chomp, power siren, ghost-eat arpeggio, death
  jingle, extra life, fruit, and menu blips. Toggle with `M`.
- **Polished UI** — animated attract menu, arcade HUD (score / high / level /
  lives / fright timer), READY!/PAUSED/GAME OVER overlays, name entry, and a
  medal-coloured high-scores table.
- **Levels & difficulty ramp**, three board sizes, power-ups, fruit bonuses,
  extra life at 10,000, and persistent high scores.

## 🚀 Run

Requires a JDK (17+).

```bash
# Compile (UTF-8 needed for the neon glyphs)
javac -encoding UTF-8 -d out $(find src -name "*.java")

# Run
java -cp out Main
```

On Windows PowerShell:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp out Main
```

## 🎮 Controls

| Action | Keys |
|---|---|
| Move | Arrow keys / WASD |
| Pause | P |
| Mute | M |
| Restart game | R |
| Back to menu | Q / Esc / Backspace |
| Confirm / dismiss | Enter / Space |

In the menu: `↑↓` select, `←→` change board size, `Enter` to play.

## 🧱 Architecture

One `JFrame` + `CardLayout` routes between **Menu**, **Game**, and **High Scores**.

```
src/
├── Main.java                # entry → menu/App
├── menu/                    # App router, MainMenu, MenuButton, HighScoresScreen
├── game/                    # GameModel (world+rules), GameLoop, GamePanel,
│                            #   Board, Hud, Overlays, GameScreen, Map, ...
├── characters/              # Entity, Pacman, Ghost, Direction, modes/personalities
├── assets/                  # Palette, Fonts, SpritePainter (art),
│                            #   Synth, Cue, SoundEngine (audio)
└── highscores/              # HighScoreManager + HighScoreEntry (highscores.ser)
```

See [SPEC.md](SPEC.md) for the full implementation specification.

---

*Pure Java 17 stdlib • procedural art & audio • single-thread game loop • authentic ghost AI*
