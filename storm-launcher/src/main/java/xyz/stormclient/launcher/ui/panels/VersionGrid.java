package xyz.stormclient.launcher.ui.panels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Card grid of every Minecraft version Storm knows. */
public final class VersionGrid extends JComponent {

    private static final int CARD_W = 210;
    private static final int CARD_H = 92;
    private static final int GAP = 14;

    private final List<MinecraftVersion> versions = VersionRegistry.all();
    private final Consumer<MinecraftVersion> onSelect;
    private final float[] hoverAnimation;

    private String selectedId;
    private int hovered = -1;

    public VersionGrid(String initialSelection, Consumer<MinecraftVersion> onSelect) {
        this.selectedId = initialSelection;
        this.onSelect = onSelect;
        this.hoverAnimation = new float[versions.size()];

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int index = indexAt(e.getX(), e.getY());
                if (index < 0) return;
                MinecraftVersion version = versions.get(index);
                if (!version.playable()) return;
                VersionGrid.this.selectedId = version.id();
                onSelect.accept(version);
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) { hovered = -1; }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hovered = indexAt(e.getX(), e.getY()); }
        });

        new Timer(16, e -> {
            boolean dirty = false;
            for (int i = 0; i < hoverAnimation.length; i++) {
                float target = i == hovered ? 1F : 0F;
                if (Math.abs(hoverAnimation[i] - target) > 0.01F) {
                    hoverAnimation[i] += (target - hoverAnimation[i]) * 0.2F;
                    dirty = true;
                }
            }
            if (dirty) repaint();
        }).start();
    }

    public String selectedId() { return selectedId; }

    private int columns() {
        return Math.max(1, (getWidth() + GAP) / (CARD_W + GAP));
    }

    private int indexAt(int mouseX, int mouseY) {
        int columns = columns();
        for (int i = 0; i < versions.size(); i++) {
            int x = (i % columns) * (CARD_W + GAP);
            int y = (i / columns) * (CARD_H + GAP);
            if (mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H) return i;
        }
        return -1;
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
        int columns = columns();

        for (int i = 0; i < versions.size(); i++) {
            MinecraftVersion version = versions.get(i);
            int x = (i % columns) * (CARD_W + GAP);
            int y = (i / columns) * (CARD_H + GAP);
            boolean selected = version.id().equals(selectedId);
            float hover = hoverAnimation[i];

            Color fill = selected
                    ? StormTheme.mix(StormTheme.PANEL_HI, StormTheme.alpha(StormTheme.ACCENT, 40), 0.55F)
                    : StormTheme.mix(StormTheme.PANEL, StormTheme.PANEL_HI, hover);
            if (!version.playable()) fill = StormTheme.alpha(StormTheme.PANEL, 150);

            UiKit.fillRound(g, x, y, CARD_W, CARD_H, 12, fill);
            UiKit.drawRound(g, x, y, CARD_W, CARD_H, 12, selected ? 1.6F : 1F,
                    selected ? StormTheme.ACCENT
                             : StormTheme.mix(StormTheme.OUTLINE, StormTheme.ACCENT, hover * 0.5F));

            g.setFont(StormTheme.bold(20));
            UiKit.text(g, version.id(), x + 16, y + 34,
                    version.playable() ? StormTheme.TEXT : StormTheme.TEXT_FAINT);

            g.setFont(StormTheme.font(11));
            UiKit.text(g, version.loader().name().toLowerCase() + " · protocol " + version.protocol(),
                    x + 16, y + 52, StormTheme.TEXT_FAINT);
            UiKit.text(g, version.note(), x + 16, y + 68, StormTheme.TEXT_DIM);

            Color status = StormTheme.fromRgb(version.support().color);
            UiKit.statusDot(g, x + CARD_W - 22, y + 16, 7, status);
            g.setFont(StormTheme.font(10));
            UiKit.textRight(g, version.support().label, x + CARD_W - 14, y + 40, status);

            if (selected) {
                g.setFont(StormTheme.bold(10));
                UiKit.textRight(g, "SELECTED", x + CARD_W - 14, y + CARD_H - 12, StormTheme.ACCENT);
            }
        }
        g.dispose();
    }

    public int preferredHeight(int width) {
        int columns = Math.max(1, (width + GAP) / (CARD_W + GAP));
        int rows = (versions.size() + columns - 1) / columns;
        return rows * (CARD_H + GAP);
    }
}
