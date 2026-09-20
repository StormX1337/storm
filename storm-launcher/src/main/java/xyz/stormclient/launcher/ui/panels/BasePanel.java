package xyz.stormclient.launcher.ui.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Shared header painting for the content pages. */
public abstract class BasePanel extends JPanel {

    private final String title;
    private final String subtitle;

    protected BasePanel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        setOpaque(false);
    }

    protected int headerHeight() { return 74; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());

        g.setFont(StormTheme.bold(24));
        UiKit.text(g, title, 30, 44, StormTheme.TEXT);
        g.setFont(StormTheme.font(13));
        UiKit.text(g, subtitle, 30, 63, StormTheme.TEXT_DIM);

        paintBody(g);
        g.dispose();
    }

    protected void paintBody(Graphics2D g) { }
}
