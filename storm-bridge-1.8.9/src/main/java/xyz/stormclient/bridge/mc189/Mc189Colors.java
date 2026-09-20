package xyz.stormclient.bridge.mc189;

import net.minecraft.util.EnumChatFormatting;

/** Vanilla chat colour to ARGB, used for team colours in the ESP and nametags. */
public final class Mc189Colors {

    private Mc189Colors() { }

    public static int of(EnumChatFormatting format) {
        switch (format) {
            case BLACK:        return 0xFF000000;
            case DARK_BLUE:    return 0xFF0000AA;
            case DARK_GREEN:   return 0xFF00AA00;
            case DARK_AQUA:    return 0xFF00AAAA;
            case DARK_RED:     return 0xFFAA0000;
            case DARK_PURPLE:  return 0xFFAA00AA;
            case GOLD:         return 0xFFFFAA00;
            case GRAY:         return 0xFFAAAAAA;
            case DARK_GRAY:    return 0xFF555555;
            case BLUE:         return 0xFF5555FF;
            case GREEN:        return 0xFF55FF55;
            case AQUA:         return 0xFF55FFFF;
            case RED:          return 0xFFFF5555;
            case LIGHT_PURPLE: return 0xFFFF55FF;
            case YELLOW:       return 0xFFFFFF55;
            case WHITE:        return 0xFFFFFFFF;
            default:           return 0;
        }
    }
}
