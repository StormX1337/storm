package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.ui.StormTheme;

/** Live launcher and game output. */
public final class ConsolePanel extends BasePanel {

    private final JTextArea area = new JTextArea();

    public ConsolePanel() {
        super("Console", "Launcher, agent and game output");

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 10, 30, 24, 30));

        area.setEditable(false);
        area.setBackground(StormTheme.PANEL);
        area.setForeground(StormTheme.TEXT_DIM);
        area.setCaretColor(StormTheme.ACCENT);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(StormTheme.OUTLINE));
        scroll.getViewport().setBackground(StormTheme.PANEL);
        scroll.getVerticalScrollBar().setBackground(StormTheme.PANEL);
        scroll.setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        Log.listen(line -> SwingUtilities.invokeLater(() -> {
            area.append(line + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        }));
    }

    @Override public Color getBackground() { return StormTheme.BACKGROUND; }
}
