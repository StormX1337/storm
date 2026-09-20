package xyz.stormclient.test;

import java.io.File;
import java.nio.file.Files;

import xyz.stormclient.Storm;
import xyz.stormclient.StormBoot;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.event.events.MotionEvent;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.impl.combat.KillAura;
import xyz.stormclient.module.impl.hud.Hud;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.ui.click.ClickGuiScreen;
import xyz.stormclient.util.StormLogger;

/**
 * Boots the whole client against {@link HeadlessGame} and drives it for a few
 * hundred ticks. Run it with:
 *
 * <pre>java -cp build/classes xyz.stormclient.test.StormSmokeTest</pre>
 */
public final class StormSmokeTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        StormLogger.setDebug(true);
        File directory = Files.createTempDirectory("storm-test").toFile();
        directory.deleteOnExit();

        HeadlessGame game = new HeadlessGame(directory);
        StormBoot.boot(game);

        check("client booted", Storm.get().initialised());
        check("bridge installed", Bridge.installed());
        check("modules registered", Storm.get().modules().all().size() >= 50);
        check("commands registered", Storm.get().commands().commands().size() >= 10);
        check("every category has modules", everyCategoryFilled());

        // ---- modules -------------------------------------------------
        KillAura aura = Storm.get().modules().get(KillAura.class);
        check("KillAura found", aura != null);

        game.spawnTarget(7, "Victim", 0.0, 2.0);   // two blocks straight ahead
        aura.setEnabled(true);
        check("KillAura enabled", aura.isEnabled());

        for (int i = 0; i < 40; i++) {
            Storm.get().bus().post(new TickEvent.Pre());
            Storm.get().bus().post(new MotionEvent(true, 0, 64, 0, 0, 0, true));
            Storm.get().bus().post(new MotionEvent(false, 0, 64, 0, 0, 0, true));
            Storm.get().bus().post(new TickEvent.Post());
            Thread.sleep(3);
        }
        check("KillAura picked a target", aura.target() != null);
        check("KillAura attacked", game.sentPackets.contains("attack:Victim"));
        check("rotations were sent to the server", game.player().serverYaw() != 0F);

        // ---- rendering ----------------------------------------------
        Storm.get().modules().get(Hud.class).setEnabled(true);
        int before = game.drawCalls;
        for (int i = 0; i < 5; i++) Storm.get().bus().post(new RenderEvent.Hud(1F));
        check("HUD drew something", game.drawCalls > before);

        // ---- gui -----------------------------------------------------
        ClickGuiScreen gui = new ClickGuiScreen();
        Storm.get().openScreen(gui);
        gui.render(10, 10, 1F);
        gui.mouseDown(20, 40, 0);
        gui.mouseUp(20, 40, 0);
        gui.keyDown(0, 'a');
        check("click gui has a panel per category", gui.panels().size() == Category.values().length);

        // ---- settings and config -------------------------------------
        NumberSetting range = (NumberSetting) aura.setting("Range");
        range.set(4.25);
        check("number setting clamps and rounds", range.get() == 4.25);

        check("config saved", Storm.get().config().save("smoke"));
        range.set(3.0);
        check("config loaded", Storm.get().config().load("smoke"));
        check("setting restored from config", ((NumberSetting) aura.setting("Range")).get() == 4.25);

        // ---- commands ------------------------------------------------
        Storm.get().commands().dispatch("toggle KillAura");
        check("toggle command works", !aura.isEnabled());
        Storm.get().commands().dispatch("friend add Someone");
        check("friend command works", Storm.get().friends().isFriend("someone"));
        Storm.get().commands().dispatch("info");
        check("info command printed", !game.chatLog.isEmpty());

        // ---- notifications -------------------------------------------
        Storm.get().notifications().success("Test", "notification");
        check("notification queued", Storm.get().notifications().count() > 0);

        // ---- shutdown ------------------------------------------------
        Storm.get().shutdown();
        check("shutdown disabled everything", Storm.get().modules().enabled().isEmpty());

        System.out.println();
        System.out.println(StormInfo.FULL_NAME + " smoke test: "
                + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) System.exit(1);
    }

    private static boolean everyCategoryFilled() {
        for (Category category : Category.values()) {
            if (Storm.get().modules().byCategory(category).isEmpty()) {
                System.out.println("  category " + category + " is empty");
                return false;
            }
        }
        return true;
    }

    private static void check(String description, boolean condition) {
        checks++;
        if (!condition) failures++;
        System.out.println((condition ? "  ok   " : "  FAIL ") + description);
    }

}
