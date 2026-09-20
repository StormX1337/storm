# Building the 1.8.9 bridge

The bridge is the only part of Storm that touches `net.minecraft`, so it needs
deobfuscated game classes to compile against. This is the one step that cannot
be done with a plain JDK.

**This is untested.** The rest of Storm is built and verified on every change;
this workspace is not, because the machine it was written on cannot reach
Mojang's or Forge's servers. Expect to iterate.

## What you need

* **JDK 8** &mdash; ForgeGradle 2.3 is the last toolchain that handles 1.8.9 and it
  does not run on anything newer. `build-bridge.ps1` searches for one and names
  the install command if there is none. A JRE will not do, `bin\javac.exe` has
  to be there.
* **JDK 17+** &mdash; for the rest of Storm, as before. The two live side by side.
* **Forge 1.8.9** installed in the game you actually play.

## 1. Install Forge in the game

Get the 1.8.9 installer from [files.minecraftforge.net](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html)
(version `11.15.1.2318`) and run it as a client install. Then pick that profile
in your launcher.

Storm refuses to load on a vanilla 1.8.9 and says so in one line, rather than
failing somewhere inside the first tick.

## 2. Build the bridge

```powershell
cd storm-bridge-1.8.9\workspace
.\build-bridge.ps1
```

The script finds the JDK 8 itself, prints every JDK it saw so you can check it
picked the right one, builds, and copies the jar into `dist\` next to the
agent. The first run downloads and deobfuscates Minecraft and takes a while.

The workspace has its own Gradle wrapper pinned to 4.10.3, so none of this
disturbs the main build, and `JAVA_HOME` is only changed inside the script.

By hand, if you prefer:

```powershell
$env:JAVA_HOME = "<your JDK 8, the path the script printed>"
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat build
copy build\libs\storm-bridge-1.8.9.jar ..\..\dist\
```

The launcher looks for the bridge beside `storm-agent.jar`. The Play page's
status card turns that row green once it is there.

## 4. Start the game

Launch from Storm, or paste the `-javaagent` line from the Inject page into
your own launcher. The log should read:

```
[Storm/INFO] Storm agent attached via premain
[Storm/INFO] detected Minecraft 1.8.9
[Storm/INFO] bridge xyz.stormclient.bridge.mc189.StormBridge189 installed
[Storm/INFO] Storm Client 1.0.0 starting on 1.8.9
[Storm/INFO] registered 68 modules
```

Then `RSHIFT` opens the menu.

## What is not in this build

The mixins are excluded, because they need the Mixin toolchain on top of
ForgeGradle. Everything driven by Forge events works: ticks, rendering,
packets, chat, input, world changes, and every rotation module, since motion
events are derived from the outgoing movement packets.

These stay inactive until the mixins are built in:

| Module | Needs |
|---|---|
| Speed, Fly, LongJump, Jesus, NoWeb, Spider | `MoveEvent` |
| Step | `StepEvent` |
| NoSlow | `SlowDownEvent` |
| FOV, Zoom, NoHurtCam, Fullbright | `ViewEvent` |
| Chams | `RenderEntityEvent` |

KillAura, AutoClicker, Criticals, Velocity, Reach, AntiBot, ESP, Tracers,
Nametags, Trajectories, the HUD, the click GUI, configs and commands do not
depend on them.

## When it fails

ForgeGradle 2.3 is old software talking to servers that have moved since.
The usual ones:

* **`Could not resolve net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT`**
  &mdash; the buildscript repositories are unreachable or jcenter is being slow.
  Try again, then check that `https://maven.minecraftforge.net/` opens.
* **`JAVA_HOME is set to an invalid directory`** &mdash; you set it by hand to a
  path that does not exist. Run `build-bridge.ps1` instead, it finds the JDK
  and prints what it found.
* **`Unsupported class file major version`** &mdash; Gradle is running on a JDK
  that is too new. `JAVA_HOME` is not pointing at the JDK 8.
* **`Could not find method compileOnly()`** &mdash; the wrapper did not pin to
  Gradle 4.10.3. Check `gradle\wrapper\gradle-wrapper.properties`.

Send the output and I will work through it.
