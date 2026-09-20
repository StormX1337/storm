package xyz.stormclient.launcher.ui;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Card layout with a transition.
 *
 * <p>Switching takes a snapshot of the page that is leaving, then fades it out
 * while the new one fades in and slides up a few pixels. The whole page is
 * painted through one Graphics, so the buttons and text fields on it come along
 * rather than popping in at full opacity.
 */
public final class PageContainer extends JPanel {

    private static final int DURATION_MS = 210;

    private final CardLayout cards = new CardLayout();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();

    private BufferedImage leaving;
    private long startedAt;
    private boolean animating;

    public PageContainer() {
        super();
        setLayout(cards);
        setOpaque(false);

        Timer timer = new Timer(16, e -> {
            if (!animating) return;
            if (progress() >= 1F) {
                animating = false;
                leaving = null;
            }
            repaint();
        });
        timer.start();
    }

    public void addPage(String name, JComponent page) {
        pages.put(name, page);
        add(page, name);
    }

    public void show(String name) {
        JComponent next = pages.get(name);
        if (next == null || next.isShowing()) return;

        leaving = snapshot();
        cards.show(this, name);
        startedAt = System.currentTimeMillis();
        animating = leaving != null;
        repaint();
    }

    private BufferedImage snapshot() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        super.paint(g);
        g.dispose();
        return image;
    }

    private float progress() {
        if (!animating) return 1F;
        float t = (System.currentTimeMillis() - startedAt) / (float) DURATION_MS;
        return t < 0 ? 0F : (t > 1F ? 1F : t);
    }

    private static float easeOut(float t) {
        return 1F - (float) Math.pow(1F - t, 3);
    }

    @Override public void paint(Graphics graphics) {
        if (!animating) {
            super.paint(graphics);
            return;
        }

        float t = easeOut(progress());

        Graphics2D incoming = (Graphics2D) graphics.create();
        incoming.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(t)));
        incoming.translate(0, (1F - t) * 14);
        super.paint(incoming);
        incoming.dispose();

        if (leaving != null) {
            Graphics2D outgoing = (Graphics2D) graphics.create();
            outgoing.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(1F - t)));
            outgoing.translate(0, -t * 10);
            outgoing.drawImage(leaving, 0, 0, null);
            outgoing.dispose();
        }
    }

    private static float clamp(float value) {
        return value < 0F ? 0F : (value > 1F ? 1F : value);
    }
}
