package xyz.stormclient;

/** Branding constants. Everything user facing pulls its name from here. */
public final class StormInfo {

    public static final String NAME       = "Storm";
    public static final String FULL_NAME  = "Storm Client";
    public static final String VERSION    = "1.0.0";
    public static final String BUILD      = "storm-1.0.0-release";
    public static final String AUTHOR     = "Storm Development";
    public static final String WEBSITE    = "https://stormclient.xyz";
    public static final String PACKAGE    = "xyz.stormclient";

    /** Chat prefix, using the client accent colours. */
    public static final String CHAT_PREFIX = "§b§lStorm §8| §r";

    private StormInfo() { }

    public static String watermark() {
        return NAME + " §8" + VERSION;
    }
}
