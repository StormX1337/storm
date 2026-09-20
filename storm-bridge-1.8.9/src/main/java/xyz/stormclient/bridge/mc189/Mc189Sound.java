package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import xyz.stormclient.bridge.ISoundEngine;

public final class Mc189Sound implements ISoundEngine {

    @Override public void play(String name, float volume, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSoundHandler() == null) return;

        String resource = map(name);
        mc.getSoundHandler().playSound(
                PositionedSoundRecord.create(new ResourceLocation(resource), pitch));
    }

    /** Storm's UI sound names mapped onto the 1.8.9 sound registry. */
    private String map(String name) {
        if (name == null) return "gui.button.press";
        switch (name.toLowerCase()) {
            case "click":   return "gui.button.press";
            case "pop":     return "random.pop";
            case "levelup": return "random.levelup";
            case "hit":     return "game.player.hurt";
            case "note":    return "note.pling";
            default:        return name;
        }
    }
}
