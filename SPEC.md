# Neon Pac-Man — Authoritative Implementation Spec (Java 17 / Swing)

> Single source of truth. Implement literally. Pure Java 17 stdlib only
> (`javax.swing`, `java.awt`, `java.awt.geom`, `javax.sound.sampled`). No external
> libraries, no image files, no audio files. All art is drawn procedurally with
> `Graphics2D`; all audio is synthesized with `javax.sound.sampled`.
>
> Where the three source specs disagreed, this document picks ONE value. Notable
> resolutions are flagged inline as **[RESOLVED]**.

---

## 1. ONE-LINE VISION

A polished neon-arcade Pac-Man: one `JPanel` rendering smooth pixel-interpolated vector sprites with bloom, one fixed-timestep 60 Hz loop on the EDT (zero game-state threads), authentic 4-personality ghost AI, and synthesized retro audio — all in pure Java 17 stdlib.

---

## 2. FINAL PALETTE

Defined once in `assets/Palette.java` as `public static final Color` constants. Use `new Color(r,g,b)` / `new Color(r,g,b,a)`.

| Name | Hex | RGB | Use |
|---|---|---|---|
| `BG_DEEP` | `#05060E` | 5, 6, 14 | Window/panel base fill |
| `BG_VIGNETTE` | `#0A0B1A` | 10, 11, 26 | Center of radial vignette |
| `BG_GRID_LINE` | `#121634` | 18, 22, 52 | Faint background grid (alpha 40) |
| `PLAYFIELD_INK` | `#03040A` | 3, 4, 10 | Inside-maze fill (darker, for depth) |
| `WALL_CORE` | `#3D6BFF` | 61, 107, 255 | Bright inner neon filament (L1 hue) |
| `WALL_EDGE` | `#1B2E8C` | 27, 46, 140 | Dark outer band of the neon tube |
| `WALL_GLOW` | `#4D7BFF` | 77, 123, 255 | Glow layer |
| `WALL_GLOW_SOFT` | `#6E92FF` | 110, 146, 255 | Outermost faint halo |
| `GATE` | `#FFB8FF` | 255, 184, 255 | Ghost-house door bar |
| `PELLET` | `#FFE9B0` | 255, 233, 176 | Dot core |
| `PELLET_GLOW` | `#FFD27A` | 255, 210, 122 | Dot halo |
| `POWER_PELLET` | `#FFFFFF` | 255, 255, 255 | Power pellet core |
| `POWER_GLOW` | `#FFE08A` | 255, 224, 138 | Power pellet halo |
| `PAC` | `#FFE21F` | 255, 226, 31 | Pac-Man body |
| `PAC_HI` | `#FFF7A8` | 255, 247, 168 | Pac-Man highlight |
| `PAC_GLOW` | `#FFD400` | 255, 212, 0 | Pac-Man glow |
| `GHOST_RED` | `#FF2B4E` | 255, 43, 78 | Blinky body |
| `GHOST_RED_GLOW` | `#FF6B85` | 255, 107, 133 | Blinky glow |
| `GHOST_PINK` | `#FF8AD0` | 255, 138, 208 | Pinky body |
| `GHOST_PINK_GLOW` | `#FFB6E6` | 255, 182, 230 | Pinky glow |
| `GHOST_CYAN` | `#22E0FF` | 34, 224, 255 | Inky body |
| `GHOST_CYAN_GLOW` | `#8AF2FF` | 138, 242, 255 | Inky glow |
| `GHOST_ORANGE` | `#FFA62B` | 255, 166, 43 | Clyde body |
| `GHOST_ORANGE_GLOW` | `#FFC878` | 255, 200, 120 | Clyde glow |
| `EYE_WHITE` | `#F4F8FF` | 244, 248, 255 | Sclera |
| `PUPIL` | `#1A1F66` | 26, 31, 102 | Pupil |
| `FRIGHT_BODY` | `#2330FF` | 35, 48, 255 | Frightened body |
| `FRIGHT_GLOW` | `#6A78FF` | 106, 120, 255 | Frightened glow |
| `FRIGHT_FACE` | `#FFD6E0` | 255, 214, 224 | Frightened mouth/eyes |
| `FRIGHT_FLASH` | `#FFFFFF` | 255, 255, 255 | Flash-white body (warning) |
| `FRIGHT_FLASH_FACE` | `#FF2B4E` | 255, 43, 78 | Flash mouth/eyes |
| `TEXT_PRIMARY` | `#EAF0FF` | 234, 240, 255 | Main HUD/menu text |
| `TEXT_DIM` | `#7C86B8` | 124, 134, 184 | Secondary labels |
| `TEXT_GLOW` | `#5C7BFF` | 92, 123, 255 | Text shadow glow |
| `ACCENT_CYAN` | `#19E0C8` | 25, 224, 200 | Selected accent, timer bar |
| `ACCENT_MAGENTA` | `#FF3DD0` | 255, 61, 208 | Hover accent, popups |
| `READY_YELLOW` | `#FFE21F` | 255, 226, 31 | "READY!" |
| `GAMEOVER_RED` | `#FF2B4E` | 255, 43, 78 | "GAME OVER" |
| `GOLD` | `#FFD24A` | 255, 210, 74 | 1st-place medal |
| `SILVER` | `#CFE0FF` | 207, 224, 255 | 2nd-place medal |
| `BRONZE` | `#E08A4A` | 224, 138, 74 | 3rd-place medal |
| `SCORE_POP` | `#9CFBFF` | 156, 251, 255 | Ghost-eat score popups |
| `FRUIT_POP` | `#FF6BA8` | 255, 107, 168 | Fruit score popups |

**Per-level maze hue rotation** (rotate `WALL_CORE`/`WALL_EDGE`/`WALL_GLOW`/`WALL_GLOW_SOFT` together, keep glow hue-locked to core):
L1 blue `#3D6BFF` · L2 magenta `#B14DFF` · L3 teal `#19E0C8` · L4 violet `#7C4DFF` · then cycle.

---

## 3. ARCHITECTURE

### 3.1 Screen-flow model — ONE JFrame + CardLayout

```
App (the single JFrame)
 └─ root JPanel (CardLayout)
     ├─ "MENU"   → MainMenu        (JPanel)
     ├─ "GAME"   → GameScreen      (JPanel → wraps GamePanel + owns GameLoop)
     └─ "SCORES" → HighScoresScreen(JPanel)
 (GAME OVER and name-entry are overlays drawn inside GameScreen — no separate card.)
```

- `App` owns the `JFrame`, the `CardLayout`, the `SoundEngine`, and the `HighScoreManager`.
- `App.show(card)` calls `currentScreen.onHide()` then `cards.show(...)` then `next.requestFocusInWindow()`.
- `GameScreen.onHide()` → `loop.stop()` + `sound.stopSiren()`. Entering GAME builds a fresh `GameModel` + `GameLoop` and `loop.start()`. Guarantees the timer never runs off-screen.
- `JFrame` uses `DO_NOTHING_ON_CLOSE` + a `WindowListener` → `App.quit()` → `loop.stop()`, `sound.shutdown()`, `System.exit(0)`. No orphaned threads.
- `Main.main` → `SwingUtilities.invokeLater(() -> new App().show("MENU"))`.

### 3.2 Final class list per package

Status legend: **KEEP** (unchanged) · **KEEP-API** (format/contract frozen) · **REPLACE** (rewrite body) · **NEW** · **DELETE**.

#### `assets` — procedural art & audio (no PNGs)
| Class | Status | Responsibility |
|---|---|---|
| `Palette` | NEW | All neon `Color` constants + `glow(Color,float a)`, `lerp(Color,Color,float t)`, `hueShift(Color,float deg)`. |
| `Fonts` | NEW | Lazy cached derived monospace fonts: `arcade(float size)`, `hud(float size)`, with tracking; fallback chain Consolas→DejaVu Sans Mono→Courier New→`Font.MONOSPACED`. |
| `SpritePainter` | NEW | Pure static `Graphics2D` recipes: `pacman`, `ghostBody`, `ghostEyes`, `frightenedFace`, `eyesOnly`, `pellet`, `powerPellet`, `fruit`, `wallSegment`, `neonText`. |
| `Synth` | NEW | Pure PCM generators → `byte[]` (16-bit, 44100 Hz, mono): `square`, `sine`, `triangle`, `noiseBurst`, `sweep`, `arpeggio`, with ADSR. No I/O. |
| `AudioFormatSpec` | NEW | Constants `SAMPLE_RATE=44100`, `BITS=16`, mono signed little-endian; shared `AudioFormat`. |
| `Cue` (enum) | NEW | One-shot SFX catalog with synth params (see §8). |
| `SoundEngine` | NEW | Public audio facade: single daemon audio thread, `play(Cue)`, `startSiren(SirenLevel)`, `stopSiren()`, `setMuted`, `isMuted`, `shutdown`. The ONLY non-EDT thread. |
| `Assets` | REPLACE | Thin facade re-exporting `Palette`/`Fonts` for import stability during migration; no `ImageIcon`. Delete once unused. |

#### `characters` — pure state, no threads/Swing
| Class | Status | Responsibility |
|---|---|---|
| `Direction` (enum) | NEW | `NONE,UP,DOWN,LEFT,RIGHT` with `dx,dy`, `opposite()`, `fromKey(int)`. |
| `Entity` (abstract) | NEW | Grid coords (authoritative) + pixel interpolation `progress∈[0,1)`, tunnel wrap, `stepToward(double dt, GameModel)`. |
| `Pacman` | REPLACE | Player state: `dir`, `desiredDir`, mouth phase, death-anim phase. No icons/threads. |
| `GhostPersonality` (enum) | NEW | `BLINKY,PINKY,INKY,CLYDE` → `color()`, `scatterTarget(GameModel)`, `chaseTarget(GameModel,self,pac,blinky)`. |
| `GhostMode` (enum) | NEW | `IN_HOUSE, LEAVING_HOUSE, SCATTER, CHASE, FRIGHTENED, EATEN`. |
| `Ghost` | REPLACE | One ghost: position + mode + personality; AI is pure functions called from the loop (greedy tile-pick), no thread. |
| `UpgradeGhost` | DELETE | Random upgrade-spawning ghost removed. |
| `UpgradeType` | DELETE | Folded away; bonuses come from authentic fruit (`FruitType`). |

#### `game` — model, loop, rendering, screens
| Class | Status | Responsibility |
|---|---|---|
| `TileType` (enum) | NEW | Decodes extended map codes (§4); `blocksPacman()`, `blocksGhost(GhostMode)`. |
| `Map` | KEEP-API | Static board provider; `getMap(size)` returns `int[][]` with extended codes. Sizes/signatures unchanged. |
| `Board` | NEW | Mutable runtime grid: tile reads, eat ops, live dot/power counts, tunnel/house metadata, spawns, `wrapX`, win check. Sole owner of board mutation. |
| `BoardSizeOption` (enum) | NEW | `SMALL(Map.SMALL_MAP), MEDIUM(Map.MEDIUM_MAP), LARGE(Map.LARGE_MAP)`; ghost count. |
| `LevelConfig` (record) | NEW | Immutable per-level tuning (speeds, fright secs/flashes, elroy, fruit, mode timeline). |
| `LevelConfigFactory` | NEW | Pure `forLevel(int)` building `LevelConfig` from the tables in §6. |
| `FruitType` (enum) | NEW | Cherry…Key per level, points, procedural draw recipe. |
| `ModeScheduler` | NEW | Scatter/chase timeline (frame counter); frightened window; "forced reverse" signal. |
| `ScorePopup` (record) | NEW | Floating score text entity (value, color, position, ttl). |
| `Particle` (record) | NEW | Death-burst particle. |
| `GamePhase` (enum) | NEW | `READY, PLAYING, DYING, LEVEL_CLEAR, GAME_OVER, PAUSED`. |
| `GameModel` | NEW | Entire mutable world for one game; `update(double dt)` mutated only on EDT. |
| `GameLoop` | NEW | Fixed-timestep `javax.swing.Timer` driver (accumulator); calls `model.update` + `panel.repaint`. Replaces all threads. |
| `GamePanel` | NEW | The single custom `JPanel.paintComponent(Graphics2D)`; coord/scale, maze `BufferedImage` cache, draw order, key bindings. Reads model, never mutates (except queueing input). |
| `Hud` | NEW | Draws score/high/level/lives/timer-bar into `GamePanel`. |
| `Overlays` | NEW | Draws READY!, PAUSED, GAME OVER, level-clear flash, name-entry. |
| `GameScreen` | NEW | CardLayout "GAME" panel; wraps `GamePanel`, owns `GameLoop` lifecycle + game-over→score flow. |
| `Game` | REPLACE | Demoted from `JFrame` to a small factory: `static GameModel newModel(BoardSizeOption, SoundEngine)`. |
| `GameState` | DELETE | Folded into `GameModel`. |
| `GameLogic` | DELETE | Split into `GameModel`/`Board`/`ModeScheduler`. |
| `GameUI` | DELETE | Replaced by `GamePanel`/`Hud`/`Overlays`. |

#### `menu`
| Class | Status | Responsibility |
|---|---|---|
| `App` | NEW | Single `JFrame` + `CardLayout` router; owns `SoundEngine`, `HighScoreManager`; lifecycle. |
| `MainMenu` | REPLACE | `JFrame`→`JPanel`: animated neon menu, board-size selector, attract chase motif, custom buttons. |
| `MenuButton` | NEW | Reusable custom-painted focusable neon pill button (idle/hover/focus/press). |
| `HighScoresScreen` | NEW | Neon high-scores panel (was `highscores/HighScore` JFrame) inside CardLayout. |

#### `highscores` — format preserved
| Class | Status | Responsibility |
|---|---|---|
| `HighScoreEntry` | KEEP-API | Field shape frozen (`String name`, `int score`); add `serialVersionUID=1L`; add `getName()`. |
| `HighScoreManager` | KEEP | `highscores.ser`, top-10, sort desc; add `qualifies(int)`; replace `printStackTrace` with handled logging. |
| `HighScore` (JFrame) | DELETE | Replaced by `menu/HighScoresScreen`. |

#### root
| Class | Status | Responsibility |
|---|---|---|
| `Main` | KEEP | One line: `invokeLater(() -> new App().show("MENU"))`. |

#### Files to DELETE
`src/assets/*.png`, `out/assets/*.png`, `out/*.png`, `src/characters/UpgradeGhost.java`, `src/characters/UpgradeType.java`, `src/game/GameLogic.java`, `src/game/GameUI.java`, `src/game/GameState.java`, `src/highscores/HighScore.java`.

---

## 4. TILE ENCODING

Keep `Map.getMap(size)` returning `int[][]`. Widen the code space; `TileType.of(int)` decodes.

| Code | TileType | Pac-Man | Ghost | Notes |
|---|---|---|---|---|
| `0` | `EMPTY` | pass | pass | eaten dot / blank |
| `1` | `WALL` | block | block | static, cached in maze image |
| `2` | `DOT` | pass + eat (10) | pass | |
| `3` | `POWER` | pass + eat (50, frightens) | pass | |
| `4` | `HOUSE` | block | pass | ghost-house interior |
| `5` | `GHOST_DOOR` | block | pass only if `EATEN`/`LEAVING_HOUSE` | door bar |
| `6` | `TUNNEL` | pass + wrapX | pass + wrapX, slowed | side-edge wrap tiles |
| `7` | `FRUIT_SPOT` | pass + eat fruit if present | pass | bonus spawn marker (also walkable) |
| `8` | `PAC_SPAWN` | pass (start) | pass | treated as `EMPTY` for pellets |
| `9` | `GHOST_SPAWN` | block | pass (spawn slot) | inside house |

**[RESOLVED] Map upgrade is done at load time inside `Board`, NOT by editing `Map.java`'s constants.** `Board` copies the raw `int[][]`, then applies the deterministic transform below. The raw maps keep `1`=wall, `0`=path. **Actual verified dimensions** (do not trust earlier drafts): Small **15 cols × 15 rows**, Medium **20 cols × 17 rows**, Large **27 cols × 28 rows**.

### 4.1 Load-time transform (per size)
1. Every `0` → `DOT (2)`, except the cells overwritten below.
2. **Pac spawn** → `PAC_SPAWN (8)` (treated as empty, no dot). Spawns:
   - Small `(7, 13)`, Medium `(9, 15)`, Large `(13, 23)`.
3. **Ghost house** carved from the central open chamber (set interior `0`→`HOUSE (4)`, door `→ GHOST_DOOR (5)`, spawn slots `→ GHOST_SPAWN (9)`):
   - **Small (15×15):** chamber is tight; use a 1×2 pen at `(7,8),(7,9)` as `HOUSE`, spawn slot `(7,9)`→`9`, door `(7,7)`→`5`, exit tile `(7,6)`.
   - **Medium (20×17):** open band rows 7–9 around cols 8–10. House `(8,8),(9,8),(10,8)`→`4`, spawns `(8,8),(9,8),(10,8)` reused as the 3 occupied slots `→9`; door `(9,7)`→`5`; exit `(9,6)`.
   - **Large (27×28):** big chamber rows 9–13, cols 9–17. House interior cols **11–15**, rows **10–12** → `4`; spawn slots `(11,11),(13,11),(15,11),(13,12)` → `9`; door `(13,9)` → `5`; exit tile `(13,8)`.
   (If any chosen cell is a wall, snap to the nearest `0` in that quadrant.)
4. **Power pellets** — set 4 path tiles near the quadrant corners to `POWER (3)` (snap to nearest `0` if a wall):
   - **Small:** `(1,2) (13,2) (1,12) (13,12)`
   - **Medium:** `(1,2) (18,2) (1,14) (18,14)`
   - **Large:** `(1,3) (25,3) (1,23) (25,23)`
5. **Tunnels:**
   - **Large:** native — **row 14** already has `0` at `col 0` and `col 26`. Mark the contiguous edge run on row 14 (cols `0–1` and `25–26`) as `TUNNEL (6)`.
   - **Small/Medium:** no native edge openings. **Synthesize** one: carve `col 0` and `col cols-1` of the central path row to `TUNNEL (6)` — Small **row 9**, Medium **row 9** (both are full-width `0` interior rows). This is a one-time `Board` mutation of its working copy only.
6. `FRUIT_SPOT (7)` = the exit tile one tile below the door (Small `(7,6)`→ no, that's exit above; place fruit at pac-spawn-adjacent open tile): Small `(7,11)`, Medium `(9,9)`, Large `(13,17)`. Fruit only renders/collides while active (§6.4); the tile is otherwise a normal walkable empty.

`Board` tracks `dotsRemaining` (DOT+POWER count) live; win = `dotsRemaining == 0`. No per-move grid scan.

---

## 5. GAME LOOP + MOVEMENT

### 5.1 Constants (single source)
```
TILE = 24 px            FPS = 60            DT = 1.0/60 s
TIMER_MS = 15           MAX_FRAME = 0.25 s  EPS = 0.5 px
PRETURN = 0.30 tiles    HUD_PX = 48 px
BASE_SPEED_CELLS = 7.5  (cells/sec at "100%")
```

### 5.2 Fixed-timestep loop (EDT only)
One `javax.swing.Timer(TIMER_MS)`; `setCoalesce(true)`. `actionPerformed` runs on the EDT:
```
now = System.nanoTime()
frame = min((now - last)/1e9, MAX_FRAME); last = now
acc += frame
while (acc >= DT) { model.update(DT); acc -= DT }   // ALL logic, EDT only
panel.setInterpolation(acc / DT)                     // render lerp fraction
panel.repaint()
```
`model.update(dt)` advances logic + fires `SoundEngine` cues (fire-and-forget). `paintComponent` only reads state. **No `synchronized`/`volatile` on game state** — the only other thread is the daemon audio executor, which shares nothing.

**[RESOLVED] Loop wakeup = 15 ms timer; logic step = fixed 1/60 s** (accumulator decouples the two). When `PAUSED`, `update` early-returns (loop still repaints the dimmed scene + overlay).

### 5.3 Speeds (cells/sec → px/sec via × TILE; multiplied by per-level factor §6.5)
| Entity / state | base cells/sec | px/sec @TILE=24 (×1.0) |
|---|---|---|
| Pac-Man | 7.5 | 180 |
| Ghost normal | 7.0 | 168 |
| Ghost frightened | 5.0 | 120 |
| Ghost in tunnel | 3.5 | 84 |
| Ghost EATEN (eyes) | 14.0 | 336 |
| Pac-Man during fright | 7.9 | 190 |

(Pac slightly outruns ghosts early; ghosts catch up via the §6.5 multiplier table.)

### 5.4 Smooth grid movement
Each `Entity` is always on a grid line. Per tick:
```
move = speed * dt
while move > 0 and dir != NONE:
    distToCenter = px-distance to next tile center along dir
    if move < distToCenter: advance by move; move = 0
    else:
        advance to center; move -= distToCenter
        atCenter()                 // decision point
        if tile ahead in dir is blocked AND no legal queued turn: dir = NONE; break
```
Pixel position = `lerp(fromCenter, toCenter, progress)`; snap exactly to center on arrival (kills drift).

### 5.5 Turn buffering (Pac-Man)
- Fields `dir` and `desiredDir`. Key press sets `desiredDir` only.
- **Reverse** (`desiredDir == dir.opposite()`): apply immediately (no center needed).
- **Perpendicular**: at a tile center, if tile in `desiredDir` is not blocked → `dir = desiredDir`.
- **Pre-turn lookahead:** within `PRETURN*TILE = 7.2 px` of a center, if `desiredDir` opens at that center, snap onto the perpendicular line early (arcade cornering). `desiredDir` persists until satisfied/overwritten.
- If `dir` leads into a wall at a center and `desiredDir` doesn't open → `dir = NONE`, `desiredDir` stays armed.

### 5.6 Tunnel wrap
On a `TUNNEL` row, when an entity center passes `col 0` going LEFT → `px += cols*TILE` (emerge right), and mirror for RIGHT. Ghosts use tunnel speed while occupying a `TUNNEL` tile; Pac unaffected.

---

## 6. GHOST AI

All ghost decisions are grid-discrete: a ghost picks a new direction **only at a tile center**, using a target tile and a fixed tie-break. Between centers it interpolates (§5.4).

### 6.1 Universal direction rule (at each center)
```
candidates = legal non-wall neighbors, EXCLUDING reverse-of-current-dir,
             EXCLUDING GHOST_DOOR unless mode∈{EATEN,LEAVING_HOUSE}
if mode == FRIGHTENED: pick pseudo-random candidate (§6.6)
else: target = currentTarget(); choose candidate minimizing
      squaredDist(neighborTile, target) = (nc-tc)^2 + (nr-tr)^2
tie-break order: UP > LEFT > DOWN > RIGHT
if no candidate (dead-end): allow reverse.
```
No-reverse is suspended exactly once on every mode transition and on frightened trigger (§6.4) — all active ghosts reverse.

### 6.2 The four personalities
Let Pac tile `(P.c,P.r)`, Pac facing `pdir`, Blinky tile `(B.c,B.r)`. **[RESOLVED] `PINKY_UP_QUIRK = true`** (faithful 1980 up-overflow).
- **Blinky (red):** `target = (P.c, P.r)`.
- **Pinky (pink):** 4 tiles ahead of Pac in `pdir`; if `pdir==UP`, also −4 cols (`c−4, r−4`).
- **Inky (cyan):** let `Q` = 2 tiles ahead of Pac (same up-quirk: 2 up & 2 left if `pdir==UP`); `target = (2*Q.c − B.c, 2*Q.r − B.r)`.
- **Clyde (orange):** `d2=(C.c−P.c)^2+(C.r−P.r)^2`; if `d2 > 64` (>8 tiles) `target = (P.c,P.r)`, else `target = Clyde scatter corner`.

### 6.3 Scatter corners (per actual grid size)
**[RESOLVED] Corrected to real dimensions** (Small 15×15, Medium 20×17, Large 27×28). Off-grid corner math still loops; these in-bounds equivalents keep ghosts circling.

| Ghost (corner) | Small | Medium | Large |
|---|---|---|---|
| Blinky (top-right) | `(13,1)` | `(18,1)` | `(25,1)` |
| Pinky (top-left) | `(1,1)` | `(1,1)` | `(2,1)` |
| Inky (bottom-right) | `(13,13)` | `(18,15)` | `(25,26)` |
| Clyde (bottom-left) | `(1,13)` | `(1,15)` | `(2,26)` |

### 6.4 Mode schedule, forced reverse, frightened
Global `Mode ∈ {SCATTER, CHASE}`; frightened is per-ghost, layered on top. `ModeScheduler` consumes a `(Mode,durationFrames)` list. **Frightened pauses the mode timer.** On every Scatter↔Chase boundary AND on frightened trigger, all active ghosts (not `EATEN`/`IN_HOUSE`) reverse once.

Per-level timeline (seconds; after the list → permanent Chase):
| Level | Phases (S=scatter, C=chase) |
|---|---|
| 1 | 7S 20C 7S 20C 5S 20C 5S ∞C |
| 2–4 | 7S 20C 7S 20C 5S 1033C 1/60S ∞C |
| 5+ | 5S 20C 5S 20C 5S 1037C 1/60S ∞C |

Frightened duration / flashes by level:
| Level | Fright secs | Flashes |
|---|---|---|
| 1 | 6 | 5 |
| 2 | 5 | 5 |
| 3 | 4 | 5 |
| 4 | 3 | 5 |
| 5 | 2 | 5 |
| 6 | 5 | 5 |
| 7–8 | 2 | 5 |
| 9–17 | 1 | 3 |
| 18+ | 0 (pellet still 50 pts + forces reverse) | — |

- **Trigger:** eating POWER sets all non-`EATEN` ghosts `→ FRIGHTENED`, refreshes window to full, **resets eat-chain to 200**, forces reverse.
- **Flash warning:** during the final `flashes` blink cycles, toggle body white↔blue every **0.13 s**.
- **Natural expiry:** ghosts return to global SCATTER/CHASE with **no** reverse.
- **Flee (§6.6):** pseudo-random direction via per-ghost LCG; default faithful (not max-distance).

### 6.5 Per-level speed multipliers (× base cells/sec)
| Level | Pac | Ghost | Ghost-tunnel | Fright-Pac | Fright-Ghost |
|---|---|---|---|---|---|
| 1 | 0.80 | 0.75 | 0.40 | 0.90 | 0.50 |
| 2–4 | 0.90 | 0.85 | 0.45 | 0.95 | 0.55 |
| 5–20 | 1.00 | 0.95 | 0.50 | 1.00 | 0.60 |
| 21+ | 0.90 | 0.95 | 0.50 | — | — |

### 6.6 Frightened RNG (per ghost, deterministic)
```
rngState = rngState*1103515245 + 12345
idx = (rngState >>> 16) % candidates.length   // candidates ordered UP,LEFT,DOWN,RIGHT
```

### 6.7 House exit order & global release
- **Per-ghost dot counters (Level 1):** Blinky starts outside; Pinky 0; Inky 30; Clyde 60. Level 2: Inky 0, Clyde 50. Level 3+: all 0.
- **Global release timer:** if Pac eats no dot for **4 s** (lvl<5) / **3 s** (lvl≥5), force-release next penned ghost in order Pinky→Inky→Clyde.
- Exit: rise to door center → move to exit tile → enter AI in current global mode, facing LEFT.
- **Small board drops Inky** (3 ghosts: Blinky, Pinky, Clyde). Small thresholds: Pinky 0, Clyde 20. Eyes ignore counters (re-exit immediately after `REVIVE_DELAY = 0.5 s` in house).

### 6.8 Cruise Elroy (Blinky speed-up)
At `dotsRemaining ≤ elroy1` → Blinky +5% and always chases; at `≤ elroy2` → +10%. Suspended while any ghost is `IN_HOUSE`.
| Level | elroy1 | elroy2 |
|---|---|---|
| 1 | 20 | 10 |
| 2 | 30 | 15 |
| 3–5 | 40 | 20 |
| 6–8 | 50 | 25 |
| 9–11 | 60 | 30 |
| 12+ | 80 | 40 |
Small/Medium clamp: `elroy ≤ round(totalDots*0.5)`.

### 6.9 Ghost-eat scoring chain
Within one frightened window: `200 → 400 → 800 → 1600` (capped). Chain index resets at the start of each window. On eat: spawn a `ScorePopup` for 1.0 s; Pac freezes `EAT_PAUSE = 30 frames`; eaten ghost `→ EATEN` (eyes), target = exit tile, speed = eyes speed; on reaching house → `IN_HOUSE` for `REVIVE_DELAY`, then `LEAVING_HOUSE`. Eyes are never edible.

---

## 7. RENDERING

### 7.1 `paintComponent` setup
`super.paintComponent`; create a scratch `Graphics2D`; set hints:
`ANTIALIASING=ON`, `STROKE_CONTROL=PURE`, `RENDERING=QUALITY`, `INTERPOLATION=BILINEAR`, `TEXT_ANTIALIASING=ON`. `JPanel` is opaque + double-buffered. Never draw outside `paintComponent`.

### 7.2 Locked draw order
1. background (fill `BG_DEEP` → radial vignette `BG_VIGNETTE→BG_DEEP` → fill maze rect `PLAYFIELD_INK` → faint grid lines alpha 40)
2. `mazeCache` (the cached static wall+glow `BufferedImage`)
3. pellets (static dots) + power pellets (pulse/blink)
4. fruit (if active)
5. score popups + death particles
6. Pac-Man (interpolated)
7. ghosts (eaten rendered eyes-only, drawn last so they read on top)
8. HUD (`Hud.draw`)
9. scanline overlay (faint horizontal lines `BG_GRID_LINE` alpha 18 every 3 px — cached)
10. active overlay (`Overlays.draw`: READY/PAUSED/GAME OVER/level-clear)

### 7.3 Coordinate system + resize
```
tileSize = max(8, min(getWidth()/cols, (getHeight()-HUD_PX)/rows))   // integer, square cells
boardW = tileSize*cols; boardH = tileSize*rows
offX = (getWidth()-boardW)/2
offY = HUD_PX + (getHeight()-HUD_PX-boardH)/2
entityCenterX = offX + (tileX + 0.5 + dir.dx*progress)*tileSize   // + sub-step interpolation
```
A `ComponentListener#componentResized` recomputes scale and calls `rebuildMazeCache()`. `JPanel` preferred size = `cols*TILE × (rows*TILE + HUD_PX)`.

### 7.4 Maze `BufferedImage` cache (per level/resize)
`TYPE_INT_ARGB`, size `boardW×boardH`, same hints. Walls drawn as **centered neon tube segments along corridor contours**, not filled boxes:
- Build `Path2D` of wall-segment midlines inset `TILE*0.28` from tile edges; round corners with `quadTo`, control point at the corner vertex, endpoints pulled back `cornerRadius = TILE*0.5`; `BasicStroke.JOIN_ROUND`.
- Stroke the same path back-to-front (layered alpha bloom over `PLAYFIELD_INK`):
  1. `WALL_GLOW_SOFT` a28, `BasicStroke(TILE*0.85)`
  2. `WALL_GLOW` a55, `BasicStroke(TILE*0.55)`
  3. `WALL_GLOW` a90, `BasicStroke(TILE*0.34)`
  4. `WALL_EDGE` a255, `BasicStroke(TILE*0.30)` (dark tube)
  5. `WALL_CORE` a255, `BasicStroke(TILE*0.12)` (bright filament)
- Ghost door = `GATE` bar `BasicStroke(TILE*0.10)` spanning 2 tiles with an alpha-40 glow underline.
Rebuilt only on resize/new level. Level-complete flash re-tints layers 4–5 white (§9.5).

### 7.5 Sprite geometry (numbers to use; all fractions of `TILE`)
**Pac-Man:** `R = TILE*0.46`. Glow: `RadialGradientPaint(0,0,R*1.8,{0,0.6,1},{PAC_GLOW a140, PAC_GLOW a40, transparent})`. Body: `Arc2D.PIE`, mouth half-angle `m = MOUTH_MAX*0.5*(1+sin(phase))` clamped ≥4°, `MOUTH_MAX=55°`; `startAngle=facingDeg+m`, `extent=360−2m`; facing R=0 U=90 L=180 D=270. Fill `RadialGradientPaint(-R*0.3,-R*0.3,R*1.3,{PAC_HI,PAC})`. Chomp `CHOMP_SPEED = 2π*5.0` (5/s); freeze when stopped.

**Ghost (alive):** body `Path2D`, width `W=TILE*0.92`, dome radius `R=W/2`, top at `-R`, sides down to `y=R*0.55`, scalloped skirt (3 bumps small scale, 4 large) via `quadTo` to `y≈R*0.95`; skirt phase wobbles `±TILE*0.06*sin(t*8)`. Fill `RadialGradientPaint(0,-R*0.3,R*1.4,{lighten(body,0.25),body})`; glow halo radius `R*1.7` alpha 130→0. Eyes: two ellipses `ew=TILE*0.26 × eh=TILE*0.32` at `(±TILE*0.20, -R*0.15)`; pupils `PUPIL` r=`TILE*0.12` offset `TILE*0.09*(dirX,dirY)`.

**Frightened:** body `FRIGHT_BODY`+`FRIGHT_GLOW`; eyes = two `FRIGHT_FACE` dots r=`TILE*0.10`; zig-zag mouth polyline 6–7 pts alternating `y=R*0.35`/`R*0.52` across `x∈[−W*0.34,W*0.34]`, `BasicStroke(TILE*0.08,ROUND,ROUND)`. Flash white/red when warning.

**Eaten:** eyes only (sclera+pupils aimed at door) + faint `EYE_WHITE` a50 halo.

**Pellet:** core r=`TILE*0.10` `PELLET` + halo `RadialGradientPaint` r=`TILE*0.22` `PELLET_GLOW a90→transparent` (static).
**Power pellet:** core r=`TILE*0.26` `POWER_PELLET` + halo r=`TILE*0.55` `POWER_GLOW a160→transparent`; scale/alpha `×(1+0.18*sin(t*5.5))`; blink toggle every 250 ms but never below core alpha 120.
**Fruit:** procedural per `FruitType` from circles + bezier stems, glow halo like power pellet.

### 7.6 Animation timing
Death (~1100 ms): pause 250 ms → spin+shrink 700 ms (mouth half-angle 0→180°, scale 1.0→0.85, rotate ~30°) → 150 ms pop of 6–8 `PAC` particles r 0→`TILE*1.5`, alpha 255→0. Score popups rise `TILE*0.6` over 800 ms, alpha 255→0 last 300 ms, scale 0.8→1.1→1.0. All oscillators read one shared `double timeMs` advanced in the loop (pause-clean, frame-rate independent).

---

## 8. AUDIO

All SFX synthesized PCM (`AudioFormatSpec`: 44100 Hz, 16-bit, mono, signed LE). `GameModel.update` calls `sound.play(Cue)` / `startSiren` / `stopSiren` fire-and-forget on the EDT; a single daemon executor serializes all `SourceDataLine` writes — no concurrent line access, shares no game state (`muted` is an `AtomicBoolean`).

### 8.1 Cue catalog
| Cue / loop | Synthesis | Trigger |
|---|---|---|
| `CHOMP_A` / `CHOMP_B` | square blip A 220→160 Hz, B 160→120 Hz, ~60 ms, alternate per dot | dot eaten |
| `SIREN` (loop) | wobbling square 380–520 Hz, seamless loop, pitch by `SirenLevel` (dots remaining) | while PLAYING |
| `FRIGHT_SIREN` (loop) | faster low warble square 120–180 Hz | frightened window |
| `EAT_GHOST` | 8-step up arpeggio square 200→1200 Hz, ~300 ms | vulnerable ghost eaten |
| `GHOST_RETREAT` | descending sine + tremolo | ghost → EATEN |
| `DEATH` | descending sweep square 600→80 Hz + noise tail, ~1.2 s | Pac caught |
| `EXTRA_LIFE` | two-note triangle jingle C6→E6 | lives++ |
| `INTRO` | 4-bar arpeggio square lead | entering READY |
| `FRUIT` | sine two-note ding-dong | fruit eaten |
| `MENU_MOVE` / `MENU_SELECT` | short square blips | menu navigation |

### 8.2 `SoundEngine` API
```java
void play(Cue cue);            // fire-and-forget one-shot
void startSiren(SirenLevel l); // long-lived loop line
void stopSiren();
void setMuted(boolean m);      // AtomicBoolean; if m → stopSiren()
boolean isMuted();
void shutdown();               // stopSiren + executor.shutdownNow + close lines
```
`M` key + a menu Mute toggle drive `setMuted`. `App.quit()`/screen teardown calls `shutdown()`.

---

## 9. UI / SCREENS

### 9.1 HUD (top bar height `≈1.6*TILE`, within `HUD_PX=48`)
Left→right baseline-aligned: `SCORE` label (`TEXT_DIM`) + value (`TEXT_PRIMARY` neon); `HIGH` centered (`GOLD` glow); `LEVEL` right + tiny fruit icon. Lives: bottom-left as mini Pac-Men (r=`TILE*0.35`, static 30° mouth facing left), one per remaining life, spacing `TILE*0.9`, cap 5. Power-up timer bar (when frightened): bottom-center `RoundRectangle2D` width `6*TILE`, height `TILE*0.3`, fill `ACCENT_CYAN` draining L→R over a dim `TEXT_DIM` track; turns `FRIGHT_FLASH` + pulses in the last 2 s.

### 9.2 Main menu
Dark vignette + faint scrolling maze silhouette (alpha 30). Animated neon "PAC-MAN" title (vertical bob ±4 px, glow + 1px cyan/magenta chromatic split). Attract chase motif: looping Pac chasing pellets with 4 trailing ghosts; occasionally flips to frightened ghosts fleeing. Neon pill `MenuButton`s: `PLAY`, `BOARD SIZE: SMALL/MEDIUM/LARGE` (3 segmented chips, active lit `ACCENT_CYAN`), `HIGH SCORES`, `MUTE`, `QUIT`. Idle outline `WALL_CORE a120`/text `TEXT_DIM`; hover outline `ACCENT_MAGENTA`/text `TEXT_PRIMARY`/scale 1.03 over 120 ms; keyboard-focused fill `ACCENT_CYAN a25` + chomping `►` marker. Mouse + Up/Down/Enter both work.

### 9.3 High scores
Neon title "HIGH SCORES". Monospaced columns `RANK  NAME  SCORE`, top 10 desc from `HighScoreManager`. Ranks 1–3 in glowing rings `GOLD`/`SILVER`/`BRONZE`; 4–10 `TEXT_PRIMARY`/`TEXT_DIM` alternating row alpha. New entry row highlighted `ACCENT_MAGENTA` with blinking caret. "Back" routes via `App`.

### 9.4 Overlays (over `BG_DEEP` a160 scrim; game keeps rendering dimmed)
- **READY!** `READY_YELLOW`, below house, scale pulse 1.0↔1.06 at 2 Hz, ~`READY=1.5 s`.
- **PAUSED** `TEXT_PRIMARY` neon + `TEXT_DIM` "P resume / Q menu", faint cyan/magenta split.
- **GAME OVER** `GAMEOVER_RED` huge, drops in `−40px→0` ease-out 350 ms then flicker; below: final score + "ENTER" → name-entry if qualifying, else menu.
- **Name entry** (in GAME OVER overlay): blinking caret field; `App.submitScoreAndShow(name,score)` → `addScore` → `show("SCORES")`.

### 9.5 Level-complete flash
On `dotsRemaining==0`: freeze input, hide ghosts, flash maze (re-tint cached layers 4–5 between `WALL_CORE` and `FRIGHT_FLASH`) 4× at 220 ms; fade to black 400 ms; `model.nextLevel()` + `rebuildMazeCache()`.

---

## 10. SCORING / PROGRESSION / CONTROLS

### 10.1 Scoring
| Item | Points |
|---|---|
| Dot | 10 |
| Power pellet | 50 |
| Ghost chain | 200 / 400 / 800 / 1600 |
| Fruit | per §10.3 |

### 10.2 Lives & progression
`START_LIVES = 3` (HUD shows 2 extra icons at start). `MAX_LIVES = 5`. `EXTRA_LIFE_SCORE = 10000` (one-time; awards above cap dropped; `MULTI_EXTRA=false`). Death = non-frightened/non-eaten ghost overlaps Pac (`<0.5` tile) → freeze, death anim `DEATH_ANIM=1.5 s`, `lives--`. If lives>0: respawn all at spawns, reset mode timer to phase 0, `READY=1.5 s`, resume. Else GAME OVER → high-score flow → menu. No final level (perpetual escalation).

### 10.3 Fruit
Appears at `FRUIT_SPOT` twice/level: after `round(totalDots*0.30)` and `round(totalDots*0.73)` dots; lingers `FRUIT_TTL = randf(9,10) s`.
| Level | Fruit | Points |
|---|---|---|
| 1 | Cherry | 100 |
| 2 | Strawberry | 300 |
| 3–4 | Orange | 500 |
| 5–6 | Apple | 700 |
| 7–8 | Melon | 1000 |
| 9–10 | Galaxian | 2000 |
| 11–12 | Bell | 3000 |
| 13+ | Key | 5000 |

### 10.4 Controls (`InputMap`/`ActionMap`, `WHEN_IN_FOCUSED_WINDOW` — never raw `KeyListener`)
| Action | Keys |
|---|---|
| Up / Down / Left / Right | Arrows / W A S D (set `desiredDir` only) |
| Pause / resume | P |
| Mute | M |
| Back to menu (from pause/overlay) | Q / Backspace |
| Confirm / dismiss overlay | Enter / Space |
| Restart current game | R |

### 10.5 Board-size mapping
| Option | Map | Grid (cols×rows) | Ghosts | Power pellets | Tunnel | Pen |
|---|---|---|---|---|---|---|
| Small | `SMALL_MAP` | 15×15 | 3 (no Inky) | 4 | synth row 9 | 1×2 |
| Medium | `MEDIUM_MAP` | 20×17 | 4 | 4 | synth row 9 | 3×1 |
| Large | `LARGE_MAP` | 27×28 | 4 | 4 | native row 14 | 5×3 chamber |

### 10.6 Constants summary
```
TILE=24 FPS=60 DT=1/60 TIMER_MS=15 MAX_FRAME=0.25 EPS=0.5 PRETURN=0.30 HUD_PX=48
BASE_SPEED_CELLS=7.5  START_LIVES=3 MAX_LIVES=5 EXTRA_LIFE_SCORE=10000
DOT=10 POWER=50 GHOST_CHAIN={200,400,800,1600}
EAT_PAUSE=30f REVIVE_DELAY=0.5s DEATH_ANIM=1.5s READY=1.5s MAZE_FLASH=2.0s
GLOBAL_RELEASE_TIMEOUT=4s(lvl<5)/3s(lvl>=5)  FRUIT_TTL=9..10s
FRUIT_THRESHOLDS=round(totalDots*0.30), round(totalDots*0.73)
PINKY_UP_QUIRK=true  CLYDE_RADIUS2=64  TIE_BREAK=UP>LEFT>DOWN>RIGHT
NO_REVERSE=true (except mode-transition & frightened-trigger forced reverse)
```

---

## 11. BUILD ORDER (incremental-compile checklist)

1. **Foundation**
   - [ ] `assets/Palette.java`, `assets/Fonts.java`
   - [ ] `characters/Direction.java`
   - [ ] `game/TileType.java`, `game/BoardSizeOption.java`
   - [ ] `game/Board.java` (load-time map transform §4.1)
   - [ ] Delete PNGs; replace `assets/Assets.java` with facade
   - [ ] `highscores/HighScoreEntry.java` (+`serialVersionUID`, `getName`), `HighScoreManager.java` (+`qualifies`, handled logging)
2. **Window shell**
   - [ ] `menu/App.java` (JFrame + CardLayout), `menu/MenuButton.java`
   - [ ] `menu/MainMenu.java` (JFrame→JPanel stub), `menu/HighScoresScreen.java` (stub)
   - [ ] `Main.java` one-liner → `new App().show("MENU")`
   - [ ] App navigates between empty screens; delete `highscores/HighScore.java`
3. **Model + loop (headless-testable rules)**
   - [ ] `characters/Entity.java`, `characters/Pacman.java`
   - [ ] `characters/GhostMode.java`, `characters/GhostPersonality.java`, `characters/Ghost.java`
   - [ ] `game/LevelConfig.java`, `game/LevelConfigFactory.java`, `game/FruitType.java`
   - [ ] `game/ModeScheduler.java`, `game/GamePhase.java`, `game/ScorePopup.java`, `game/Particle.java`
   - [ ] `game/GameModel.java`, `game/GameLoop.java`, `game/Game.java` (factory)
   - [ ] Delete `game/GameLogic.java`, `game/GameState.java`; delete `characters/UpgradeGhost.java`, `UpgradeType.java`
4. **Rendering**
   - [ ] `assets/SpritePainter.java`
   - [ ] `game/GamePanel.java` (hints, coord/scale, maze cache, draw order)
   - [ ] `game/Hud.java`, `game/Overlays.java`, `game/GameScreen.java`
   - [ ] Delete `game/GameUI.java`; smooth motion + neon maze visible
5. **Input** — InputMap/ActionMap bindings + turn buffering wired in `GamePanel`
6. **Audio** — `assets/AudioFormatSpec.java`, `assets/Synth.java`, `assets/Cue.java`, `assets/SoundEngine.java`; wire cues from `GameModel.update`; `M` mute
7. **Polish** — menu attract animation, overlays, score popups, frightened flash, fruit, high-score capture flow, scanline/vignette, optional screen shake

### Invariants
- All game state mutated only inside `GameLoop.tick → GameModel.update`, on the EDT. No `synchronized`/`volatile` on game state.
- Grid is authoritative; pixels derived each frame → resolution-independent, smooth, resize-safe.
- Static maze cached once per level/resize in a `BufferedImage`.
- One JFrame + CardLayout; timer + audio always stopped before a screen is hidden.
- `highscores.ser` stays byte-compatible (`HighScoreEntry` field shape frozen).
