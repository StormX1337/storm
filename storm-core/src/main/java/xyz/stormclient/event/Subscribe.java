package xyz.stormclient.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a one argument method as an event handler. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /** Higher runs first. Use {@link Priority}. */
    int priority() default Priority.NORMAL;

    /** Receive the event even when a previous listener cancelled it. */
    boolean receiveCancelled() default false;
}
