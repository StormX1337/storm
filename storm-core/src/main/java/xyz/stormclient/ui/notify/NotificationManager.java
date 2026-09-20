package xyz.stormclient.ui.notify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

/** Stacked toasts in the bottom right corner. */
public final class NotificationManager {

    private static final int MAX_VISIBLE = 5;

    private final List<Notification> notifications = new CopyOnWriteArrayList<Notification>();

    public void push(Notification notification) {
        notifications.add(notification);
        while (notifications.size() > 16) notifications.remove(0);
    }

    public void push(String title, String message) {
        push(new Notification(title, message, Notification.Type.INFO, 2500));
    }

    public void success(String title, String message) {
        push(new Notification(title, message, Notification.Type.SUCCESS, 2500));
    }

    public void error(String title, String message) {
        push(new Notification(title, message, Notification.Type.ERROR, 4000));
    }

    public List<Notification> visible() {
        List<Notification> out = new ArrayList<Notification>();
        for (Notification n : notifications) {
            out.add(n);
            if (out.size() >= MAX_VISIBLE) break;
        }
        return out;
    }

    @Subscribe
    public void onRender(RenderEvent.Hud event) {
        if (!Bridge.installed()) return;
        IRenderer r = Bridge.mc().renderer();
        Theme theme = Storm.get().theme();
        IFontRenderer font = Bridge.mc().font(theme.font(), 16);

        double screenW = Bridge.mc().scaledWidth();
        double screenH = Bridge.mc().scaledHeight();

        double y = screenH - 10;
        int drawn = 0;

        for (Notification n : notifications) {
            if (n.expired()) n.animation.set(false);
            if (n.expired() && n.animation.finished()) { notifications.remove(n); continue; }
            if (drawn++ >= MAX_VISIBLE) continue;

            float anim = n.animation.easedOut();
            double width = Math.max(150, font.width(n.message()) + 34);
            double height = 34;
            double x = screenW - 10 - width * anim + (1 - anim) * 12;

            y -= height + 5;

            if (theme.shadows()) r.shadow(x, y, width, height, theme.radius(), ColorUtil.withAlpha(0xFF000000, (int) (70 * anim)));
            r.roundedRect(x, y, width, height, theme.radius(), ColorUtil.fade(theme.panel(), anim));
            r.rect(x, y + height - 2, width * (1F - n.progress()), 2, ColorUtil.fade(n.type().color, anim));
            r.roundedRect(x + 6, y + height / 2 - 6, 3, 12, 1.5F, ColorUtil.fade(n.type().color, anim));

            font.draw(n.title(), x + 15, y + 7, ColorUtil.fade(theme.text(), anim));
            font.draw(font.trim(n.message(), (int) width - 22), x + 15, y + 19,
                      ColorUtil.fade(theme.textDim(), anim));
        }
    }

    public void clear() { notifications.clear(); }

    public int count() { return notifications.size(); }

    /** Removes everything that already faded out, called from the client tick. */
    public void prune() {
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (n.expired() && n.animation.finished() && MathUtil.clamp(n.animation.raw(), 0F, 1F) <= 0.001F) {
                notifications.remove(n);
            }
        }
    }
}
