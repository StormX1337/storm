package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.LicenceClient;
import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;
import xyz.stormclient.licence.Licence;
import xyz.stormclient.licence.LicenceVerifier;
import xyz.stormclient.licence.MachineId;

/**
 * Where the customer puts the key they were sold.
 *
 * <p>They type the short key; this asks the licence server to swap it for the
 * signed licence the client actually checks. Pasting a signed licence straight
 * in still works, for keys handed out by hand with no server running.
 */
public final class LicencePanel extends BasePanel {

    private final LauncherConfig config;
    private final JTextField keyField = new JTextField();
    private final JTextField serverField = new JTextField();

    private Licence licence;
    private String status = "";
    private Color statusColor = StormTheme.TEXT_DIM;
    private boolean working;

    public LicencePanel(LauncherConfig config) {
        super("Licence", "The key Storm starts with");
        this.config = config;
        this.licence = LicenceVerifier.verify(config.licence());

        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 56, 30, 24, 30));

        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 12));
        fields.setOpaque(false);
        keyField.setText(config.licenceKey());
        serverField.setText(config.licenceServer());
        fields.add(field("Licence key", keyField, config::setLicenceKey));
        fields.add(field("Licence server", serverField, config::setLicenceServer));
        add(fields, BorderLayout.NORTH);

        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(button("Copy machine id", StormButton.Style.GHOST, 170, this::copyMachineId));
        south.add(button("Paste", StormButton.Style.GHOST, 100, this::paste));
        south.add(button("Remove", StormButton.Style.GHOST, 110, this::remove));
        south.add(button("Activate", StormButton.Style.PRIMARY, 150, this::activate));
        add(south, BorderLayout.SOUTH);
    }

    private StormButton button(String label, StormButton.Style style, int width, Runnable action) {
        StormButton button = new StormButton(label, style, action);
        button.setPreferredSize(new Dimension(width, 44));
        return button;
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
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                onChange.accept(input.getText().trim());
                config.save();
                holder.repaint();
            }
        });

        holder.add(title, BorderLayout.NORTH);
        holder.add(input, BorderLayout.CENTER);
        return holder;
    }

    // ------------------------------------------------------------------
    private void activate() {
        if (working) return;
        String typed = keyField.getText().replaceAll("\\s+", "");
        config.setLicenceKey(typed);
        config.setLicenceServer(serverField.getText().trim());
        config.save();

        // a signed licence pasted straight in needs no server at all
        if (typed.startsWith("STORM1.")) {
            applyBlob(typed, "key accepted");
            return;
        }

        if (config.licenceServer().isEmpty()) {
            say("enter your licence server's address as well, "
                    + "or paste a signed licence instead", StormTheme.RED);
            return;
        }

        working = true;
        say("asking " + config.licenceServer() + "...", StormTheme.TEXT_DIM);
        new Thread(() -> {
            LicenceClient.Result result = LicenceClient.activate(config.licenceServer(), typed);
            SwingUtilities.invokeLater(() -> {
                working = false;
                if (result.ok()) {
                    applyBlob(result.licence, "activated for " + result.holder);
                } else {
                    if (result.refused) {
                        // the server made a decision, so the old licence is stale
                        config.setLicence("");
                        config.save();
                        licence = LicenceVerifier.verify("");
                    }
                    say(result.error, result.refused ? StormTheme.RED : StormTheme.TEXT_DIM);
                    Log.warn("licence: " + result.error);
                }
            });
        }, "storm-licence").start();
    }

    private void applyBlob(String blob, String success) {
        // with no public key compiled in there is nothing to check it against,
        // so say that rather than reporting an unchecked key as accepted
        if (!LicenceVerifier.enforced()) {
            config.setLicence(blob);
            config.save();
            licence = LicenceVerifier.verify(blob);
            say("stored, but this build does not check keys", StormTheme.TEXT_DIM);
            return;
        }
        Licence checked = LicenceVerifier.verify(blob);
        if (!checked.valid()) {
            say(checked.reason(), StormTheme.RED);
            return;
        }
        config.setLicence(blob);
        config.save();
        licence = checked;
        say(success, StormTheme.GREEN);
    }

    private void remove() {
        keyField.setText("");
        config.setLicence("");
        config.setLicenceKey("");
        config.save();
        licence = LicenceVerifier.verify("");
        say("key removed", StormTheme.TEXT_DIM);
    }

    private void paste() {
        try {
            Object clip = Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (clip != null) keyField.setText(String.valueOf(clip).trim());
        } catch (Exception e) {
            say("nothing to paste", StormTheme.TEXT_DIM);
        }
        repaint();
    }

    /** What the customer sends you so you can tie their key to their computer. */
    private void copyMachineId() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(MachineId.get()), null);
        say("machine id copied: " + MachineId.get(), StormTheme.ACCENT);
    }

    private void say(String message, Color color) {
        status = message;
        statusColor = color;
        repaint();
    }

    public void refresh() {
        keyField.setText(config.licenceKey());
        serverField.setText(config.licenceServer());
        licence = LicenceVerifier.verify(config.licence());
        repaint();
    }

    @Override protected void paintBody(Graphics2D g) {
        int y = headerHeight() + 6;
        boolean enforced = LicenceVerifier.enforced();
        boolean ok = licence.valid();
        Color tint = !enforced ? StormTheme.TEXT_DIM : ok ? StormTheme.GREEN : StormTheme.RED;

        UiKit.fillRound(g, 30, y, getWidth() - 60, 38, 10, StormTheme.PANEL);
        UiKit.drawRound(g, 30, y, getWidth() - 60, 38, 10, 1F, tint);
        g.setColor(tint);
        g.fillOval(44, y + 16, 7, 7);

        g.setFont(StormTheme.bold(13));
        UiKit.text(g, !enforced ? "This build does not require a key"
                : ok ? "Licensed to " + licence.holder() : "No valid key", 60, y + 17, StormTheme.TEXT);
        g.setFont(StormTheme.font(11));
        UiKit.text(g, !enforced
                        ? "Compile the client with a public key to turn licensing on"
                        : licence.statusLine(),
                60, y + 30, StormTheme.TEXT_DIM);

        g.setFont(StormTheme.font(11));
        UiKit.textRight(g, "machine id " + MachineId.get(), getWidth() - 30, 44, StormTheme.TEXT_FAINT);
        if (!status.isEmpty()) {
            g.setFont(StormTheme.font(12));
            UiKit.textRight(g, status, getWidth() - 30, 64, statusColor);
        }
    }
}
