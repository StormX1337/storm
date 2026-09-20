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

## Building

```bash
./gradlew build            # core, agent and launcher
./gradlew :storm-core:smokeTest
```

The 1.8.9 bridge is skipped unless a Minecraft workspace is present, because it
needs deobfuscated game classes:

```bash
./gradlew -Pstorm.mc=true :storm-bridge-1.8.9:build
```

Set that workspace up with ForgeGradle (`1.8.9-11.15.1.2318`) or drop a
deobfuscated jar into `storm-bridge-1.8.9/libs/`.

## Running

```bash
java -jar storm-launcher.jar
```

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

## A word on where you use it

Most servers forbid clients like this, and many of these features will get an
account banned. Use Storm on your own server, in single player, or anywhere the
rules actually allow it. That is your call to make, not the code's.
