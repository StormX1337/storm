package xyz.stormclient.bridge.mc189;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Timer;

import xyz.stormclient.bridge.GameVersion;
import xyz.stormclient.bridge.IChat;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IGuiBridge;
import xyz.stormclient.bridge.IInput;
import xyz.stormclient.bridge.IMinecraft;
import xyz.stormclient.bridge.INetwork;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.bridge.ISoundEngine;
import xyz.stormclient.bridge.IWorld;
import xyz.stormclient.util.StormLogger;

public final class Mc189Minecraft implements IMinecraft {

    private final Minecraft mc;

    private final Mc189Renderer renderer = new Mc189Renderer();
    private final Mc189Input input = new Mc189Input();
    private final Mc189Network network;
    private final Mc189Gui gui = new Mc189Gui();
    private final Mc189Chat chat = new Mc189Chat();
    private final Mc189Sound sound = new Mc189Sound();

    private Mc189Player playerWrapper;
    private Mc189World worldWrapper;

    /** Vanilla's render timer, reached by reflection because the field is private. */
    private final Field timerField;

    public Mc189Minecraft(Minecraft mc) {
        this.mc = mc;
        this.network = new Mc189Network(mc);
        this.timerField = findTimerField();
    }

    private Field findTimerField() {
        for (Field field : Minecraft.class.getDeclaredFields()) {
            if (field.getType() == Timer.class) {
                field.setAccessible(true);
                return field;
            }
        }
        StormLogger.warn("could not find the vanilla timer, Timer module will do nothing");
        return null;
    }

    @Override public GameVersion version() { return GameVersion.V1_8_9; }

    @Override public IPlayer player() {
        if (mc.thePlayer == null) return null;
        if (playerWrapper == null || playerWrapper.handle() != mc.thePlayer) {
            playerWrapper = new Mc189Player(mc.thePlayer);
        }
        return playerWrapper;
    }

    @Override public IWorld world() {
        if (mc.theWorld == null) return null;
        if (worldWrapper == null || worldWrapper.handle() != mc.theWorld) {
            worldWrapper = new Mc189World(mc.theWorld);
        }
        return worldWrapper;
    }

    @Override public IRenderer renderer()    { return renderer; }
    @Override public IFontRenderer font()    { return Mc189Font.vanilla(mc.fontRendererObj); }
    @Override public IFontRenderer font(String name, int size) { return Mc189Font.storm(name, size, mc.fontRendererObj); }
    @Override public IInput input()          { return input; }
    @Override public INetwork network()      { return network; }
    @Override public IGuiBridge gui()        { return gui; }
    @Override public IChat chat()            { return chat; }
    @Override public ISoundEngine sound()    { return sound; }

    @Override public boolean ingame()        { return mc.thePlayer != null && mc.theWorld != null; }
    @Override public boolean windowFocused() { return org.lwjgl.opengl.Display.isActive(); }

    @Override public int displayWidth()      { return mc.displayWidth; }
    @Override public int displayHeight()     { return mc.displayHeight; }

    @Override public int scaledWidth()       { return new ScaledResolution(mc).getScaledWidth(); }
    @Override public int scaledHeight()      { return new ScaledResolution(mc).getScaledHeight(); }
    @Override public double scaleFactor()    { return new ScaledResolution(mc).getScaleFactor(); }

    @Override public int fps()               { return Minecraft.getDebugFPS(); }
    @Override public float partialTicks()    { return timer() == null ? 1F : timer().renderPartialTicks; }
    @Override public float renderPartialTicks() { return partialTicks(); }

    @Override public float timerSpeed() {
        Timer timer = timer();
        return timer == null ? 1F : timer.timerSpeed;
    }

    @Override public void setTimerSpeed(float speed) {
        Timer timer = timer();
        if (timer != null) timer.timerSpeed = speed;
    }

    private Timer timer() {
        if (timerField == null) return null;
        try {
            return (Timer) timerField.get(mc);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Override public String serverAddress() {
        if (mc.isSingleplayer()) return "singleplayer";
        return mc.getCurrentServerData() == null ? "unknown" : mc.getCurrentServerData().serverIP;
    }

    @Override public String username() {
        return mc.getSession() == null ? "" : mc.getSession().getUsername();
    }

    @Override public File gameDirectory() { return mc.mcDataDir; }

    @Override public int perspective()             { return mc.gameSettings.thirdPersonView; }
    @Override public void setPerspective(int mode) { mc.gameSettings.thirdPersonView = mode; }

    @Override public void shutdownSafely() { mc.shutdown(); }

    public Minecraft handle() { return mc; }
}
