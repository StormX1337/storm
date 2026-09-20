package xyz.stormclient.ui.hud;

import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.impl.hud.HudModule;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.ui.Screen;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.MathUtil;

/**
 * Drag HUD elements around, scroll to scale them, right click to toggle them.
 * Elements snap to the screen edges, the screen centre and to each other.
 */
public final class HudEditorScreen extends Screen {

    private static final double SNAP = 4;

    private HudModule dragged;
    private double grabX, grabY;
    private HudModule hovered;

    @Override public void onOpen(int width, int height) {
        super.onOpen(width, height);
        Storm.get().hud().setEditing(true);
    }

    @Override public void onClose() {
        Storm.get().hud().setEditing(false);
        Storm.get().config().saveCurrent();
    }

    @Override public void render(int mouseX, int mouseY, float partialTicks) {
        IRenderer r = r();
        IFontRenderer font = font(16);

        r.rect(0, 0, width, height, ColorUtil.withAlpha(0xFF000000, 120));
        drawGuides(r);

        hovered = null;
        for (HudModule element : Storm.get().hud().elements()) {
            if (!element.isEnabled()) continue;

            double x = element.screenX();
            double y = element.screenY();
            double w = element.width() * element.scale();
            double h = element.height() * element.scale();

            r.push();
            r.translate(x, y, 0);
            r.scale(element.scale(), element.scale(), 1);
            element.renderElement(r, font);
            r.pop();

            boolean isHovered = MathUtil.inside(mouseX, mouseY, x, y, w, h);
            if (isHovered) hovered = element;

            int outline = element == dragged ? theme().accent()
                        : isHovered ? ColorUtil.withAlpha(theme().accent(), 160)
                        : ColorUtil.withAlpha(theme().text(), 60);
            r.rectOutline(x - 1, y - 1, w + 2, h + 2, 1F, outline);

            if (isHovered || element == dragged) {
                font.drawShadow(element.name() + "  §7" + String.format("%.2fx", element.scale()),
                        x, y - 11, theme().text());
            }
        }

        renderToolbar(r, font);
    }

    private void drawGuides(IRenderer r) {
        int color = ColorUtil.withAlpha(theme().accent(), 40);
        r.rect(width / 2.0, 0, 1, height, color);
        r.rect(0, height / 2.0, width, 1, color);
    }

    private void renderToolbar(IRenderer r, IFontRenderer font) {
        double h = 22;
        r.rect(0, height - h, width, h, ColorUtil.withAlpha(theme().panel(), 235));
        r.rect(0, height - h, width, 1, ColorUtil.withAlpha(theme().accent(), 150));

        font.draw("drag to move  ·  scroll to scale  ·  right click to toggle  ·  R to reset",
                8, height - h + 7, theme().textDim());

        String right = Storm.get().hud().visibleElements().size() + " elements active";
        font.draw(right, width - font.width(right) - 8, height - h + 7, theme().text());
    }

    @Override public void mouseDown(int mouseX, int mouseY, int button) {
        List<HudModule> elements = Storm.get().hud().elements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudModule element = elements.get(i);
            if (!element.isEnabled()) continue;

            double w = element.width() * element.scale();
            double h = element.height() * element.scale();
            if (!MathUtil.inside(mouseX, mouseY, element.screenX(), element.screenY(), w, h)) continue;

            if (button == 1) { element.toggle(); return; }
            dragged = element;
            grabX = mouseX - element.screenX();
            grabY = mouseY - element.screenY();
            return;
        }
    }

    @Override public void mouseUp(int mouseX, int mouseY, int button) { dragged = null; }

    @Override public void mouseDragged(int mouseX, int mouseY, int button) {
        if (dragged == null) return;
        double x = mouseX - grabX;
        double y = mouseY - grabY;
        double w = dragged.width() * dragged.scale();
        double h = dragged.height() * dragged.scale();

        x = snap(x, 0);
        x = snap(x, width / 2.0 - w / 2);
        x = snap(x, width - w);
        y = snap(y, 0);
        y = snap(y, height / 2.0 - h / 2);
        y = snap(y, height - h);

        dragged.setScreenPosition(MathUtil.clamp(x, 0, Math.max(0, width - w)),
                                  MathUtil.clamp(y, 0, Math.max(0, height - h)));
    }

    private double snap(double value, double target) {
        return Math.abs(value - target) < SNAP ? target : value;
    }

    @Override public void mouseScroll(int amount) {
        if (hovered == null) return;
        NumberSetting scale = (NumberSetting) hovered.setting("Scale");
        if (scale != null) scale.set(scale.get() + (amount > 0 ? 0.05 : -0.05));
    }

    @Override public void keyDown(int keyCode, char typed) {
        if (keyCode == Keyboard.code("R")) {
            Storm.get().hud().resetPositions();
            Storm.get().notifications().push("HUD", "Positions reset");
        }
    }

    @Override public boolean darkenBackground() { return false; }
}
