<p align="center">
  <img src="assets/storm-logo.svg" width="110" alt="Storm">
</p>

<h1 align="center">Storm Client</h1>

<p align="center">
  A Minecraft PvP client written from scratch &mdash; own event bus, own module
  system, own rendering layer, own interface, own assets.
</p>

---

## What this is

Storm is a complete client project under the package `xyz.stormclient`:

| Module | What it does | Compiles standalone |
|---|---|---|
| `storm-core` | The whole client: events, modules, settings, GUI, HUD, configs, commands | yes, Java 8, no dependencies |
| `storm-agent` | JVM agent for `premain` and runtime attach | yes, Java 8 |
| `storm-launcher` | Frameless launcher with version picker and injection menu | yes, Java 17 |
| `storm-bridge-1.8.9` | The only module that imports `net.minecraft.*` | needs a Forge/MCP workspace |

Everything is original code. No source, asset, logo or file from any other
client is used anywhere in this repository, and nothing here touches licence
checks, authentication or DRM.

## The idea behind the layout

`storm-core` contains **no Minecraft import at all**. Every call into the game
goes through the interfaces in `xyz.stormclient.bridge`:

```
Module  ->  Bridge.mc()  ->  IMinecraft / IPlayer / IWorld / IRenderer / ...
                                        |
                        +---------------+---------------+
                        |               |               |
                 bridge-1.8.9    bridge-1.12.2     bridge-modern
```

Two things fall out of that:

* **One client, several versions.** A new version means one new bridge, never a
  new fork of the client.
* **The client is testable without Minecraft.** `storm-core/src/test` contains a
  fake game the whole client boots against:

```
$ ./gradlew :storm-core:smokeTest

[Storm/INFO] Storm Client 1.0.0 starting on 1.8.9
[Storm/INFO] registered 68 modules
[Storm/INFO] registered 12 commands
  ok   KillAura picked a target
  ok   KillAura attacked
  ok   rotations were sent to the server
  ...
Storm Client smoke test: 21/21 checks passed
```

## Features

**Combat** KillAura (single / switch / multi, silent, lock or smooth rotations,
FOV limit, randomisation, raytrace, keep sprint), AutoClicker with click
patterns, Criticals, Reach, Velocity, AntiBot with a voting heuristic,
HitBoxes, AutoBlock, TriggerBot, AimAssist, WTap.

**Movement** Sprint, Speed, NoSlow, Fly, Step, LongJump, InvMove, Jesus, NoWeb,
Sneak, AntiVoid, Spider.

**Player** AutoTool, ChestStealer, InvManager, FastPlace, NoFall, Blink,
Freecam, AutoRespawn, Scaffold, AutoArmor, FastBreak.

**Render** ESP (2D / box / outline / corners, health bars, friend colours),
Tracers, Chams, Nametags, Fullbright, NoHurtCam, ViewClip, Animations with
presets, Trajectories, BlockHighlight, FOV, Zoom, Shaders, ClickGUI.

**HUD** Watermark and module list, Keystrokes with CPS, TargetHUD, ArmorHUD,
PotionHUD, Crosshair, Scoreboard, BlockCounter, plus a drag and drop HUD editor
with snapping.

**World / Misc** Timer, Nuker, AutoSign, BedProtect, AutoGG, AntiAFK,
ChatSuffix, NameProtect, DiscordRPC, AutoReconnect, Notifier.

Every module carries its own settings (toggle, slider, dropdown, colour picker
with rainbow, keybind, text field), and everything is saved into named config
profiles as readable JSON.

### Interface

* **ClickGUI** (`RSHIFT`) &mdash; one draggable window per category, live search,
  animated toggles, inline colour pickers, five themes, one accent colour drives
  the whole palette.
* **HUD editor** (`H`) &mdash; drag elements, scroll to scale, snapping to edges
  and to the screen centre.
* **Commands** (`.help`) &mdash; toggle, bind, config, friend, hud, theme, panic,
  prefix, say, reset, info.

## Getting the code

```bash
git clone -b claude/charming-archimedes-400dsx https://github.com/StormX1337/storm.git
cd storm
```

From then on `git pull` is all it takes. Without git installed
(`winget install Git.Git` fixes that), `update.ps1` downloads the current
branch and replaces the sources in place, leaving `dist\` and `build-out\`
alone:

```powershell
.\update.ps1 build
```

## Building

You need a **JDK 17 or newer**. Two ways, pick either.

**Without Gradle** &mdash; one script, no downloads, no wrapper, no network:

```bash
./build.sh test            # Linux / macOS
.\build.ps1 test           # Windows PowerShell
```

It writes `dist/storm-agent.jar` and `dist/storm-launcher.jar`, ready to use,
and `test` also runs the smoke test.

**With Gradle:**

```bash
./gradlew build            # core, agent and launcher
./gradlew :storm-core:smokeTest
```

The 1.8.9 bridge is not part of either build, because it needs deobfuscated
game classes and a toolchain old enough to produce them. It has its own
self contained workspace:

```powershell
cd storm-bridge-1.8.9\workspace
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_504"   # ForgeGradle 2 needs Java 8
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat build
```

Full procedure, including installing Forge and what works without the mixins:
[docs/BRIDGE-BUILD.md](docs/BRIDGE-BUILD.md).

## Running

```bash
cd dist
java -jar storm-launcher.jar
```

Start it with a **JDK**, not a JRE. Without an attach provider the injection
page cannot work, and the launcher says so in the top right corner.

The launcher has two ways in:

**Play** &mdash; starts an installed version directly with
`-javaagent:storm-agent.jar` already on the command line. Offline session only;
Storm never asks for, stores or sends account credentials.

**Inject** &mdash; scans for running JVMs and attaches the agent to one through
the Attach API. Needs a JDK, and on Java 21+ the game has to be started with
`-XX:+EnableDynamicAgentLoading`.

There is also a button that writes a `Storm <version>` profile into the official
launcher, for anyone who wants to keep using a real Mojang session.

Installing Storm as a plain Forge mod works too &mdash; `StormMod` calls the same
`StormBridge189.install()`.

## Version support

| Version | State | Loader |
|---|---|---|
| 1.8.9 | full, primary target | Forge |
| 1.7.10 | bridge in beta | Forge |
| 1.12.2 | bridge in beta | Forge |
| 1.16.5 | bridge in beta | Fabric |
| 1.18.2 &ndash; 1.21.4 | planned, launcher already lists them | Fabric |

Adding one is described in [docs/BRIDGE.md](docs/BRIDGE.md).

## Documentation

* [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) &mdash; how the pieces fit together
* [docs/BRIDGE.md](docs/BRIDGE.md) &mdash; writing a bridge for another version
* [docs/BRIDGE-BUILD.md](docs/BRIDGE-BUILD.md) &mdash; building the 1.8.9 bridge

## Troubleshooting

**`Cannot find JAR 'kotlin-compiler-embeddable-...jar' required by module
'gradle-declarative-dsl-core'`**

The Gradle wrapper distribution downloaded only partially. Delete it and let the
wrapper fetch it again:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14.3-bin"
.\gradlew build
```

```bash
rm -rf ~/.gradle/wrapper/dists/gradle-8.14.3-bin
./gradlew build
```

Or skip Gradle entirely and use `build.ps1` / `build.sh`.

**`javac 1.8.0_...`, or a stack trace ending in `Method.java:498`**

Both mean a JDK 8. Storm needs JDK 17 or newer. `build.ps1` searches the usual
install locations and picks the newest JDK it finds, so often there is already
one on the machine and nothing needs doing. If it reports nothing recent:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Then open a **new** terminal, or point the current one at it:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
.\build.ps1 test
```

**`error: Invalid filename: ?F:\...`**

An older `build.ps1`. The source list was written with a byte order mark, which
javac read as part of the first file name. Pull and run it again.

**The launcher says "no attach API"**

It is running on a JRE. Start it with the `java` from a JDK:

```bash
"$JAVA_HOME/bin/java" -jar dist/storm-launcher.jar
```

**Injecting fails with "the target JVM does not allow attaching"**

Java 21 and newer refuse dynamic agents by default. Start the game with
`-XX:+EnableDynamicAgentLoading`, which the launcher's own *Play* path already
does.

**The agent attaches but nothing happens in game**

Expected until the 1.8.9 bridge is built. The agent logs
`bridge xyz.stormclient.bridge.mc189.StormBridge189 is not on the class path`.
See the building section above.

## A word on where you use it

Most servers forbid clients like this, and many of these features will get an
account banned. Use Storm on your own server, in single player, or anywhere the
rules actually allow it. That is your call to make, not the code's.
