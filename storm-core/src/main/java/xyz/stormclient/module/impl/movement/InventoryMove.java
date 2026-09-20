package xyz.stormclient.module.impl.movement;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;

/** Keeps walking and turning while a container or the inventory is open. */
public class InventoryMove extends Module {

    private final BooleanSetting rotate = add(new BooleanSetting("Free look", false));
    private final BooleanSetting jump   = add(new BooleanSetting("Jump", true));
    private final BooleanSetting chat   = add(new BooleanSetting("In chat", false));

    public InventoryMove() {
        super("InvMove", "Move while your inventory is open", Category.MOVEMENT);
    }

    public boolean allowRotate() { return rotate.get(); }
    public boolean allowJump()   { return jump.get(); }
    public boolean inChat()      { return chat.get(); }
}
