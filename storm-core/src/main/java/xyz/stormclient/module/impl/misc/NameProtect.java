package xyz.stormclient.module.impl.misc;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.StringSetting;

/** Hides your real name in chat, nametags and the tab list, for recording. */
public class NameProtect extends Module {

    private final StringSetting  replacement = add(new StringSetting("Shown as", "You"));
    private final BooleanSetting chat    = add(new BooleanSetting("Chat", true));
    private final BooleanSetting tags    = add(new BooleanSetting("Nametags", true));
    private final BooleanSetting others  = add(new BooleanSetting("Hide others", false));

    public NameProtect() {
        super("NameProtect", "Hides your name for screenshots and streams", Category.MISC);
    }

    public String apply(String input) {
        if (!isEnabled() || input == null || nullCheck()) return input;
        String own = mc().username();
        if (own == null || own.isEmpty()) return input;
        return input.replace(own, replacement.get());
    }

    public boolean chat()   { return chat.get(); }
    public boolean tags()   { return tags.get(); }
    public boolean others() { return others.get(); }
}
