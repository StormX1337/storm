package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import xyz.stormclient.bridge.IInput;

public final class Mc189Input implements IInput {

    private Minecraft mc() { return Minecraft.getMinecraft(); }

    @Override public boolean keyDown(int key) {
        return key > 0 && Keyboard.isKeyDown(key);
    }

    @Override public boolean mouseDown(int button) {
        return Mouse.isButtonDown(button);
    }

    @Override public int mouseX() {
        ScaledResolution resolution = new ScaledResolution(mc());
        return Mouse.getX() * resolution.getScaledWidth() / mc().displayWidth;
    }

    @Override public int mouseY() {
        ScaledResolution resolution = new ScaledResolution(mc());
        return resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / mc().displayHeight - 1;
    }

    @Override public boolean forward() { return mc().gameSettings.keyBindForward.isKeyDown(); }
    @Override public boolean back()    { return mc().gameSettings.keyBindBack.isKeyDown(); }
    @Override public boolean left()    { return mc().gameSettings.keyBindLeft.isKeyDown(); }
    @Override public boolean right()   { return mc().gameSettings.keyBindRight.isKeyDown(); }
    @Override public boolean jump()    { return mc().gameSettings.keyBindJump.isKeyDown(); }
    @Override public boolean sneak()   { return mc().gameSettings.keyBindSneak.isKeyDown(); }
    @Override public boolean sprint()  { return mc().gameSettings.keyBindSprint.isKeyDown(); }
    @Override public boolean attack()  { return mc().gameSettings.keyBindAttack.isKeyDown(); }
    @Override public boolean use()     { return mc().gameSettings.keyBindUseItem.isKeyDown(); }

    @Override public void setKeyState(String bind, boolean pressed) {
        KeyBinding binding = bindingFor(bind);
        if (binding == null) return;
        KeyBinding.setKeyBindState(binding.getKeyCode(), pressed);
        if (pressed) KeyBinding.onTick(binding.getKeyCode());
    }

    private KeyBinding bindingFor(String name) {
        Minecraft mc = mc();
        if (name == null) return null;
        switch (name.toLowerCase()) {
            case "forward": return mc.gameSettings.keyBindForward;
            case "back":    return mc.gameSettings.keyBindBack;
            case "left":    return mc.gameSettings.keyBindLeft;
            case "right":   return mc.gameSettings.keyBindRight;
            case "jump":    return mc.gameSettings.keyBindJump;
            case "sneak":   return mc.gameSettings.keyBindSneak;
            case "sprint":  return mc.gameSettings.keyBindSprint;
            case "attack":  return mc.gameSettings.keyBindAttack;
            case "use":     return mc.gameSettings.keyBindUseItem;
            default:        return null;
        }
    }

    @Override public boolean moving() {
        return forward() || back() || left() || right();
    }

    @Override public String keyName(int key) { return Keyboard.getKeyName(key); }
}
