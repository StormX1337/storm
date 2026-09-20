package xyz.stormclient.launcher.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.Timer;

/** Flat button with a hover fade, in three weights. */
public class StormButton extends JComponent {

    public enum Style { PRIMARY, GHOST, DANGER }

    private final String label;
    private final Style style;
    private Runnable action;

    private float hover;
    private boolean pressed;
    private boolean enabledState = true;
    private String subLabel = "";
    private boolean loading;
    private Icons.Kind icon;
    private boolean hovered;

    public StormButton(String label, Style style, Runnable action) {
        this.label = label;
        this.style = style;
        this.action = action;

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(150, 38));

        Timer timer = new Timer(16, e -> {
            if (loading) { repaint(); return; }

            float target = isHovered() && enabledState ? 1F : 0F;
            if (Math.abs(hover - target) < 0.01F) {
                hover = target;
            } else {
                hover += (target - hover) * 0.16F;
                repaint();
            }
        });
        timer.start();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = enabledState; repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                if (pressed && enabledState && StormButton.this.action != null) StormButton.this.action.run();
                pressed = false;
                repaint();
            }
        });
    }

    public void setAction(Runnable action) { this.action = action; }

    public void setIcon(Icons.Kind icon) {
        this.icon = icon;
        repaint();
    }

    /** Swaps the label for a spinner while something is running. */
    public void setLoading(boolean loading) {
        this.loading = loading;
        setEnabledState(!loading);
        repaint();
    }

    public boolean loading() { return loading; }

    public void setSubLabel(String subLabel) {
        this.subLabel = subLabel == null ? "" : subLabel;
        repaint();
    }

    public void setEnabledState(boolean enabled) {
        this.enabledState = enabled;
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    public boolean enabledState() { return enabledState; }

    /**
     * Tracked through enter and exit rather than asking for the pointer every
     * frame: getMousePosition costs a round trip and throws when there is no
     * display at all.
     */
    private boolean isHovered() { return hovered; }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
        int w = getWidth(), h = getHeight();
        float t = enabledState ? hover : 0F;
        double radius = 9;

        Color base;
        Color textColor;
        switch (style) {
            case PRIMARY:
                base = StormTheme.mix(StormTheme.ACCENT_DIM, StormTheme.ACCENT, 0.4F + 0.6F * t);
                textColor = new Color(0x0B1016);
                break;
            case DANGER:
                base = StormTheme.mix(StormTheme.alpha(StormTheme.RED, 40), StormTheme.RED, t);
                textColor = t > 0.5F ? new Color(0x14090C) : StormTheme.RED;
                break;
            default:
                base = StormTheme.mix(StormTheme.PANEL_HI, StormTheme.alpha(StormTheme.ACCENT, 60), t);
                textColor = StormTheme.mix(StormTheme.TEXT_DIM, StormTheme.TEXT, t);
        }
        if (!enabledState) {
            base = StormTheme.PANEL;
            textColor = StormTheme.TEXT_FAINT;
        }

        int offset = pressed ? 1 : 0;
        if (style == Style.PRIMARY && enabledState) {
            g.setColor(StormTheme.alpha(StormTheme.ACCENT, (int) (45 * t)));
            g.fillRoundRect(2, 4, w - 4, h - 4, (int) radius + 4, (int) radius + 4);
        }
        UiKit.fillRound(g, 0, offset, w, h - 1, radius, base);
        if (style == Style.GHOST) {
            UiKit.drawRound(g, 0, offset, w, h - 1, radius, 1F,
                    StormTheme.mix(StormTheme.OUTLINE, StormTheme.ACCENT, t));
        }

        if (loading) {
            Icons.spinner(g, w / 2.0 - 11, h / 2.0 - 11, 22, textColor);
            g.dispose();
            return;
        }

        double iconWidth = icon == null ? 0 : 24;
        g.setFont(StormTheme.bold(subLabel.isEmpty() ? 14 : 15));
        double labelWidth = g.getFontMetrics().stringWidth(label);
        double contentLeft = (w - labelWidth - iconWidth) / 2.0;

        if (icon != null) {
            Icons.draw(g, icon, contentLeft, h / 2.0 - 8 + offset - (subLabel.isEmpty() ? 0 : 6), 16, textColor);
        }

        if (subLabel.isEmpty()) {
            UiKit.text(g, label, contentLeft + iconWidth, h / 2.0 + 5 + offset, textColor);
        } else {
            UiKit.text(g, label, contentLeft + iconWidth, h / 2.0 + offset, textColor);
            g.setFont(StormTheme.font(11));
            UiKit.textCenter(g, subLabel, w / 2.0, h / 2.0 + 14 + offset, StormTheme.alpha(textColor, 170));
        }
        g.dispose();
    }
}
