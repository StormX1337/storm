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

/** Left hand navigation with an animated selection pill. */
public final class Sidebar extends JComponent {

    public static final int WIDTH = 188;

    private static final int ITEM_HEIGHT = 40;
    private static final int TOP = 92;

    private final String[] items;
    private final String[] icons;
    private final Consumer<Integer> onSelect;

    private int selected;
    private int hovered = -1;
    private float pillY;

    public Sidebar(String[] items, String[] icons, Consumer<Integer> onSelect) {
        this.items = items;
        this.icons = icons;
        this.onSelect = onSelect;
        this.pillY = TOP;

        setPreferredSize(new Dimension(WIDTH, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int index = indexAt(e.getY());
                if (index >= 0) select(index);
            }
            @Override public void mouseExited(MouseEvent e) { hovered = -1; repaint(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int index = indexAt(e.getY());
                if (index != hovered) { hovered = index; repaint(); }
            }
        });

        new Timer(16, e -> {
            float target = TOP + selected * ITEM_HEIGHT;
            if (Math.abs(pillY - target) > 0.4F) {
                pillY += (target - pillY) * 0.25F;
                repaint();
            } else if (pillY != target) {
                pillY = target;
                repaint();
            }
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
        g.setColor(StormTheme.alpha(StormTheme.OUTLINE, 120));
        g.fillRect(WIDTH - 1, 0, 1, h);

        // logo block
        UiKit.bolt(g, 22, 30, 26, StormTheme.ACCENT);
        g.setFont(StormTheme.bold(21));
        UiKit.text(g, "STORM", 58, 45, StormTheme.TEXT);
        g.setFont(StormTheme.font(11));
        UiKit.text(g, "LAUNCHER " + StormLauncher.VERSION, 58, 59, StormTheme.TEXT_FAINT);

        // selection pill
        g.setColor(StormTheme.alpha(StormTheme.ACCENT, 26));
        g.fillRoundRect(10, (int) pillY + 3, WIDTH - 24, ITEM_HEIGHT - 6, 9, 9);
        g.setColor(StormTheme.ACCENT);
        g.fillRoundRect(10, (int) pillY + 10, 3, ITEM_HEIGHT - 20, 3, 3);

        for (int i = 0; i < items.length; i++) {
            int y = TOP + i * ITEM_HEIGHT;
            boolean active = i == selected;
            Color color = active ? StormTheme.TEXT
                        : i == hovered ? StormTheme.mix(StormTheme.TEXT_DIM, StormTheme.TEXT, 0.6F)
                        : StormTheme.TEXT_DIM;

            g.setFont(StormTheme.font(15));
            UiKit.text(g, icons[i], 26, y + 25, active ? StormTheme.ACCENT : color);
            g.setFont(active ? StormTheme.bold(14) : StormTheme.font(14));
            UiKit.text(g, items[i], 50, y + 25, color);
        }

        g.setFont(StormTheme.font(11));
        UiKit.text(g, "own build · no third party code", 20, h - 20, StormTheme.TEXT_FAINT);
        g.dispose();
    }
}
