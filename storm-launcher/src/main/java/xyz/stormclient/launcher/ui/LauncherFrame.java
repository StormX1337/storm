package xyz.stormclient.launcher.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import xyz.stormclient.launcher.StormLauncher;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.ui.panels.AboutPanel;
import xyz.stormclient.launcher.ui.panels.ConsolePanel;
import xyz.stormclient.launcher.ui.panels.HomePanel;
import xyz.stormclient.launcher.ui.panels.InjectPanel;
import xyz.stormclient.launcher.ui.panels.LicencePanel;
import xyz.stormclient.launcher.ui.panels.SettingsPanel;

/** The launcher window: frameless, rounded, with its own title bar. */
public final class LauncherFrame extends JFrame {

    private static final int WIDTH = 1020;
    private static final int HEIGHT = 700;

    private static final String[] PAGES =
            { "home", "inject", "licence", "settings", "console", "about" };

    private final LauncherConfig config;
    private final PageContainer content = new PageContainer();

    private final HomePanel home;
    private final InjectPanel inject;
    private final LicencePanel licence;

    public LauncherFrame(LauncherConfig config) {
        this.config = config;

        setTitle(StormLauncher.NAME);
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(940, 640));
        setLocationRelativeTo(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 18, 18));

        home = new HomePanel(config);
        inject = new InjectPanel(config);
        licence = new LicencePanel(config);

        content.addPage("home", home);
        content.addPage("inject", inject);
        content.addPage("licence", licence);
        content.addPage("settings", new SettingsPanel(config));
        content.addPage("console", new ConsolePanel());
        content.addPage("about", new AboutPanel());

        Sidebar sidebar = new Sidebar(
                new String[] { "Play", "Inject", "Licence", "Settings", "Console", "About" },
                new Icons.Kind[] { Icons.Kind.PLAY, Icons.Kind.INJECT, Icons.Kind.KEY,
                                   Icons.Kind.SETTINGS, Icons.Kind.CONSOLE, Icons.Kind.INFO },
                this::onNavigate);

        JPanel root = new Backdrop();
        root.setLayout(new BorderLayout());
        root.add(new TitleBar(), BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
        content.show("home");

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
            }
        });
    }

    private void onNavigate(int index) {
        content.show(PAGES[index]);
        if (index == 0) home.refresh();
        if (index == 1) inject.refresh();
        if (index == 2) licence.refresh();
    }

    @Override public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) home.refresh();
    }

    /** Dark background with two slow moving accent glows. */
    private static final class Backdrop extends JPanel {

        private float phase;

        Backdrop() {
            setOpaque(false);
            new Timer(50, e -> {
                phase += 0.004F;
                repaint();
            }).start();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
            int w = getWidth(), h = getHeight();

            g.setColor(StormTheme.BACKGROUND);
            g.fillRect(0, 0, w, h);

            glow(g, w * 0.82 + Math.sin(phase) * 40, h * 0.10 + Math.cos(phase * 0.8) * 26,
                 w * 0.42, StormTheme.alpha(StormTheme.ACCENT, 34));
            glow(g, w * 0.18 + Math.cos(phase * 0.7) * 30, h * 0.92 + Math.sin(phase) * 20,
                 w * 0.34, StormTheme.alpha(new Color(0x7A5CFF), 22));

            g.dispose();
        }

        private void glow(Graphics2D g, double cx, double cy, double radius, Color color) {
            if (radius <= 0) return;
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Double(cx, cy), (float) radius,
                    new float[] { 0F, 1F },
                    new Color[] { color, StormTheme.alpha(color, 0) }));
            g.fillOval((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
            g.setPaint(null);
        }
    }

    /** Draggable strip with the window buttons. */
    private final class TitleBar extends JComponent {

        private Point grab;
        private int hoveredButton = -1;
        private float hoverFade;

        TitleBar() {
            setPreferredSize(new Dimension(0, 36));

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    grab = e.getPoint();
                    int button = buttonAt(e.getX());
                    if (button == 0) setState(ICONIFIED);
                    else if (button == 1) {
                        config.save();
                        dispose();
                        System.exit(0);
                    }
                }
                @Override public void mouseExited(MouseEvent e) { hoveredButton = -1; repaint(); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (grab == null || buttonAt(grab.x) >= 0) return;
                    Point location = getLocationOnScreen();
                    setLocation(location.x + e.getX() - grab.x, location.y + e.getY() - grab.y);
                }
                @Override public void mouseMoved(MouseEvent e) {
                    int button = buttonAt(e.getX());
                    if (button != hoveredButton) {
                        hoveredButton = button;
                        setCursor(Cursor.getPredefinedCursor(
                                button >= 0 ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR));
                        repaint();
                    }
                }
            });

            new Timer(16, e -> {
                float target = hoveredButton >= 0 ? 1F : 0F;
                if (Math.abs(hoverFade - target) > 0.01F) {
                    hoverFade += (target - hoverFade) * 0.25F;
                    repaint();
                }
            }).start();
        }

        private int buttonAt(int x) {
            int w = getWidth();
            if (x >= w - 40 && x < w - 8)  return 1;
            if (x >= w - 80 && x < w - 48) return 0;
            return -1;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
            int w = getWidth(), h = getHeight();

            g.setColor(StormTheme.SIDEBAR);
            g.fillRect(0, 0, w, h);
            g.setColor(StormTheme.alpha(StormTheme.OUTLINE, 90));
            g.fillRect(0, h - 1, w, 1);

            g.setFont(StormTheme.font(11));
            UiKit.text(g, StormLauncher.NAME, 16, 23, StormTheme.TEXT_FAINT);

            button(g, w - 80, hoveredButton == 0, false);
            button(g, w - 40, hoveredButton == 1, true);
            g.dispose();
        }

        private void button(Graphics2D g, int x, boolean hovered, boolean close) {
            if (hovered) {
                UiKit.fillRound(g, x, 5, 32, 26, 8,
                        close ? StormTheme.alpha(StormTheme.RED, (int) (70 * hoverFade))
                              : StormTheme.alpha(StormTheme.PANEL_HI, (int) (255 * hoverFade)));
            }
            Color color = hovered ? (close ? StormTheme.RED : StormTheme.TEXT) : StormTheme.TEXT_DIM;
            g.setColor(color);
            g.setStroke(new java.awt.BasicStroke(1.4F, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            if (close) {
                g.drawLine(x + 12, 13, x + 20, 22);
                g.drawLine(x + 20, 13, x + 12, 22);
            } else {
                g.drawLine(x + 12, 18, x + 20, 18);
            }
        }
    }
}
