import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Issues Storm licence keys.
 *
 * <p>Build and run it with any JDK 8 or newer:
 *
 * <pre>
 *   javac -d out tools/licence/LicenceTool.java
 *   java -cp out LicenceTool genkey
 *   java -cp out LicenceTool sign --holder "Ivan" --plan pro --days 30
 * </pre>
 *
 * <p>{@code genkey} writes {@code licence-private.key} and prints the matching
 * public key. Paste that public key into {@code LicenceVerifier.PUBLIC_KEY} and
 * rebuild the client; from then on Storm only starts with a key this tool
 * signed. Keep the private key off every machine you hand the client to: it is
 * the one thing that cannot be replaced if it leaks.
 */
public final class LicenceTool {

    private static final String PRIVATE_FILE = "licence-private.key";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }
        if ("genkey".equals(args[0])) { genkey(); return; }
        if ("sign".equals(args[0]))   { sign(args); return; }
        usage();
    }

    // ------------------------------------------------------------------
    private static void genkey() throws Exception {
        File target = new File(PRIVATE_FILE);
        if (target.exists()) {
            System.out.println(PRIVATE_FILE + " already exists.");
            System.out.println("Delete it only if you are prepared to reissue every key you handed out.");
            return;
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        FileOutputStream out = new FileOutputStream(target);
        try {
            out.write(priv.getBytes(Charset.forName("UTF-8")));
        } finally {
            out.close();
        }

        System.out.println("wrote " + target.getAbsolutePath());
        System.out.println();
        System.out.println("Paste this into LicenceVerifier.PUBLIC_KEY and rebuild:");
        System.out.println();
        System.out.println("    public static final String PUBLIC_KEY = \"" + pub + "\";");
        System.out.println();
        System.out.println("Keep " + PRIVATE_FILE + " private. Anyone holding it can mint keys.");
    }

    // ------------------------------------------------------------------
    private static void sign(String[] args) throws Exception {
        String holder = arg(args, "--holder", null);
        String plan = arg(args, "--plan", "standard");
        String machine = arg(args, "--machine", "");
        String id = arg(args, "--id", Long.toHexString(System.nanoTime()).substring(0, 8));
        String keyFile = arg(args, "--key", PRIVATE_FILE);
        long days = Long.parseLong(arg(args, "--days", "0"));

        if (holder == null) {
            System.out.println("sign needs --holder \"Some Name\"");
            return;
        }
        File key = new File(keyFile);
        if (!key.isFile()) {
            System.out.println("no private key at " + key.getAbsolutePath() + ", run genkey first");
            return;
        }

        long issued = System.currentTimeMillis();
        long expires = days <= 0 ? 0 : issued + days * 86400000L;

        // the payload is flat on purpose: the client parses it without a JSON
        // reader, and every byte of it is covered by the signature
        String payload = "id=" + clean(id)
                + ";holder=" + clean(holder)
                + ";plan=" + clean(plan)
                + ";machine=" + clean(machine)
                + ";issued=" + issued
                + ";expires=" + expires;

        byte[] payloadBytes = payload.getBytes(Charset.forName("UTF-8"));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(
                        new String(Files.readAllBytes(key.toPath()), Charset.forName("UTF-8")).trim())));

        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);
        rsa.update(payloadBytes);

        String blob = "STORM1."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(rsa.sign());

        System.out.println(payload);
        System.out.println();
        System.out.println(blob);
    }

    /** Values may not carry the separators the payload is built from. */
    private static String clean(String value) {
        return value == null ? "" : value.replace(";", " ").replace("=", " ").trim();
    }

    private static String arg(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) return args[i + 1];
        }
        return fallback;
    }

    private static void usage() {
        System.out.println("Storm licence tool");
        System.out.println();
        System.out.println("  genkey");
        System.out.println("      Creates the signing pair. Run once, keep the private key safe.");
        System.out.println();
        System.out.println("  sign --holder NAME [--plan NAME] [--days N] [--machine ID] [--id ID]");
        System.out.println("      Prints a key for that person. --days 0 never expires.");
        System.out.println("      --machine ties the key to one computer; the id is shown on the");
        System.out.println("      launcher's Licence page.");
    }
}
