package xyz.stormclient.launcher.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import xyz.stormclient.launcher.StormLauncher;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.ui.panels.AboutPanel;
import xyz.stormclient.launcher.ui.panels.ConsolePanel;
import xyz.stormclient.launcher.ui.panels.HomePanel;
import xyz.stormclient.launcher.ui.panels.InjectPanel;
import xyz.stormclient.launcher.ui.panels.SettingsPanel;

/** The launcher window: frameless, rounded, with its own title bar. */
public final class LauncherFrame extends JFrame {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 640;

    private final LauncherConfig config;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final HomePanel home;
    private final InjectPanel inject;

    public LauncherFrame(LauncherConfig config) {
        this.config = config;

        setTitle(StormLauncher.NAME);
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 16, 16));

        home = new HomePanel(config);
        inject = new InjectPanel(config);

        content.setOpaque(false);
        content.add(home, "home");
        content.add(inject, "inject");
        content.add(new SettingsPanel(config), "settings");
        content.add(new ConsolePanel(), "console");
        content.add(new AboutPanel(), "about");

        Sidebar sidebar = new Sidebar(
                new String[] { "Play", "Inject", "Settings", "Console", "About" },
                new String[] { "▶", "⬇", "⚙", "⌨", "ℹ" },
                this::onNavigate);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
                g.setColor(StormTheme.BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(StormTheme.alpha(StormTheme.ACCENT, 12));
                g.fillOval(getWidth() - 320, -180, 520, 420);
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.add(new TitleBar(), BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
        cards.show(content, "home");
    }

    private void onNavigate(int index) {
        String[] names = { "home", "inject", "settings", "console", "about" };
        cards.show(content, names[index]);
        if (index == 1) inject.refresh();
        if (index == 0) home.refresh();
    }

    @Override public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) home.refresh();
    }

    /** Draggable strip with the window buttons. */
    private final class TitleBar extends JComponent {

        private Point grab;
        private int hoveredButton = -1;

        TitleBar() {
            setPreferredSize(new Dimension(0, 34));

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
        }

        private int buttonAt(int x) {
            int w = getWidth();
            if (x >= w - 38 && x < w - 8) return 1;      // close
            if (x >= w - 76 && x < w - 46) return 0;     // minimise
            return -1;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
            int w = getWidth(), h = getHeight();

            g.setColor(StormTheme.SIDEBAR);
            g.fillRect(0, 0, w, h);

            g.setFont(StormTheme.font(12));
            UiKit.text(g, StormLauncher.NAME, 14, 21, StormTheme.TEXT_FAINT);

            drawButton(g, w - 76, hoveredButton == 0, false);
            drawButton(g, w - 38, hoveredButton == 1, true);
            g.dispose();
        }

        private void drawButton(Graphics2D g, int x, boolean hovered, boolean close) {
            if (hovered) {
                g.setColor(close ? StormTheme.alpha(StormTheme.RED, 60) : StormTheme.PANEL_HI);
                g.fillRoundRect(x, 4, 30, 26, 7, 7);
            }
            Color color = hovered ? (close ? StormTheme.RED : StormTheme.TEXT) : StormTheme.TEXT_DIM;
            g.setColor(color);
            if (close) {
                g.drawLine(x + 11, 13, x + 19, 21);
                g.drawLine(x + 19, 13, x + 11, 21);
            } else {
                g.drawLine(x + 11, 17, x + 19, 17);
            }
        }
    }
}
