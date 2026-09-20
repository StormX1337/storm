package xyz.stormclient.bridge.mc189;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import xyz.stormclient.bridge.IRenderer;

/** Fixed function GL implementation of the Storm renderer. */
public final class Mc189Renderer implements IRenderer {

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);

    // ------------------------------------------------------------------
    //  state
    // ------------------------------------------------------------------
    @Override public void push() { GlStateManager.pushMatrix(); }
    @Override public void pop()  { GlStateManager.popMatrix(); }

    @Override public void translate(double x, double y, double z) { GlStateManager.translate(x, y, z); }
    @Override public void scale(double x, double y, double z)     { GlStateManager.scale(x, y, z); }
    @Override public void rotate(float angle, float x, float y, float z) { GlStateManager.rotate(angle, x, y, z); }

    @Override public void color(int argb) {
        GlStateManager.color(r(argb), g(argb), b(argb), a(argb));
    }

    @Override public void enableBlend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
    }

    @Override public void disableBlend() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override public void enableDepth()  { GlStateManager.enableDepth(); }
    @Override public void disableDepth() { GlStateManager.disableDepth(); }
    @Override public void lineWidth(float width) { GL11.glLineWidth(width); }

    private static float a(int argb) { return ((argb >> 24) & 0xFF) / 255F; }
    private static float r(int argb) { return ((argb >> 16) & 0xFF) / 255F; }
    private static float g(int argb) { return ((argb >> 8) & 0xFF) / 255F; }
    private static float b(int argb) { return (argb & 0xFF) / 255F; }

    // ------------------------------------------------------------------
    //  2D
    // ------------------------------------------------------------------
    @Override public void rect(double x, double y, double w, double h, int argb) {
        enableBlend();
        color(argb);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x, y + h, 0).endVertex();
        buffer.pos(x + w, y + h, 0).endVertex();
        buffer.pos(x + w, y, 0).endVertex();
        buffer.pos(x, y, 0).endVertex();
        tessellator.draw();
        disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override public void rectOutline(double x, double y, double w, double h, float thickness, int argb) {
        rect(x, y, w, thickness, argb);
        rect(x, y + h - thickness, w, thickness, argb);
        rect(x, y + thickness, thickness, h - thickness * 2, argb);
        rect(x + w - thickness, y + thickness, thickness, h - thickness * 2, argb);
    }

    @Override public void roundedRect(double x, double y, double w, double h, float radius, int argb) {
        if (radius <= 0.01F) { rect(x, y, w, h, argb); return; }
        float r = Math.min(radius, (float) Math.min(w, h) / 2F);

        rect(x + r, y, w - r * 2, h, argb);
        rect(x, y + r, r, h - r * 2, argb);
        rect(x + w - r, y + r, r, h - r * 2, argb);

        corner(x + r, y + r, r, 180, 270, argb);
        corner(x + w - r, y + r, r, 270, 360, argb);
        corner(x + w - r, y + h - r, r, 0, 90, argb);
        corner(x + r, y + h - r, r, 90, 180, argb);
    }

    private void corner(double cx, double cy, float radius, float from, float to, int argb) {
        enableBlend();
        color(argb);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(cx, cy);
        for (float angle = from; angle <= to; angle += 4F) {
            double rad = Math.toRadians(angle);
            GL11.glVertex2d(cx + Math.cos(rad) * radius, cy + Math.sin(rad) * radius);
        }
        GL11.glEnd();
        disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override public void roundedRectOutline(double x, double y, double w, double h,
                                             float radius, float thickness, int argb) {
        roundedRect(x, y, w, h, radius, argb);
        roundedRect(x + thickness, y + thickness, w - thickness * 2, h - thickness * 2,
                Math.max(0F, radius - thickness), 0x00000000);
        // the inner clear pass only works on top of an opaque background, so draw
        // the four edges explicitly instead
        rect(x + radius, y, w - radius * 2, thickness, argb);
        rect(x + radius, y + h - thickness, w - radius * 2, thickness, argb);
        rect(x, y + radius, thickness, h - radius * 2, argb);
        rect(x + w - thickness, y + radius, thickness, h - radius * 2, argb);
    }

    @Override public void gradientRect(double x, double y, double w, double h, int topArgb, int bottomArgb) {
        enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x, y + h, bottomArgb);
        vertex(buffer, x + w, y + h, bottomArgb);
        vertex(buffer, x + w, y, topArgb);
        vertex(buffer, x, y, topArgb);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        disableBlend();
    }

    @Override public void gradientRectH(double x, double y, double w, double h, int leftArgb, int rightArgb) {
        enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x, y + h, leftArgb);
        vertex(buffer, x + w, y + h, rightArgb);
        vertex(buffer, x + w, y, rightArgb);
        vertex(buffer, x, y, leftArgb);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        disableBlend();
    }

    private void vertex(WorldRenderer buffer, double x, double y, int argb) {
        buffer.pos(x, y, 0)
              .color(r(argb), g(argb), b(argb), a(argb))
              .endVertex();
    }

    @Override public void circle(double cx, double cy, double radius, int argb) {
        corner(cx, cy, (float) radius, 0, 360, argb);
    }

    @Override public void arc(double cx, double cy, double radius, float start, float end,
                              float thickness, int argb) {
        enableBlend();
        color(argb);
        GL11.glLineWidth(thickness);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float angle = start; angle <= end; angle += 3F) {
            double rad = Math.toRadians(angle);
            GL11.glVertex2d(cx + Math.cos(rad) * radius, cy + Math.sin(rad) * radius);
        }
        GL11.glEnd();
        disableBlend();
    }

    @Override public void shadow(double x, double y, double w, double h, float radius, int argb) {
        int alpha = (argb >>> 24);
        for (int i = 6; i > 0; i--) {
            int step = (argb & 0x00FFFFFF) | ((alpha / (i + 2)) << 24);
            roundedRect(x - i, y - i + 1, w + i * 2, h + i * 2, radius + i, step);
        }
    }

    /** No shader pipeline on 1.8.9 by default, so the frost is faked with a tint. */
    @Override public void blur(double x, double y, double w, double h, float strength) {
        roundedRect(x, y, w, h, 0F, 0x22000000);
    }

    @Override public void image(String resource, double x, double y, double w, double h, int argb) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        color(argb);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(resource));
        } catch (Exception e) {
            return;                       // missing texture, skip instead of crashing the frame
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + h, 0).tex(0, 1).endVertex();
        buffer.pos(x + w, y + h, 0).tex(1, 1).endVertex();
        buffer.pos(x + w, y, 0).tex(1, 0).endVertex();
        buffer.pos(x, y, 0).tex(0, 0).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override public void scissorBegin(double x, double y, double w, double h) {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int factor = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * factor),
                       (int) ((resolution.getScaledHeight() - (y + h)) * factor),
                       (int) (w * factor),
                       (int) (h * factor));
    }

    @Override public void scissorEnd() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    // ------------------------------------------------------------------
    //  3D
    // ------------------------------------------------------------------
    @Override public void box3D(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, int argb, boolean filled) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        double ox = manager.viewerPosX, oy = manager.viewerPosY, oz = manager.viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(argb);

        double x1 = minX - ox, y1 = minY - oy, z1 = minZ - oz;
        double x2 = maxX - ox, y2 = maxY - oy, z2 = maxZ - oz;

        GL11.glBegin(filled ? GL11.GL_QUADS : GL11.GL_LINE_STRIP);
        edges(x1, y1, z1, x2, y2, z2);
        GL11.glEnd();

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    private void edges(double x1, double y1, double z1, double x2, double y2, double z2) {
        double[][] corners = {
                { x1, y1, z1 }, { x2, y1, z1 }, { x2, y1, z2 }, { x1, y1, z2 },
                { x1, y2, z1 }, { x2, y2, z1 }, { x2, y2, z2 }, { x1, y2, z2 }
        };
        int[][] faces = {
                { 0, 1, 2, 3 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 },
                { 1, 2, 6, 5 }, { 2, 3, 7, 6 }, { 3, 0, 4, 7 }
        };
        for (int[] face : faces) {
            for (int index : face) {
                GL11.glVertex3d(corners[index][0], corners[index][1], corners[index][2]);
            }
        }
    }

    @Override public void line3D(double x1, double y1, double z1,
                                 double x2, double y2, double z2, int argb, float width) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GL11.glLineWidth(width);
        color(argb);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1 - manager.viewerPosX, y1 - manager.viewerPosY, z1 - manager.viewerPosZ);
        GL11.glVertex3d(x2 - manager.viewerPosX, y2 - manager.viewerPosY, z2 - manager.viewerPosZ);
        GL11.glEnd();

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @Override public double[] project(double x, double y, double z) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        boolean ok = GLU.gluProject((float) (x - manager.viewerPosX),
                                    (float) (y - manager.viewerPosY),
                                    (float) (z - manager.viewerPosZ),
                                    modelView, projection, viewport, screenCoords);
        if (!ok) return null;

        float screenX = screenCoords.get(0);
        float screenY = screenCoords.get(1);
        float screenZ = screenCoords.get(2);
        screenCoords.clear();
        if (screenZ < 0 || screenZ >= 1) return null;

        int factor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        return new double[] { screenX / factor,
                              (Minecraft.getMinecraft().displayHeight - screenY) / factor };
    }

    @Override public void beginEntityOutline() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
    }

    @Override public void endEntityOutline(int argb) {
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }
}
