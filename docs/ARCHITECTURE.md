# Architecture

## The one rule

`storm-core` never imports `net.minecraft`. If a feature needs something from
the game, the interface for it goes into `xyz.stormclient.bridge` first and the
bridges implement it. That rule is what makes the client portable and what makes
the headless smoke test possible.

## Boot sequence

```
launcher / mod loader
        |
        v
StormAgent.premain|agentmain          (storm-agent)
        |  detects the version, appends the bridge jar
        v
BridgeLoader.load(version)
        |  reflection: xyz.stormclient.bridge.mc189.StormBridge189#install
        v
StormBoot.boot(new Mc189Minecraft(mc))
        |  Bridge.install(impl)
        v
Storm.get().start()
        |  modules, commands, listeners, config
        v
running
```

## Packages

| Package | Contents |
|---|---|
| `bridge` | The interfaces every version implements, plus `Bridge` and `GameVersion` |
| `event` | `EventBus`, `@Subscribe`, priorities, and every event type |
| `module` | `Module`, `ModuleManager`, `Category`, and all features under `impl` |
| `setting` | Boolean, number, mode, colour, keybind, string, group |
| `config` | Profile based JSON storage |
| `command` | Chat commands behind a configurable prefix |
| `ui` | Theme, click GUI, HUD editor, notifications |
| `rotation` | `RotationManager` owns every server side rotation |
| `target` | Shared target selection for combat, ESP and the target HUD |
| `social` | Friends |
| `util` | Maths, colours, timers, animations, JSON, logging |

## Event bus

Reflection based registration, cached per listener class, sorted by priority and
posted through a copy on write list so a module can toggle itself from inside a
handler. A handler that throws is reported and its module is switched off rather
than taking the frame down with it.

```java
@Subscribe(priority = Priority.HIGH)
public void onMotion(MotionEvent event) {
    if (event.isPre()) event.setYaw(42F);
}
```

## Rotations

Modules never write to the player's view directly. They call

```java
Storm.get().rotations().request(rotation, priority, ticks, yawStep, pitchStep, silent);
```

and `RotationManager` interpolates, applies the sensitivity grid the vanilla
mouse handler would produce, keeps the rotation alive for a few ticks and then
hands control back. Two modules asking at once is decided by priority instead of
by whichever one happens to run last.

## Settings and configs

A setting knows its name, its bounds, how to serialise itself and when it should
be visible. That is enough for the GUI to draw it and for the config writer to
store it, so adding a setting never means touching either.

```java
private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.05).suffix("m"));
private final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));

// shown only when it matters
maxTargets.visibleWhen(() -> mode.is("Multi"));
```

Configs land in `.minecraft/storm/configs/<name>.json`.

## Rendering

`IRenderer` is the whole drawing surface: rectangles, rounded rectangles,
gradients, circles, arcs, shadows, scissors, textures, 3D boxes, 3D lines and a
world to screen projection. The 1.8.9 bridge implements it on fixed function GL,
a modern bridge can implement the same calls on core profile without the UI
noticing.

Text goes through `IFontRenderer`. The 1.8.9 bridge ships `StormFontRenderer`,
which bakes a TrueType font into one atlas at load time and falls back to the
vanilla font when no font file is present.
