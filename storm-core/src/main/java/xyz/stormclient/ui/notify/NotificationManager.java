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
import xyz.stormclient.ui.UiScale;
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
        IFontRenderer title = Bridge.mc().font(theme.font(), UiScale.HUD_FONT);
        IFontRenderer body = Bridge.mc().font(theme.font(), UiScale.COMPONENT_FONT);

        double screenW = Bridge.mc().scaledWidth();
        double screenH = Bridge.mc().scaledHeight();

        double y = screenH - 10;
        int drawn = 0;

        for (Notification n : notifications) {
            if (n.expired()) n.animation.set(false);
            if (n.expired() && n.animation.finished()) { notifications.remove(n); continue; }
            if (drawn++ >= MAX_VISIBLE) continue;

            float anim = n.animation.easedOut();
            // the card grows with whichever of the two lines is longer, so the
            // message never has to be cut short to fit a fixed box
            double text = Math.max(title.width(n.title()), body.width(n.message()));
            double width = Math.max(96, text + 26);
            // the two lines decide the height, so a taller font never spills out
            double height = 7 + title.height() + body.height();
            double x = screenW - 10 - width * anim + (1 - anim) * 12;

            y -= height + 4;

            if (theme.shadows()) r.shadow(x, y, width, height, theme.radius(), ColorUtil.withAlpha(0xFF000000, (int) (70 * anim)));
            r.roundedRect(x, y, width, height, theme.radius(), ColorUtil.fade(theme.panel(), anim));
            r.roundedRectOutline(x, y, width, height, theme.radius(), 1F,
                    ColorUtil.fade(theme.outline(), anim));
            r.rect(x, y + height - 1.5, width * (1F - n.progress()), 1.5, ColorUtil.fade(n.type().color, anim));
            r.roundedRect(x + 5, y + 5, 2, height - 10, 1F, ColorUtil.fade(n.type().color, anim));

            title.draw(n.title(), x + 12, y + 3, ColorUtil.fade(theme.text(), anim));
            body.draw(body.trim(n.message(), (int) width - 18), x + 12, y + 3 + title.height(),
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
