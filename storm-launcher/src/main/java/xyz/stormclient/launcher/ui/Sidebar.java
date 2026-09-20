package xyz.stormclient.launcher.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

import xyz.stormclient.launcher.StormLauncher;

/** Left hand navigation. The selection pill slides, hovered rows fade in. */
public final class Sidebar extends JComponent {

    public static final int WIDTH = 196;

    private static final int ITEM_HEIGHT = 44;
    private static final int TOP = 104;

    private final String[] items;
    private final Icons.Kind[] icons;
    private final Consumer<Integer> onSelect;
    private final float[] hover;

    private int selected;
    private int hoveredIndex = -1;
    private float pillY = TOP;
    private float pillScale = 1F;

    public Sidebar(String[] items, Icons.Kind[] icons, Consumer<Integer> onSelect) {
        this.items = items;
        this.icons = icons;
        this.onSelect = onSelect;
        this.hover = new float[items.length];

        setPreferredSize(new Dimension(WIDTH, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int index = indexAt(e.getY());
                if (index >= 0) {
                    pillScale = 0.94F;
                    select(index);
                }
            }
            @Override public void mouseExited(MouseEvent e) { hoveredIndex = -1; }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hoveredIndex = indexAt(e.getY()); }
        });

        new Timer(16, e -> {
            boolean dirty = false;

            float target = TOP + selected * ITEM_HEIGHT;
            if (Math.abs(pillY - target) > 0.3F) {
                pillY += (target - pillY) * 0.28F;
                dirty = true;
            } else if (pillY != target) {
                pillY = target;
                dirty = true;
            }
            if (pillScale < 1F) {
                pillScale = Math.min(1F, pillScale + 0.02F);
                dirty = true;
            }
            for (int i = 0; i < hover.length; i++) {
                float want = i == hoveredIndex ? 1F : 0F;
                if (Math.abs(hover[i] - want) > 0.01F) {
                    hover[i] += (want - hover[i]) * 0.22F;
                    dirty = true;
                }
            }
            if (dirty) repaint();
        }).start();
    }

    private int indexAt(int y) {
        int index = (y - TOP) / ITEM_HEIGHT;
        return index >= 0 && index < items.length && y >= TOP ? index : -1;
    }

    public void select(int index) {
        if (index < 0 || index >= items.length) return;
        selected = index;
        onSelect.accept(index);
        repaint();
    }

    public int selected() { return selected; }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
        int h = getHeight();

        g.setColor(StormTheme.SIDEBAR);
        g.fillRect(0, 0, WIDTH, h);
        g.setColor(StormTheme.alpha(StormTheme.OUTLINE, 110));
        g.fillRect(WIDTH - 1, 0, 1, h);

        // ---- brand -------------------------------------------------
        g.setColor(StormTheme.alpha(StormTheme.ACCENT, 26));
        g.fillOval(10, 22, 44, 44);
        UiKit.bolt(g, 22, 30, 24, StormTheme.ACCENT);

        g.setFont(StormTheme.bold(22));
        UiKit.text(g, "STORM", 64, 48, StormTheme.TEXT);
        g.setFont(StormTheme.font(10));
        UiKit.text(g, "LAUNCHER " + StormLauncher.VERSION, 64, 62, StormTheme.TEXT_FAINT);

        g.setColor(StormTheme.alpha(StormTheme.OUTLINE, 90));
        g.fillRect(20, 84, WIDTH - 44, 1);

        // ---- selection pill ----------------------------------------
        double pillHeight = (ITEM_HEIGHT - 8) * pillScale;
        double pillOffset = (ITEM_HEIGHT - 8 - pillHeight) / 2;
        UiKit.fillRound(g, 12, pillY + 4 + pillOffset, WIDTH - 28, pillHeight, 10,
                StormTheme.alpha(StormTheme.ACCENT, 28));
        UiKit.fillRound(g, 12, pillY + 13, 3, ITEM_HEIGHT - 26, 2, StormTheme.ACCENT);

        // ---- items --------------------------------------------------
        for (int i = 0; i < items.length; i++) {
            int y = TOP + i * ITEM_HEIGHT;
            boolean active = i == selected;
            float hovered = hover[i];

            Color color = active
                    ? StormTheme.TEXT
                    : StormTheme.mix(StormTheme.TEXT_DIM, StormTheme.TEXT, hovered);
            Color iconColor = active
                    ? StormTheme.ACCENT
                    : StormTheme.mix(StormTheme.TEXT_DIM, StormTheme.ACCENT, hovered * 0.6F);

            Icons.draw(g, icons[i], 28 + hovered * 2, y + 13, 18, iconColor);

            g.setFont(active ? StormTheme.bold(14) : StormTheme.font(14));
            UiKit.text(g, items[i], 58 + hovered * 2, y + 27, color);
        }

        g.setFont(StormTheme.font(10));
        UiKit.text(g, "own build · no third party code", 20, h - 20, StormTheme.TEXT_FAINT);
        g.dispose();
    }
}
