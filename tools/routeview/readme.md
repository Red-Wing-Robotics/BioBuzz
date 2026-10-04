# Route View

A live map and editor for the JSON autonomous routes that pushes every saved route to the
Control Hub, so a route change reaches the robot without a rebuild or redeploy.

```
./gradlew -p tools/routeview run            # from the repo root; then open http://127.0.0.1:8765
./gradlew -p tools/routeview run --args="--open --hub 192.168.43.1:5555"
```

Flags: `--port N` (default 8765) · `--hub SERIAL` (default `192.168.43.1:5555`, the Control Hub
over Wi-Fi; a USB serial from `adb devices` also works) · `--no-push` (start with push-on-save
off) · `--open` (open the page in a browser).

## The loop

1. Edit `TeamCode/src/main/assets/autos/NAME.json`, either in the page's editor (**Edit** on a
   route) or in any other editor, and save.
2. Within ~0.2 s Route View checks the file the way the robot does at INIT and redraws the map.
   If the robot would accept it, Route View pushes it to `/sdcard/FIRST/autos/NAME.json`, which
   overrides the APK's copy. A file the robot would reject is **not** pushed; the error, with its
   path (`routine[2].follow.line[1]: unknown waypoint …`), shows on the page and in the console
   with a terminal bell.
3. INIT the auto. The Driver Station line `route: NAME — /sdcard/FIRST/autos/NAME.json · 3610a686`
   ends in the file's checksum. It should match the `#3610a686` Route View printed for the push.

**Connecting to the hub:** Route View does `adb connect 192.168.43.1:5555` for you, using the
`adb` in your Android SDK (`sdk.dir` in local.properties). It retries every 2 s while the hub is
away, and **Connect** in the header tries right now and shows adb's answer. **Disconnect** runs
`adb disconnect` and stops the retries until you click Connect again. Saves made while
disconnected are queued and pushed as soon as the hub is connected.

## The editor

**Edit** opens the file beside its map. As you type, the text is checked by the robot's own parser
and compiler, not a separate JSON Schema, so it catches what INIT would: bad JSON, unknown keys and
step kinds, wrong types, waypoints that don't exist. The error's line is highlighted, and the map
previews your unsaved changes (dimmed while there's an error).

- **Save** (⌘S / Ctrl+S) is refused until the robot would accept the file. A save is pushed like
  any other.
- If the file changes on disk while you edit (VS Code, git), an unedited draft follows it; an edited
  one asks whether to load the disk version or keep yours. Line endings are kept as they were.
- **Insert…** adds a step, waypoint or heading template at the cursor. *Route file reference*
  under the editor lists the format. Tab indents; Esc leaves the editor.
- It edits existing routes only. A new route also needs an OpMode in `opmodes/auto/`, so create
  the file in the repo.

## The page

- **Routes:** each file's check result and its state on the hub: *on hub ✓*, *hub has a different
  version*, *not on hub* (the robot runs the APK copy), *only on the hub*, or *hub ?* (not
  connected).
- **Map:** the same drawing, points estimate, time estimate and rule checks as `docs/autos.pdf`, for
  red and blue. It flashes when a save changes it.
- **Connect / Disconnect:** see above. The banner under the header shows adb's last answer, e.g.
  *no answer from 192.168.43.1:5555. Is this laptop on the robot's Wi-Fi?*
- **Push to hub / Push all / Remove from hub / Re-read hub:** for syncing by hand, e.g. at an
  event. *Remove* deletes the hub's override so the robot goes back to the APK copy. After an APK
  update, remove overrides you don't want, or the hub keeps running them.

Warnings (yellow) don't block a push. The usual one is an `action`/`waitUntil` name that no
`.action("…")` / `.condition("…")` in TeamCode registers. That's a text search, so a name built at
runtime gets a false warning, but a real typo fails INIT on the robot.

## How it works

- `tools/routeview` is its own Gradle build (its own `settings.gradle`), so the SDK's gradle
  files stay untouched. It compiles TeamCode's route engine (`auto/`, `auto/route/`) and the
  auto-maps code (`TeamCode/src/test/.../automaps/`) **from TeamCode's source tree**: the list is
  `TEAMCODE_SOURCES` in `build.gradle`. Those files must stay free of Android/SDK types;
  `src/stubs` stands in for the one they mention (`RobotHardware`). The compile-check hook builds
  Route View whenever one of them changes.
- `FieldPainter` draws onto a `Canvas`. `Pdf.Page` and `SvgCanvas` both implement it, so the
  PDF and the page come from the same code.
- `RouteView` holds the watcher (polls file times every 200 ms), the hub thread (every adb call,
  in order), and the state the page long-polls (`WebServer`). Pushes send exactly the bytes that
  were checked. The editor's saves go through the same file and watcher: `/api/check` checks a
  draft, `/api/save` checks again, compares the checksum the draft started from, and swaps the
  file in atomically.
- The server listens on 127.0.0.1 only, never the robot's Wi-Fi. It refuses requests addressed to
  another host name (DNS rebinding) and POSTs from other sites, because whoever can reach it can
  write to the robot.
- No CDN: the page is one self-contained file (`src/main/resources/routeview/index.html`), since
  the robot's Wi-Fi has no internet.

```
./gradlew -p tools/routeview test           # unit tests (run in the repo root)
```

Nothing here can push without a Control Hub, so adb behaviour is only verified on the robot.
