# Writing a bridge

A bridge is the only place that may import `net.minecraft`. It implements the
interfaces in `xyz.stormclient.bridge` and posts Storm events.

## 1. Module

```
storm-bridge-<version>/
  build.gradle                 # depends on :storm-core plus the game
  src/main/java/xyz/stormclient/bridge/<id>/
```

Register the class name in `BridgeLoader#bridgeClass` so the agent can find it.

## 2. Entry point

One static method, nothing else:

```java
public static void install() {
    StormBoot.boot(new McXXXMinecraft(Minecraft.getInstance()));
    // register your event hooks here
}
```

## 3. What to implement

| Interface | Notes |
|---|---|
| `IMinecraft` | Root handle. `version()` decides how modules branch. |
| `IPlayer` / `IEntity` | Read everything, write only on the local player. |
| `IWorld` | Blocks by coordinate, entity lists, a raytrace. |
| `IRenderer` | The whole drawing surface. The largest piece of work. |
| `IFontRenderer` | Reuse `StormFontRenderer` if your version has a GL texture path. |
| `IInventory` / `IItemStack` | Map your items onto `ItemType`. |
| `INetwork` / `IPacket` | Map your packets onto `PacketType`, expose fields by name. |
| `IInput`, `IChat`, `ISoundEngine`, `IGuiBridge` | Small and mechanical. |

## 4. Events

Post these and most modules work:

| Event | Where it comes from |
|---|---|
| `TickEvent.Pre/Post` | client tick |
| `RenderEvent.Hud` / `RenderEvent.World` | overlay and world render passes |
| `MotionEvent` pre/post | the outgoing movement packet, or a hook in the walk update |
| `PacketEvent.Send/Receive` | the network pipeline |
| `KeyEvent`, `MouseEvent` | input |
| `ChatEvent.Send/Receive` | chat |
| `WorldEvent.Load` | world change, also clear your entity cache here |

These need a bytecode hook and go through `StormHooks`: `MoveEvent`,
`JumpEvent`, `StepEvent`, `SlowDownEvent`, `StrafeEvent`, `ViewEvent`,
`RenderEntityEvent`.

## 5. Two useful tricks from the 1.8.9 bridge

**Motion events without a mixin.** Intercept the outgoing movement packet in the
netty pipeline, post `MotionEvent` with its values, write the result back, send,
then post the post event. Every rotation module works with nothing but a channel
handler.

**Packet fields by name.** `Mc189Packet` resolves a field once by reflection,
accepting both MCP and searge names, and caches it. Modules ask for
`getDouble("motionX")` and never see a mapping.

## 6. Checklist

* `./gradlew :storm-core:smokeTest` still passes
* ClickGUI opens, panels drag, sliders move
* HUD editor drags and scales
* A config survives a save and a load
* `.help` prints in chat
