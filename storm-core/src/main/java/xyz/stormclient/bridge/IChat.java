package xyz.stormclient.bridge;

public interface IChat {
    /** Prints locally with the Storm prefix. */
    void print(String message);
    /** Prints locally without a prefix. */
    void printRaw(String message);
    /** Replaces the previous message with the same id, used for toggle spam. */
    void printWithId(String message, int id);
    boolean isOpen();
    void open(String prefill);
}
