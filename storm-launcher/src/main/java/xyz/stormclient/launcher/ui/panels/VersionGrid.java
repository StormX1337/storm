package xyz.stormclient.launcher.ui.panels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.Icons;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Card grid of every Minecraft version Storm knows. */
public final class VersionGrid extends JComponent {

    private static final int CARD_W = 224;
    private static final int CARD_H = 96;
    private static final int GAP = 12;

    private final List<MinecraftVersion> versions = VersionRegistry.all();
    private final Consumer<MinecraftVersion> onSelect;
    private final float[] hover;
    private final float[] press;

    private Set<String> installed = java.util.Collections.emptySet();
    private String selectedId;
    private int hoveredIndex = -1;

    public VersionGrid(String initialSelection, Consumer<MinecraftVersion> onSelect) {
        this.selectedId = initialSelection;
        this.onSelect = onSelect;
        this.hover = new float[versions.size()];
        this.press = new float[versions.size()];

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int index = indexAt(e.getX(), e.getY());
                if (index < 0) return;
                MinecraftVersion version = versions.get(index);
                if (!version.playable()) return;

                press[index] = 1F;
                VersionGrid.this.selectedId = version.id();
                onSelect.accept(version);
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) { hoveredIndex = -1; }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hoveredIndex = indexAt(e.getX(), e.getY()); }
        });

        new Timer(16, e -> {
            boolean dirty = false;
            for (int i = 0; i < hover.length; i++) {
                float want = i == hoveredIndex && versions.get(i).playable() ? 1F : 0F;
                if (Math.abs(hover[i] - want) > 0.01F) {
                    hover[i] += (want - hover[i]) * 0.2F;
                    dirty = true;
                }
                if (press[i] > 0F) {
                    press[i] = Math.max(0F, press[i] - 0.08F);
                    dirty = true;
                }
            }
            if (dirty) repaint();
        }).start();
    }

    public String selectedId() { return selectedId; }

    /** Versions actually present on disk, shown as a badge on the card. */
    public void setInstalled(Set<String> installed) {
        this.installed = installed;
        repaint();
    }

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
            boolean selected = version.id().equals(selectedId);
            float lift = hover[i] * 3F - press[i] * 2F;

            double x = (i % columns) * (CARD_W + GAP);
            double y = (i / columns) * (CARD_H + GAP) - lift;

            card(g, version, x, y, selected, hover[i]);
        }
        g.dispose();
    }

    private void card(Graphics2D g, MinecraftVersion version, double x, double y,
                      boolean selected, float hovered) {
        boolean playable = version.playable();
        boolean isInstalled = installed.contains(version.id());

        // glow under the card, only while hovered or selected
        float glow = Math.max(hovered, selected ? 0.7F : 0F);
        if (glow > 0.01F && playable) {
            for (int i = 8; i > 0; i--) {
                g.setColor(StormTheme.alpha(StormTheme.ACCENT, (int) (5 * glow * (9 - i) / 4)));
                g.fillRoundRect((int) (x - i), (int) (y - i + 3),
                        CARD_W + i * 2, CARD_H + i * 2, 14 + i, 14 + i);
            }
        }

        Color fill = selected
                ? StormTheme.mix(StormTheme.PANEL_HI, StormTheme.alpha(StormTheme.ACCENT, 46), 0.6F)
                : StormTheme.mix(StormTheme.PANEL, StormTheme.PANEL_HI, hovered);
        if (!playable) fill = StormTheme.alpha(StormTheme.PANEL, 130);

        UiKit.gradient(g, x, y, CARD_W, CARD_H, 13,
                fill, StormTheme.mix(fill, StormTheme.BACKGROUND, 0.35F));
        UiKit.drawRound(g, x, y, CARD_W, CARD_H, 13, selected ? 1.6F : 1F,
                selected ? StormTheme.ACCENT
                         : StormTheme.mix(StormTheme.OUTLINE, StormTheme.ACCENT, hovered * 0.55F));

        // version number
        g.setFont(StormTheme.bold(22));
        UiKit.text(g, version.id(), x + 16, y + 36,
                playable ? StormTheme.TEXT : StormTheme.TEXT_FAINT);

        // loader and protocol
        g.setFont(StormTheme.font(10));
        UiKit.text(g, version.loader().name().toLowerCase() + "  ·  protocol " + version.protocol(),
                x + 16, y + 54, StormTheme.TEXT_FAINT);

        g.setFont(StormTheme.font(11));
        UiKit.text(g, version.note(), x + 16, y + 72, StormTheme.TEXT_DIM);

        // support state, top right
        Color status = StormTheme.fromRgb(version.support().color);
        UiKit.statusDot(g, x + CARD_W - 24, y + 15, 7, status);
        g.setFont(StormTheme.font(9));
        UiKit.textRight(g, version.support().label.toUpperCase(), x + CARD_W - 14, y + 38, status);

        // installed state, bottom right
        if (playable) {
            String label = isInstalled ? "installed" : "not installed";
            Color color = isInstalled ? StormTheme.GREEN : StormTheme.TEXT_FAINT;
            g.setFont(StormTheme.font(10));
            double labelWidth = g.getFontMetrics().stringWidth(label);
            if (isInstalled) {
                Icons.draw(g, Icons.Kind.CHECK, x + CARD_W - 26 - labelWidth, y + CARD_H - 24, 11, color);
            }
            UiKit.textRight(g, label, x + CARD_W - 14, y + CARD_H - 15, color);
        }

        if (selected) {
            UiKit.fillRound(g, x, y + 16, 3, CARD_H - 32, 2, StormTheme.ACCENT);
        }
    }

    @Override public java.awt.Dimension getPreferredSize() {
        int width = getWidth() > 0 ? getWidth() : CARD_W * 3 + GAP * 2;
        return new java.awt.Dimension(width, preferredHeight(width));
    }

    public int preferredHeight(int width) {
        int columns = Math.max(1, (width + GAP) / (CARD_W + GAP));
        int rows = (versions.size() + columns - 1) / columns;
        return rows * (CARD_H + GAP);
    }
}
