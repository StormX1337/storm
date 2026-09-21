package xyz.stormclient.licenceserver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Signs licence blobs with the private key that lives only on this server.
 *
 * <p>The payload format is the one {@code LicenceVerifier} parses, and the
 * signature covers exactly its bytes. Nothing here ever logs or returns the
 * private key.
 */
public final class Signer {

    private final PrivateKey privateKey;

    public Signer(File keyFile) throws Exception {
        String base64 = new String(Files.readAllBytes(keyFile.toPath()), StandardCharsets.UTF_8).trim();
        this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    /**
     * @param id      short identifier shown in the client, not the customer's key
     * @param holder  who it is for
     * @param plan    plan name
     * @param machine machine id it is bound to, or empty for any machine
     * @param expires when the client stops accepting this blob
     * @param until   when the purchase itself ends, 0 for never
     */
    public String sign(String id, String holder, String plan, String machine,
                       long issued, long expires, long until) throws Exception {
        String payload = "id=" + clean(id)
                + ";holder=" + clean(holder)
                + ";plan=" + clean(plan)
                + ";machine=" + clean(machine)
                + ";issued=" + issued
                + ";expires=" + expires
                + ";until=" + until;

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);
        rsa.update(bytes);

        return "STORM1."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(rsa.sign());
    }

    /** Values may not carry the separators the payload is built from. */
    private static String clean(String value) {
        return value == null ? "" : value.replace(";", " ").replace("=", " ").trim();
    }
}
