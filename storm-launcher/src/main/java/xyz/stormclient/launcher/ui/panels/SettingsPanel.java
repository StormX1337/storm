package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import xyz.stormclient.launcher.core.GameDirectories;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.ui.Icons;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Java path, memory, directories and the agent jar. */
public final class SettingsPanel extends BasePanel {

    private final LauncherConfig config;
    private JTextField gameDirField;
    private String saved = "";

    public SettingsPanel(LauncherConfig config) {
        super("Settings", "Where things live and how the game is started");
        this.config = config;

        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 12, 30, 24, 30));

        JPanel fields = new JPanel(new GridLayout(0, 2, 16, 12));
        fields.setOpaque(false);

        fields.add(field("Username", config.username(), config::setUsername));
        fields.add(field("Config profile", config.configProfile(), config::setConfigProfile));
        fields.add(field("Memory (MB)", String.valueOf(config.ram()), value -> {
            try { config.setRam(Integer.parseInt(value.trim())); } catch (NumberFormatException ignored) { }
        }));
        fields.add(field("Java executable", config.javaPath(), config::setJavaPath));

        gameDirField = new JTextField(config.gameDirectory());
        fields.add(field("Game directory", gameDirField, config::setGameDirectory));
        fields.add(field("Agent jar", config.agentJar(), config::setAgentJar));
        add(fields, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);
        middle.add(installations(), BorderLayout.NORTH);

        JPanel toggles = new JPanel(new GridLayout(1, 3, 12, 12));
        toggles.setOpaque(false);
        toggles.setPreferredSize(new Dimension(0, 52));
        toggles.add(toggle("Auto inject on launch", config.autoInject(), config::setAutoInject));
        toggles.add(toggle("Debug logging", config.debug(), config::setDebug));
        toggles.add(toggle("Close launcher on launch", config.closeOnLaunch(), config::setCloseOnLaunch));

        // a grid in CENTER would stretch the toggles over the whole page
        JPanel toggleHolder = new JPanel(new BorderLayout());
        toggleHolder.setOpaque(false);
        toggleHolder.add(toggles, BorderLayout.NORTH);
        middle.add(toggleHolder, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        StormButton browse = new StormButton("Browse", StormButton.Style.GHOST, this::onBrowse);
        browse.setIcon(Icons.Kind.FOLDER);
        browse.setPreferredSize(new Dimension(140, 44));

        StormButton save = new StormButton("Save settings", StormButton.Style.PRIMARY, () -> {
            config.save();
            saved = "saved";
            repaint();
        });
        save.setPreferredSize(new Dimension(180, 44));

        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(browse);
        south.add(save);
        add(south, BorderLayout.SOUTH);
    }

    /** One clickable row per launcher Storm found on this machine. */
    private JComponent installations() {
        List<GameDirectories.Install> installs =
                GameDirectories.scan(new File(config.gameDirectory()));

        JPanel holder = new JPanel(new GridLayout(1, Math.max(1, Math.min(3, installs.size())), 10, 10)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
                g.setFont(StormTheme.font(12));
                UiKit.text(g, "Minecraft installations found on this machine", 2, 12, StormTheme.TEXT_DIM);
                g.dispose();
            }
        };
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        holder.setPreferredSize(new Dimension(0, 74));

        if (installs.isEmpty()) {
            JLabel none = new JLabel("none found, set the game directory by hand");
            none.setForeground(StormTheme.TEXT_FAINT);
            none.setFont(StormTheme.font(12));
            holder.add(none);
            return holder;
        }

        for (GameDirectories.Install install : installs) {
            StormButton button = new StormButton(install.launcher, StormButton.Style.GHOST, null);
            button.setSubLabel(install.versions.size() + " versions"
                    + (install.vanillaLayout ? "" : " · inject only"));
            button.setAction(() -> {
                gameDirField.setText(install.root.getAbsolutePath());
                config.setGameDirectory(install.root.getAbsolutePath());
                config.save();
                saved = "game directory set to " + install.launcher;
                repaint();
            });
            button.setPreferredSize(new Dimension(0, 48));
            holder.add(button);
        }
        return holder;
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser(new File(config.gameDirectory()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Pick the Minecraft directory");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File chosen = chooser.getSelectedFile();
        gameDirField.setText(chosen.getAbsolutePath());
        config.setGameDirectory(chosen.getAbsolutePath());
        config.save();

        int count = GameDirectories.installedIn(chosen).size();
        saved = count + " versions found in " + chosen.getName();
        repaint();
    }

    private JComponent field(String label, String value, Consumer<String> onChange) {
        return field(label, new JTextField(value), onChange);
    }

    private JComponent field(String label, JTextField input, Consumer<String> onChange) {
        JPanel holder = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
                boolean focused = input.isFocusOwner();
                UiKit.fillRound(g, 0, 18, getWidth(), getHeight() - 18, 9, StormTheme.PANEL);
                UiKit.drawRound(g, 0, 18, getWidth(), getHeight() - 18, 9, focused ? 1.5F : 1F,
                        focused ? StormTheme.ACCENT : StormTheme.OUTLINE);
                g.dispose();
            }
        };
        holder.setOpaque(false);

        JLabel title = new JLabel(label);
        title.setForeground(StormTheme.TEXT_DIM);
        title.setFont(StormTheme.font(12));

        input.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        input.setBackground(new Color(0, 0, 0, 0));
        input.setOpaque(false);
        input.setForeground(StormTheme.TEXT);
        input.setCaretColor(StormTheme.ACCENT);
        input.setFont(StormTheme.font(13));
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { holder.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e)   { holder.repaint(); }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { onChange.accept(input.getText()); }
            public void insertUpdate(DocumentEvent e)  { changed(); }
            public void removeUpdate(DocumentEvent e)  { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });

        holder.add(title, BorderLayout.NORTH);
        holder.add(input, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(0, 62));
        return holder;
    }

    private JComponent toggle(String label, boolean initial, Consumer<Boolean> onChange) {
        final boolean[] state = { initial };
        StormButton button = new StormButton(label, StormButton.Style.GHOST, null);
        button.setSubLabel(initial ? "on" : "off");
        button.setAction(() -> {
            state[0] = !state[0];
            onChange.accept(state[0]);
            button.setSubLabel(state[0] ? "on" : "off");
            config.save();
        });
        button.setPreferredSize(new Dimension(0, 48));
        return button;
    }

    @Override protected void paintBody(Graphics2D g) {
        g.setFont(StormTheme.font(11));
        UiKit.textRight(g, "stored in " + LauncherConfig.defaultGameDirectory().getName()
                + "\\storm-launcher.properties", getWidth() - 30, 44, StormTheme.TEXT_FAINT);
        if (!saved.isEmpty()) {
            g.setFont(StormTheme.font(12));
            UiKit.textRight(g, saved, getWidth() - 30, 64, StormTheme.GREEN);
        }
    }
}
