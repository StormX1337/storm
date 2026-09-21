package xyz.stormclient.licence;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * A stable fingerprint of the computer Storm is running on.
 *
 * <p>Used to tie a licence to one machine. It is deliberately built from things
 * that survive a reinstall of the client but not a change of computer, and it
 * is hashed, so the licence never carries a MAC address or a user name around.
 */
public final class MachineId {

    private static String cached;

    private MachineId() { }

    public static synchronized String get() {
        if (cached != null) return cached;
        cached = compute();
        return cached;
    }

    private static String compute() {
        StringBuilder raw = new StringBuilder();
        raw.append(System.getProperty("os.name", "?")).append('|');
        raw.append(System.getProperty("os.arch", "?")).append('|');
        raw.append(System.getProperty("user.name", "?")).append('|');
        raw.append(macAddress());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(
                    java.nio.charset.Charset.forName("UTF-8")));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02x", hash[i]));
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.toString().hashCode());
        }
    }

    /** The first real adapter's address, skipping loopback and virtual ones. */
    private static String macAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface adapter = interfaces.nextElement();
                if (adapter.isLoopback() || adapter.isVirtual() || !adapter.isUp()) continue;
                byte[] mac = adapter.getHardwareAddress();
                if (mac == null || mac.length == 0) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02x", b));
                return sb.toString();
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
