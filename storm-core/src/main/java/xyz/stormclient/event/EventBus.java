package xyz.stormclient.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reflection based bus with a per event type listener cache.
 * Registration is the slow path and only happens on module toggle, posting is
 * a plain array walk so it is safe to call every tick and every frame.
 */
public final class EventBus {

    private final Map<Class<?>, CopyOnWriteArrayList<Handler>> handlers = new HashMap<Class<?>, CopyOnWriteArrayList<Handler>>();
    private final Map<Class<?>, List<Method>> methodCache = new HashMap<Class<?>, List<Method>>();

    public void register(Object listener) {
        if (listener == null) return;
        for (Method m : methodsOf(listener.getClass())) {
            Class<?> type = m.getParameterTypes()[0];
            CopyOnWriteArrayList<Handler> list = handlers.get(type);
            if (list == null) {
                list = new CopyOnWriteArrayList<Handler>();
                handlers.put(type, list);
            }
            for (Handler h : list) {
                if (h.owner == listener && h.method.equals(m)) return;   // already registered
            }
            Subscribe sub = m.getAnnotation(Subscribe.class);
            list.add(new Handler(listener, m, sub.priority(), sub.receiveCancelled()));
            sortList(list);
        }
    }

    public void unregister(Object listener) {
        if (listener == null) return;
        for (CopyOnWriteArrayList<Handler> list : handlers.values()) {
            List<Handler> dead = new ArrayList<Handler>();
            for (Handler h : list) if (h.owner == listener) dead.add(h);
            list.removeAll(dead);
        }
    }

    /** @return the event, so callers can write {@code if (bus.post(e).isCancelled())}. */
    public <T extends Event> T post(T event) {
        CopyOnWriteArrayList<Handler> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) return event;
        for (Handler h : list) {
            if (event.isCancelled() && !h.receiveCancelled) continue;
            try {
                h.method.invoke(h.owner, event);
            } catch (Throwable t) {
                Throwable cause = t.getCause() == null ? t : t.getCause();
                System.err.println("[Storm] handler " + h.owner.getClass().getSimpleName()
                        + "#" + h.method.getName() + " failed: " + cause);
                cause.printStackTrace();
            }
        }
        return event;
    }

    public void clear() {
        handlers.clear();
    }

    private void sortList(List<Handler> list) {
        List<Handler> copy = new ArrayList<Handler>(list);
        copy.sort(new Comparator<Handler>() {
            public int compare(Handler a, Handler b) { return Integer.compare(b.priority, a.priority); }
        });
        list.clear();
        list.addAll(copy);
    }

    private List<Method> methodsOf(Class<?> type) {
        List<Method> cached = methodCache.get(type);
        if (cached != null) return cached;

        List<Method> found = new ArrayList<Method>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(Subscribe.class)) continue;
                if (m.getParameterTypes().length != 1) continue;
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (!Event.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                m.setAccessible(true);
                found.add(m);
            }
        }
        methodCache.put(type, found);
        return found;
    }

    private static final class Handler {
        final Object owner;
        final Method method;
        final int priority;
        final boolean receiveCancelled;

        Handler(Object owner, Method method, int priority, boolean receiveCancelled) {
            this.owner = owner;
            this.method = method;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
        }
    }
}
