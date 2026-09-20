package xyz.stormclient.bridge;

/**
 * All drawing Storm does goes through here, so the UI code is identical on
 * legacy fixed function GL (1.8.9) and on modern core profile builds.
 * Coordinates are in scaled GUI space unless the method says "world".
 */
public interface IRenderer {

    // ---- state -------------------------------------------------------
    void push();
    void pop();
    void translate(double x, double y, double z);
    void scale(double x, double y, double z);
    void rotate(float angle, float x, float y, float z);
    void color(int argb);
    void enableBlend();
    void disableBlend();
    void enableDepth();
    void disableDepth();
    void lineWidth(float width);

    // ---- 2D ----------------------------------------------------------
    void rect(double x, double y, double w, double h, int argb);
    void rectOutline(double x, double y, double w, double h, float thickness, int argb);
    void roundedRect(double x, double y, double w, double h, float radius, int argb);
    void roundedRectOutline(double x, double y, double w, double h, float radius, float thickness, int argb);
    void gradientRect(double x, double y, double w, double h, int topArgb, int bottomArgb);
    void gradientRectH(double x, double y, double w, double h, int leftArgb, int rightArgb);
    void circle(double cx, double cy, double radius, int argb);
    void arc(double cx, double cy, double radius, float start, float end, float thickness, int argb);
    /** Cheap approximated drop shadow behind a rounded panel. */
    void shadow(double x, double y, double w, double h, float radius, int argb);
    /** Blur / frost behind the given area. Implementations without shaders tint instead. */
    void blur(double x, double y, double w, double h, float strength);

    void image(String resource, double x, double y, double w, double h, int argb);

    void scissorBegin(double x, double y, double w, double h);
    void scissorEnd();

    // ---- 3D (world space) --------------------------------------------
    void box3D(double minX, double minY, double minZ,
               double maxX, double maxY, double maxZ, int argb, boolean filled);
    void line3D(double x1, double y1, double z1, double x2, double y2, double z2, int argb, float width);
    /** Projects a world position into scaled GUI space, or returns null when behind the camera. */
    double[] project(double x, double y, double z);

    void beginEntityOutline();
    void endEntityOutline(int argb);
}
