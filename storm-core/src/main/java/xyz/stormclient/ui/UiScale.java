package xyz.stormclient.ui;

/**
 * Every text size Storm's UI uses, in scaled GUI units.
 *
 * <p>Minecraft's own font is nine units tall, so anything much above that turns
 * the menu into a wall of oversized letters at the usual GUI scale. These stay
 * close to it and let the font renderer's oversampling keep them sharp.
 */
public final class UiScale {

    private UiScale() { }

    /** The Storm wordmark in the top bar. */
    public static final int TITLE_FONT = 13;
    /** Category names. */
    public static final int HEADER_FONT = 10;
    /** Module rows. */
    public static final int ROW_FONT = 9;
    /** Setting rows inside an expanded module. */
    public static final int COMPONENT_FONT = 8;
    /** The top bar and the hint strip along the bottom. */
    public static final int SMALL_FONT = 9;

    /** Body text in a HUD element, and the module list. */
    public static final int HUD_FONT = 9;
    /** The one large number a HUD element leads with, such as the FPS counter. */
    public static final int HUD_LARGE_FONT = 14;
    /** Nametags, which are scaled again by their distance. */
    public static final int NAMETAG_FONT = 10;
}
