package xyz.stormclient.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.stormclient.bridge.*;

/**
 * A Minecraft that does not exist.
 *
 * <p>Every bridge method is implemented with harmless defaults so the client
 * core can be booted, ticked and rendered without a game. That makes the whole
 * of storm-core testable on a plain JVM, which is exactly the point of keeping
 * it free of Minecraft imports.
 */
public final class HeadlessGame implements IMinecraft {

    private final File directory;
    private final Player player = new Player();
    private final World world = new World();
    private final Renderer renderer = new Renderer();
    private final Font font = new Font();
    private final Input input = new Input();
    private final Network network = new Network();
    private final Gui gui = new Gui();
    private final Chat chat = new Chat();

    public final List<String> chatLog = new ArrayList<String>();
    public final List<String> sentPackets = new ArrayList<String>();
    public int drawCalls;

    public HeadlessGame(File directory) { this.directory = directory; }

    @Override public GameVersion version() { return GameVersion.V1_8_9; }
    @Override public IPlayer player()      { return player; }
    @Override public IWorld world()        { return world; }
    @Override public IRenderer renderer()  { return renderer; }
    @Override public IFontRenderer font()  { return font; }
    @Override public IFontRenderer font(String name, int size) { return font; }
    @Override public IInput input()        { return input; }
    @Override public INetwork network()    { return network; }
    @Override public IGuiBridge gui()      { return gui; }
    @Override public IChat chat()          { return chat; }
    @Override public ISoundEngine sound()  { return (name, volume, pitch) -> { }; }

    @Override public boolean ingame()        { return true; }
    @Override public boolean windowFocused() { return true; }
    @Override public int displayWidth()      { return 1920; }
    @Override public int displayHeight()     { return 1080; }
    @Override public int scaledWidth()       { return 640; }
    @Override public int scaledHeight()      { return 360; }
    @Override public double scaleFactor()    { return 3; }
    @Override public int fps()               { return 240; }
    @Override public float partialTicks()    { return 1F; }
    @Override public float renderPartialTicks() { return 1F; }

    private float timerSpeed = 1F;
    @Override public float timerSpeed()      { return timerSpeed; }
    @Override public void setTimerSpeed(float speed) { this.timerSpeed = speed; }

    @Override public String serverAddress()  { return "test.local"; }
    @Override public String username()       { return "Tester"; }
    @Override public File gameDirectory()    { return directory; }

    private int perspective;
    @Override public int perspective()             { return perspective; }
    @Override public void setPerspective(int mode) { this.perspective = mode; }
    @Override public void shutdownSafely()         { }

    public IScreen openScreen() { return gui.screen; }

    // ------------------------------------------------------------------
    public class Player implements IPlayer {
        double x = 0, y = 64, z = 0;
        float yaw, pitch, serverYaw, serverPitch;
        double mx, my, mz;
        boolean sprinting, sneaking, onGround = true;
        float fall;
        final Inventory inventory = new Inventory();

        public int id() { return 1; }
        public UUID uuid() { return UUID.nameUUIDFromBytes("tester".getBytes()); }
        public String name() { return "Tester"; }
        public String displayName() { return "Tester"; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public double lastX() { return x; }
        public double lastY() { return y; }
        public double lastZ() { return z; }
        public double motionX() { return mx; }
        public double motionY() { return my; }
        public double motionZ() { return mz; }
        public float yaw() { return yaw; }
        public float pitch() { return pitch; }
        public float prevYaw() { return yaw; }
        public float prevPitch() { return pitch; }
        public float health() { return 20F; }
        public float maxHealth() { return 20F; }
        public float absorption() { return 0F; }
        public float armorValue() { return 0F; }
        public int hurtTime() { return 0; }
        public int ticksExisted() { return 200; }
        public float width() { return 0.6F; }
        public float height() { return 1.8F; }
        public float eyeHeight() { return 1.62F; }
        public boolean isPlayer() { return true; }
        public boolean isLiving() { return true; }
        public boolean isLocalPlayer() { return true; }
        public boolean isMonster() { return false; }
        public boolean isAnimal() { return false; }
        public boolean isInvisible() { return false; }
        public boolean isSneaking() { return sneaking; }
        public boolean isSprinting() { return sprinting; }
        public boolean isOnGround() { return onGround; }
        public boolean isDead() { return false; }
        public boolean isInWater() { return false; }
        public boolean isInLava() { return false; }
        public boolean isBurning() { return false; }
        public boolean isNpc() { return false; }
        public String teamName() { return ""; }
        public int teamColor() { return 0; }
        public IItemStack heldItem() { return inventory.slot(inventory.heldSlot()); }
        public IItemStack armorSlot(int slot) { return Item.EMPTY; }
        public double distanceTo(IEntity other) { return distanceTo(other.x(), other.y(), other.z()); }
        public double distanceTo(double ox, double oy, double oz) {
            double dx = x - ox, dy = y - oy, dz = z - oz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        public double distanceSqTo(IEntity other) { return Math.pow(distanceTo(other), 2); }
        public double[] boundingBox() { return new double[] { x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3 }; }
        public double renderX(float partial) { return x; }
        public double renderY(float partial) { return y; }
        public double renderZ(float partial) { return z; }

        public void setYaw(float value) { yaw = value; }
        public void setPitch(float value) { pitch = value; }
        public void setRotation(float y2, float p) { yaw = y2; pitch = p; }
        public void setServerRotation(float y2, float p) { serverYaw = y2; serverPitch = p; }
        public float serverYaw() { return serverYaw; }
        public float serverPitch() { return serverPitch; }
        public void setMotion(double a, double b, double c) { mx = a; my = b; mz = c; }
        public void setMotionX(double v) { mx = v; }
        public void setMotionY(double v) { my = v; }
        public void setMotionZ(double v) { mz = v; }
        public void setPosition(double a, double b, double c) { x = a; y = b; z = c; }
        public void setSprinting(boolean v) { sprinting = v; }
        public void setSneaking(boolean v) { sneaking = v; }
        public void setOnGround(boolean v) { onGround = v; }
        public void jump() { my = 0.42; onGround = false; }
        public void swingArm() { sentPackets.add("swing"); }
        public void attack(IEntity target) { sentPackets.add("attack:" + target.name()); }
        public void useItem() { sentPackets.add("use"); }
        public void stopUsingItem() { }
        public boolean isUsingItem() { return false; }
        public boolean isBlocking() { return false; }
        public boolean isCollidedHorizontally() { return false; }
        public boolean isCollidedVertically() { return onGround; }
        public boolean isOnLadder() { return false; }
        public boolean isInWeb() { return false; }
        public boolean isCreative() { return false; }
        public boolean isSpectator() { return false; }
        public boolean canBeHurt() { return true; }
        public float hunger() { return 20F; }
        public float saturation() { return 5F; }
        public int experienceLevel() { return 30; }
        public int jumpTicks() { return 0; }
        public void setJumpTicks(int ticks) { }
        public float fallDistance() { return fall; }
        public void setFallDistance(float value) { fall = value; }
        public float attackStrength() { return 1F; }
        public float moveForward() { return 1F; }
        public float moveStrafe() { return 0F; }
        public void setMoveForward(float v) { }
        public void setMoveStrafe(float v) { }
        public int gameMode() { return 0; }
        public IInventory inventory() { return inventory; }
        public double reach() { return 3.0; }
    }

    // ------------------------------------------------------------------
    public class Target extends Player {
        private final int id;
        private final String name;
        Target(int id, String name, double x, double z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.z = z;
        }
        @Override public int id() { return id; }
        @Override public String name() { return name; }
        @Override public String displayName() { return name; }
        @Override public boolean isLocalPlayer() { return false; }
    }

    // ------------------------------------------------------------------
    public class World implements IWorld {
        public final List<IEntity> entities = new ArrayList<IEntity>();

        public List<IEntity> entities() { return entities; }
        public List<IEntity> players()  { return entities; }
        public IEntity entityById(int id) {
            for (IEntity e : entities) if (e.id() == id) return e;
            return null;
        }
        public String blockName(int x, int y, int z) { return y < 64 ? "minecraft:stone" : "minecraft:air"; }
        public int blockId(int x, int y, int z) { return y < 64 ? 1 : 0; }
        public boolean isAir(int x, int y, int z) { return y >= 64; }
        public boolean isLiquid(int x, int y, int z) { return false; }
        public boolean isSolid(int x, int y, int z) { return y < 64; }
        public boolean isReplaceable(int x, int y, int z) { return y >= 64; }
        public boolean isContainer(int x, int y, int z) { return false; }
        public float slipperiness(int x, int y, int z) { return 0.6F; }
        public long worldTime() { return 6000; }
        public int dimension() { return 0; }
        public int minY() { return 0; }
        public int maxY() { return 256; }
        public int[] raytraceBlock(double reach) { return null; }
        public boolean collides(double a, double b, double c, double d, double e, double f) { return false; }
    }

    // ------------------------------------------------------------------
    public class Renderer implements IRenderer {
        public void push() { }
        public void pop() { }
        public void translate(double x, double y, double z) { }
        public void scale(double x, double y, double z) { }
        public void rotate(float a, float x, float y, float z) { }
        public void color(int argb) { }
        public void enableBlend() { }
        public void disableBlend() { }
        public void enableDepth() { }
        public void disableDepth() { }
        public void lineWidth(float width) { }
        public void rect(double x, double y, double w, double h, int argb) { drawCalls++; }
        public void rectOutline(double x, double y, double w, double h, float t, int argb) { drawCalls++; }
        public void roundedRect(double x, double y, double w, double h, float r, int argb) { drawCalls++; }
        public void roundedRectOutline(double x, double y, double w, double h, float r, float t, int argb) { drawCalls++; }
        public void gradientRect(double x, double y, double w, double h, int a, int b) { drawCalls++; }
        public void gradientRectH(double x, double y, double w, double h, int a, int b) { drawCalls++; }
        public void circle(double x, double y, double r, int argb) { drawCalls++; }
        public void arc(double x, double y, double r, float s, float e, float t, int argb) { drawCalls++; }
        public void shadow(double x, double y, double w, double h, float r, int argb) { drawCalls++; }
        public void blur(double x, double y, double w, double h, float s) { }
        public void image(String res, double x, double y, double w, double h, int argb) { drawCalls++; }
        public void scissorBegin(double x, double y, double w, double h) { }
        public void scissorEnd() { }
        public void box3D(double a, double b, double c, double d, double e, double f, int argb, boolean filled) { drawCalls++; }
        public void line3D(double a, double b, double c, double d, double e, double f, int argb, float w) { drawCalls++; }
        public double[] project(double x, double y, double z) { return new double[] { 100, 100 }; }
        public void beginEntityOutline() { }
        public void endEntityOutline(int argb) { }
    }

    // ------------------------------------------------------------------
    public static final class Font implements IFontRenderer {
        public int width(String text) { return text == null ? 0 : text.length() * 6; }
        public int height() { return 9; }
        public void draw(String text, double x, double y, int argb) { }
        public void drawShadow(String text, double x, double y, int argb) { }
        public void drawCentered(String text, double cx, double y, int argb) { }
        public void drawCenteredShadow(String text, double cx, double y, int argb) { }
        public String trim(String text, int maxWidth) { return text; }
    }

    // ------------------------------------------------------------------
    public static final class Item implements IItemStack {
        public static final Item EMPTY = new Item("", 0);
        private final String name;
        private final int count;
        Item(String name, int count) { this.name = name; this.count = count; }
        public boolean isEmpty() { return name.isEmpty(); }
        public String registryName() { return name; }
        public String displayName() { return name; }
        public int count() { return count; }
        public int damage() { return 0; }
        public int maxDamage() { return name.contains("sword") ? 250 : 0; }
        public int durabilityPercent() { return 100; }
        public ItemType type() {
            if (name.contains("sword")) return ItemType.SWORD;
            if (name.contains("block") || name.contains("wool")) return ItemType.BLOCK;
            return isEmpty() ? ItemType.EMPTY : ItemType.MISC;
        }
        public int enchantment(String n) { return 0; }
        public double attackDamage() { return type() == ItemType.SWORD ? 7 : 1; }
        public double miningSpeed(String block) { return 1; }
        public List<String> lore() { return new ArrayList<String>(); }
        public boolean isFood() { return false; }
        public boolean isPotion() { return false; }
        public boolean isBlock() { return type() == ItemType.BLOCK; }
        public boolean isArmor() { return false; }
        public boolean isWeapon() { return type() == ItemType.SWORD; }
        public boolean isTool() { return false; }
    }

    // ------------------------------------------------------------------
    public final class Inventory implements IInventory {
        private final IItemStack[] slots = new IItemStack[36];
        private int held;

        Inventory() {
            for (int i = 0; i < slots.length; i++) slots[i] = Item.EMPTY;
            slots[0] = new Item("minecraft:diamond_sword", 1);
            slots[1] = new Item("minecraft:wool", 64);
        }

        public IItemStack slot(int index) {
            return index >= 0 && index < slots.length ? slots[index] : Item.EMPTY;
        }
        public int size() { return slots.length; }
        public IItemStack armor(int slot) { return Item.EMPTY; }
        public IItemStack offhand() { return Item.EMPTY; }
        public int heldSlot() { return held; }
        public void setHeldSlot(int slot) { held = slot; }
        public boolean containerOpen() { return false; }
        public int windowId() { return -1; }
        public int containerSize() { return 0; }
        public IItemStack containerSlot(int index) { return Item.EMPTY; }
        public void click(int windowId, int slot, int button, int mode) {
            sentPackets.add("click:" + slot);
        }
        public void swapToHotbar(int slot, int hotbarIndex) { }
        public void drop(int slot, boolean fullStack) { }
        public void closeContainer() { }
        public int findSlot(ItemType type, int from, int to) {
            for (int i = from; i < Math.min(to, slots.length); i++) if (slots[i].type() == type) return i;
            return -1;
        }
        public int findBlockSlot() { return 1; }
    }

    // ------------------------------------------------------------------
    public final class Input implements IInput {
        public boolean attacking;
        public boolean keyDown(int key) { return false; }
        public boolean mouseDown(int button) { return false; }
        public int mouseX() { return 0; }
        public int mouseY() { return 0; }
        public boolean forward() { return true; }
        public boolean back() { return false; }
        public boolean left() { return false; }
        public boolean right() { return false; }
        public boolean jump() { return false; }
        public boolean sneak() { return false; }
        public boolean sprint() { return true; }
        public boolean attack() { return attacking; }
        public boolean use() { return false; }
        public void setKeyState(String bind, boolean pressed) { }
        public boolean moving() { return true; }
        public String keyName(int key) { return "KEY" + key; }
    }

    // ------------------------------------------------------------------
    public final class Network implements INetwork {
        public void send(IPacket packet) { sentPackets.add(packet.rawName()); }
        public void sendSilent(IPacket packet) { sentPackets.add("silent:" + packet.rawName()); }
        public void receive(IPacket packet) { }
        public int ping() { return 24; }
        public boolean connected() { return true; }
        public void sendChat(String message) { chatLog.add("> " + message); }
        public IPacket createRotationPacket(float yaw, float pitch, boolean onGround) { return new Packet("look"); }
        public IPacket createPositionPacket(double x, double y, double z, boolean onGround) { return new Packet("position"); }
        public IPacket createFullPacket(double x, double y, double z, float yaw, float pitch, boolean g) { return new Packet("full"); }
        public IPacket createOnGroundPacket(boolean onGround) { return new Packet("ground"); }
        public IPacket createUseEntityPacket(IEntity target, boolean attack) { return new Packet("use_entity"); }
        public IPacket createSwingPacket() { return new Packet("swing"); }
        public IPacket createEntityActionPacket(String action) { return new Packet(action); }
    }

    public static final class Packet implements IPacket {
        private final String name;
        Packet(String name) { this.name = name; }
        public PacketType type() { return PacketType.OTHER; }
        public String rawName() { return name; }
        public double getDouble(String field) { return 0; }
        public float getFloat(String field) { return 0; }
        public int getInt(String field) { return 0; }
        public boolean getBoolean(String field) { return false; }
        public String getString(String field) { return ""; }
        public Object getRaw() { return this; }
        public void setDouble(String field, double value) { }
        public void setFloat(String field, float value) { }
        public void setInt(String field, int value) { }
        public void setBoolean(String field, boolean value) { }
    }

    // ------------------------------------------------------------------
    public final class Gui implements IGuiBridge {
        IScreen screen;
        public void open(IScreen value) {
            if (screen != null) screen.onClose();
            screen = value;
            if (screen != null) screen.onOpen(scaledWidth(), scaledHeight());
        }
        public void close() { open(null); }
        public boolean anyScreenOpen() { return screen != null; }
        public boolean stormScreenOpen() { return screen != null; }
        public String currentScreenName() { return screen == null ? "" : screen.getClass().getSimpleName(); }
        public void grabMouse(boolean grab) { }
    }

    // ------------------------------------------------------------------
    public final class Chat implements IChat {
        public void print(String message) { chatLog.add(message); }
        public void printRaw(String message) { chatLog.add(message); }
        public void printWithId(String message, int id) { chatLog.add(message); }
        public boolean isOpen() { return false; }
        public void open(String prefill) { }
    }

    public Target spawnTarget(int id, String name, double x, double z) {
        Target target = new Target(id, name, x, z);
        world.entities.add(target);
        return target;
    }
}
