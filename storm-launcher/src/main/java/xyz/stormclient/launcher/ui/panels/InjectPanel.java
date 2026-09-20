package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import xyz.stormclient.launcher.core.GameProcess;
import xyz.stormclient.launcher.core.Injector;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.ProcessScanner;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Attaches Storm to a Minecraft that is already running. */
public final class InjectPanel extends BasePanel {

    private final LauncherConfig config;
    private final ProcessList list = new ProcessList();
    private final StormButton injectButton;
    private final StormButton refreshButton;

    private String status = "";
    private Color statusColor = StormTheme.TEXT_DIM;

    public InjectPanel(LauncherConfig config) {
        super("Inject", "Attach the Storm agent to a running game");
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 16, 30, 24, 30));
        add(list, BorderLayout.CENTER);

        injectButton = new StormButton("INJECT", StormButton.Style.PRIMARY, this::onInject);
        injectButton.setIcon(xyz.stormclient.launcher.ui.Icons.Kind.INJECT);
        injectButton.setPreferredSize(new Dimension(190, 48));
        injectButton.setEnabledState(false);
        injectButton.setSubLabel("nothing selected");

        refreshButton = new StormButton("Rescan", StormButton.Style.GHOST, this::refresh);
        refreshButton.setIcon(xyz.stormclient.launcher.ui.Icons.Kind.REFRESH);
        refreshButton.setPreferredSize(new Dimension(150, 48));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(0, 60));
        actions.add(refreshButton);
        actions.add(injectButton);
        add(actions, BorderLayout.SOUTH);

        // keep the list fresh while the page is open
        new Timer(4000, e -> { if (isShowing()) refresh(); }).start();
    }

    public void refresh() {
        new Thread(() -> {
            List<GameProcess> found = ProcessScanner.scan();
            SwingUtilities.invokeLater(() -> {
                list.setProcesses(found);
                updateButton();
            });
        }, "Storm-Scan").start();
    }

    private void updateButton() {
        GameProcess selected = list.selected();
        injectButton.setEnabledState(selected != null && Injector.available());
        injectButton.setSubLabel(selected == null ? "nothing selected" : "pid " + selected.pid());
    }

    private void onInject() {
        GameProcess target = list.selected();
        if (target == null) return;

        MinecraftVersion version = VersionRegistry.byId(
                target.detectedVersion().isEmpty() ? config.version() : target.detectedVersion());

        File agent = new File(config.agentJar());
        File bridge = xyz.stormclient.launcher.core.LauncherPaths.bridgeJar(agent, version.bridgeJarName());
        String options = Injector.buildOptions(version.id(), config.configProfile(), bridge, config.debug());

        injectButton.setEnabledState(false);
        status = "injecting into pid " + target.pid() + "...";
        statusColor = StormTheme.TEXT_DIM;
        repaint();

        new Thread(() -> {
            Injector.Result result = Injector.inject(target, agent, options);
            SwingUtilities.invokeLater(() -> {
                status = result.message;
                statusColor = result.success ? StormTheme.GREEN : StormTheme.RED;
                updateButton();
                repaint();
            });
        }, "Storm-Inject").start();
    }

    @Override protected void paintBody(Graphics2D g) {
        g.setFont(StormTheme.font(12));
        if (!status.isEmpty()) UiKit.textRight(g, status, getWidth() - 30, 44, statusColor);
        UiKit.textRight(g, "agent: " + new File(config.agentJar()).getName(),
                getWidth() - 30, 63, StormTheme.TEXT_FAINT);
    }

    /** Scrollable, custom painted process list. */
    private final class ProcessList extends JComponent {

        private static final int ROW = 56;

        private List<GameProcess> processes = new ArrayList<>();
        private int selectedIndex = -1;
        private int hovered = -1;
        private int scroll;

        ProcessList() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int index = indexAt(e.getY());
                    if (index >= 0) {
                        selectedIndex = index;
                        updateButton();
                        repaint();
                    }
                }
                @Override public void mouseExited(MouseEvent e) { hovered = -1; repaint(); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int index = indexAt(e.getY());
                    if (index != hovered) { hovered = index; repaint(); }
                }
            });
            addMouseWheelListener(e -> {
                int max = Math.max(0, processes.size() * ROW - getHeight());
                scroll = Math.max(0, Math.min(max, scroll + e.getWheelRotation() * 30));
                repaint();
            });
        }

        void setProcesses(List<GameProcess> processes) {
            String previous = selected() == null ? null : selected().pid();
            this.processes = processes;
            this.selectedIndex = -1;

            for (int i = 0; i < processes.size(); i++) {
                if (processes.get(i).pid().equals(previous)) { selectedIndex = i; break; }
            }
            if (selectedIndex < 0) {
                for (int i = 0; i < processes.size(); i++) {
                    if (processes.get(i).minecraft()) { selectedIndex = i; break; }
                }
            }
            repaint();
        }

        GameProcess selected() {
            return selectedIndex >= 0 && selectedIndex < processes.size() ? processes.get(selectedIndex) : null;
        }

        private int indexAt(int mouseY) {
            int index = (mouseY + scroll) / ROW;
            return index >= 0 && index < processes.size() ? index : -1;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
            int w = getWidth();

            if (processes.isEmpty()) {
                UiKit.fillRound(g, 0, 0, w, 96, 12, StormTheme.alpha(StormTheme.PANEL, 200));
                g.setFont(StormTheme.bold(15));
                UiKit.text(g, "No running game found", 20, 40, StormTheme.TEXT);
                g.setFont(StormTheme.font(12));
                UiKit.text(g, "Start Minecraft, then press Rescan. "
                        + "Java 21+ needs -XX:+EnableDynamicAgentLoading on the game.",
                        20, 62, StormTheme.TEXT_DIM);
                g.dispose();
                return;
            }

            for (int i = 0; i < processes.size(); i++) {
                GameProcess process = processes.get(i);
                int y = i * ROW - scroll;
                if (y + ROW < 0 || y > getHeight()) continue;

                boolean selected = i == selectedIndex;
                Color fill = selected
                        ? StormTheme.mix(StormTheme.PANEL_HI, StormTheme.alpha(StormTheme.ACCENT, 45), 0.6F)
                        : i == hovered ? StormTheme.PANEL_HI : StormTheme.alpha(StormTheme.PANEL, 210);

                UiKit.fillRound(g, 0, y, w, ROW - 8, 10, fill);
                if (selected) UiKit.drawRound(g, 0, y, w, ROW - 8, 10, 1.4F, StormTheme.ACCENT);

                UiKit.statusDot(g, 16, y + 20, 8,
                        process.minecraft() ? StormTheme.GREEN : StormTheme.TEXT_FAINT);

                g.setFont(StormTheme.bold(14));
                UiKit.text(g, process.shortName(), 40, y + 22, StormTheme.TEXT);
                g.setFont(StormTheme.font(11));
                String detail = "pid " + process.pid()
                        + (process.detectedVersion().isEmpty() ? "" : "  ·  " + process.detectedVersion())
                        + (process.minecraft() ? "  ·  minecraft" : "  ·  java process");
                UiKit.text(g, detail, 40, y + 38, StormTheme.TEXT_DIM);

                g.setFont(StormTheme.font(11));
                UiKit.textRight(g, selected ? "target" : "", w - 16, y + 30, StormTheme.ACCENT);
            }
            g.dispose();
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(0, Math.max(120, processes.size() * ROW));
        }
    }
}
