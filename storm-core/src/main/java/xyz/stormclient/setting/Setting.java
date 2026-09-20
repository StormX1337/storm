package xyz.stormclient.setting;

import java.util.function.Supplier;

/** Base of every configurable value. Settings are self describing so the GUI and the config writer need no extra code. */
public abstract class Setting<T> {

    private final String name;
    private String description = "";
    protected T value;
    protected final T defaultValue;
    private Supplier<Boolean> visible = () -> true;
    private Runnable onChange;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
        this.defaultValue = value;
    }

    public String name()        { return name; }
    public String description() { return description; }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S describe(String description) {
        this.description = description;
        return (S) this;
    }

    public T get() { return value; }

    public void set(T value) {
        if (this.value != null && this.value.equals(value)) return;
        this.value = value;
        if (onChange != null) onChange.run();
    }

    public T getDefault()  { return defaultValue; }
    public void reset()    { set(defaultValue); }
    public boolean isDefault() { return value != null && value.equals(defaultValue); }

    /** Hides the setting in the GUI while the condition is false. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(Supplier<Boolean> condition) {
        this.visible = condition;
        return (S) this;
    }

    public boolean visible() { return visible.get(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Runnable action) {
        this.onChange = action;
        return (S) this;
    }

    /** Serialised form used by the config system. */
    public abstract String serialize();
    public abstract void deserialize(String raw);

    /** Short value text for the GUI. */
    public String display() { return String.valueOf(value); }
}
