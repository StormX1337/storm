package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Java path, memory, directories and the agent jar. */
public final class SettingsPanel extends BasePanel {

    private final LauncherConfig config;

    public SettingsPanel(LauncherConfig config) {
        super("Settings", "Where things live and how the game is started");
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 10, 30, 24, 30));

        JPanel fields = new JPanel(new GridLayout(0, 2, 16, 12));
        fields.setOpaque(false);

        fields.add(field("Username", config.username(), config::setUsername));
        fields.add(field("Config profile", config.configProfile(), config::setConfigProfile));
        fields.add(field("Memory (MB)", String.valueOf(config.ram()), value -> {
            try { config.setRam(Integer.parseInt(value.trim())); } catch (NumberFormatException ignored) { }
        }));
        fields.add(field("Java executable", config.javaPath(), config::setJavaPath));
        fields.add(field("Game directory", config.gameDirectory(), config::setGameDirectory));
        fields.add(field("Agent jar", config.agentJar(), config::setAgentJar));

        add(fields, BorderLayout.NORTH);

        JPanel toggles = new JPanel(new GridLayout(0, 3, 12, 12));
        toggles.setOpaque(false);
        toggles.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        toggles.add(toggle("Auto inject on launch", config.autoInject(), config::setAutoInject));
        toggles.add(toggle("Debug logging", config.debug(), config::setDebug));
        toggles.add(toggle("Close launcher on launch", config.closeOnLaunch(), config::setCloseOnLaunch));
        add(toggles, BorderLayout.CENTER);

        StormButton save = new StormButton("Save settings", StormButton.Style.PRIMARY, () -> {
            config.save();
            repaint();
        });
        save.setPreferredSize(new Dimension(170, 42));
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(save, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    private JComponent field(String label, String value, java.util.function.Consumer<String> onChange) {
        JPanel holder = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
                UiKit.fillRound(g, 0, 18, getWidth(), getHeight() - 18, 9, StormTheme.PANEL);
                UiKit.drawRound(g, 0, 18, getWidth(), getHeight() - 18, 9, 1F, StormTheme.OUTLINE);
                g.dispose();
            }
        };
        holder.setOpaque(false);

        JLabel title = new JLabel(label);
        title.setForeground(StormTheme.TEXT_DIM);
        title.setFont(StormTheme.font(12));

        JTextField input = new JTextField(value);
        input.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        input.setBackground(new Color(0, 0, 0, 0));
        input.setOpaque(false);
        input.setForeground(StormTheme.TEXT);
        input.setCaretColor(StormTheme.ACCENT);
        input.setFont(StormTheme.font(13));
        input.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { onChange.accept(input.getText()); }
            public void insertUpdate(DocumentEvent e)  { changed(); }
            public void removeUpdate(DocumentEvent e)  { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });

        holder.add(title, BorderLayout.NORTH);
        holder.add(input, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(0, 58));
        return holder;
    }

    private JComponent toggle(String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        final boolean[] state = { initial };
        StormButton button = new StormButton(label, StormButton.Style.GHOST, null);
        button.setSubLabel(initial ? "on" : "off");
        button.setAction(() -> {
            state[0] = !state[0];
            onChange.accept(state[0]);
            button.setSubLabel(state[0] ? "on" : "off");
            config.save();
        });
        button.setPreferredSize(new Dimension(0, 46));
        return button;
    }

    @Override protected void paintBody(Graphics2D g) {
        g.setFont(StormTheme.font(12));
        UiKit.textRight(g, "stored in " + LauncherConfig.defaultGameDirectory().getName()
                + "/storm-launcher.properties", getWidth() - 30, 44, StormTheme.TEXT_FAINT);
    }
}
