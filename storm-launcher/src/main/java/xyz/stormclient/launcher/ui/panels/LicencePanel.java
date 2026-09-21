package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;
import xyz.stormclient.licence.Licence;
import xyz.stormclient.licence.LicenceVerifier;
import xyz.stormclient.licence.MachineId;

/**
 * Where the user pastes their key.
 *
 * <p>The launcher checks the signature here so a bad key is caught before the
 * game starts, and hands the blob to the client through a system property.
 */
public final class LicencePanel extends BasePanel {

    private final LauncherConfig config;
    private final JTextArea input = new JTextArea();

    private Licence licence;
    private String status = "";
    private Color statusColor = StormTheme.TEXT_DIM;

    public LicencePanel(LauncherConfig config) {
        super("Licence", "The key Storm starts with");
        this.config = config;
        this.licence = LicenceVerifier.verify(config.licence());

        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 56, 30, 24, 30));

        add(keyField(), BorderLayout.NORTH);

        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(button("Copy machine id", StormButton.Style.GHOST, 170, this::copyMachineId));
        south.add(button("Paste", StormButton.Style.GHOST, 110, this::paste));
        south.add(button("Remove", StormButton.Style.GHOST, 110, this::remove));
        south.add(button("Apply key", StormButton.Style.PRIMARY, 160, this::apply));
        add(south, BorderLayout.SOUTH);
    }

    private StormButton button(String label, StormButton.Style style, int width, Runnable action) {
        StormButton button = new StormButton(label, style, action);
        button.setPreferredSize(new Dimension(width, 44));
        return button;
    }

    private JComponent keyField() {
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
        holder.setPreferredSize(new Dimension(0, 118));

        JLabel title = new JLabel("Licence key");
        title.setForeground(StormTheme.TEXT_DIM);
        title.setFont(StormTheme.font(12));

        input.setText(config.licence());
        input.setLineWrap(true);
        input.setWrapStyleWord(false);
        input.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        input.setBackground(new Color(0, 0, 0, 0));
        input.setOpaque(false);
        input.setForeground(StormTheme.TEXT);
        input.setCaretColor(StormTheme.ACCENT);
        input.setFont(StormTheme.font(12));
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { holder.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e)   { holder.repaint(); }
        });

        holder.add(title, BorderLayout.NORTH);
        holder.add(input, BorderLayout.CENTER);
        return holder;
    }

    // ------------------------------------------------------------------
    private void apply() {
        String blob = input.getText().replaceAll("\\s+", "");
        // with no public key compiled in there is nothing to check against, so
        // say that rather than reporting an unchecked key as accepted
        if (!LicenceVerifier.enforced()) {
            config.setLicence(blob);
            config.save();
            licence = LicenceVerifier.verify(blob);
            status = blob.isEmpty()
                    ? "nothing to store"
                    : "stored, but this build does not check keys";
            statusColor = StormTheme.TEXT_DIM;
            repaint();
            return;
        }
        Licence checked = LicenceVerifier.verify(blob);
        if (!checked.valid()) {
            status = checked.reason();
            statusColor = StormTheme.RED;
            repaint();
            return;
        }
        config.setLicence(blob);
        config.save();
        licence = checked;
        status = "key accepted";
        statusColor = StormTheme.GREEN;
        repaint();
    }

    private void remove() {
        input.setText("");
        config.setLicence("");
        config.save();
        licence = LicenceVerifier.verify("");
        status = "key removed";
        statusColor = StormTheme.TEXT_DIM;
        repaint();
    }

    private void paste() {
        try {
            Object clip = Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (clip != null) input.setText(String.valueOf(clip).trim());
        } catch (Exception e) {
            status = "nothing to paste";
            statusColor = StormTheme.TEXT_DIM;
        }
        repaint();
    }

    /** What the user sends you so you can bind their key to their computer. */
    private void copyMachineId() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(MachineId.get()), null);
        status = "machine id copied: " + MachineId.get();
        statusColor = StormTheme.ACCENT;
        repaint();
    }

    public void refresh() {
        input.setText(config.licence());
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
