package xyz.stormclient.bridge;

public interface IFontRenderer {

    int  width(String text);
    int  height();

    void draw(String text, double x, double y, int argb);
    void drawShadow(String text, double x, double y, int argb);
    void drawCentered(String text, double cx, double y, int argb);
    void drawCenteredShadow(String text, double cx, double y, int argb);

    /** Cuts the string to fit the given pixel width. */
    String trim(String text, int maxWidth);
}
