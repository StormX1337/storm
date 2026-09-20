package xyz.stormclient.module.impl.render;

import xyz.stormclient.Storm;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.ui.click.ClickGuiScreen;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Keyboard;

/** Opens the main menu and owns the theme settings. */
public class ClickGuiModule extends Module {

    private final ModeSetting    preset   = add(new ModeSetting("Theme", "Storm", "Storm", "Midnight", "Ember", "Mint", "Light"));
    private final NumberSetting  radius   = add(new NumberSetting("Corner radius", 6, 0, 12, 0.5).suffix("px"));
    private final NumberSetting  speed    = add(new NumberSetting("Animation speed", 6, 1, 20, 0.5));
    private final BooleanSetting shadows  = add(new BooleanSetting("Shadows", true));
    private final BooleanSetting blur     = add(new BooleanSetting("Blur", true));
    private final BooleanSetting rainbow  = add(new BooleanSetting("Rainbow accent", false));
    private final NumberSetting  fontSize = add(new NumberSetting("Font size", 18, 12, 26, 1).suffix("px"));

    public ClickGuiModule() {
        super("ClickGUI", "Opens the Storm menu", Category.RENDER);
        setKeybind(Keyboard.KEY_RSHIFT);
        for (xyz.stormclient.setting.Setting<?> setting : settings()) setting.onChange(this::apply);
    }

    @Override public void onInit() { apply(); }

    private void apply() {
        Theme theme = Storm.get().theme();
        theme.setPreset(Theme.Preset.valueOf(preset.get().toUpperCase()));
        theme.setRadius(radius.getFloat());
        theme.setAnimationSpeed(speed.getFloat());
        theme.setShadows(shadows.get());
        theme.setBlur(blur.get());
        theme.setRainbow(rainbow.get());
        theme.setFontSize(fontSize.getInt());
    }

    @Override public void onEnable() {
        apply();
        Storm.get().openScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
