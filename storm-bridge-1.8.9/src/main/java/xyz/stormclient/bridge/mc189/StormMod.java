package xyz.stormclient.bridge.mc189;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import xyz.stormclient.StormInfo;

/**
 * Forge entry point, used when Storm is installed as a normal mod rather than
 * injected by the launcher. Both paths end in {@link StormBridge189#install()}.
 */
@Mod(modid = StormMod.MOD_ID,
     name = StormInfo.FULL_NAME,
     version = StormInfo.VERSION,
     clientSideOnly = true,
     acceptedMinecraftVersions = "[1.8.9]")
public final class StormMod {

    public static final String MOD_ID = "storm";

    @SideOnly(Side.CLIENT)
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        StormBridge189.install();
    }
}
