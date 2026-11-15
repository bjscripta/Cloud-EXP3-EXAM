import java.net.URI;
import javax.net.ssl.HttpsURLConnection;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

/** Downloads the AWS regional CA bundle over verified HTTPS. No private keys. */
public class RdsTruststore {
    public static void main(String[] args) throws Exception {
        String region = args.length > 0 ? args[0] : "us-east-1";
        if (!region.matches("[a-z]{2}(?:-gov)?-[a-z]+-[0-9]+")) {
            throw new IllegalArgumentException("Invalid AWS region");
        }
        Path output = Path.of(args.length > 1 ? args[1] : "certs/rds-truststore.p12").toAbsolutePath();
        URI uri = URI.create("https://truststore.pki.rds.amazonaws.com/" + region + "/" + region + "-bundle.pem");
        var connection = (HttpsURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        if (connection.getResponseCode() != 200) throw new IllegalStateException("AWS CA download failed");
        var store = KeyStore.getInstance("PKCS12");
        char[] publicStorePassword = "changeit".toCharArray();
        store.load(null, publicStorePassword);
        int count = 0;
        try (var input = connection.getInputStream()) {
            for (var cert : CertificateFactory.getInstance("X.509").generateCertificates(input)) {
                store.setCertificateEntry("aws-rds-" + (++count), cert);
            }
        } finally { connection.disconnect(); }
        if (count == 0) throw new IllegalStateException("No certificates in AWS bundle");
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), "rds-truststore-", ".p12");
        try {
            try (var target = Files.newOutputStream(temporary)) { store.store(target, publicStorePassword); }
            // Public CA certificates must be readable by the non-root application UID.
            try {
                Files.setPosixFilePermissions(temporary, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));
            } catch (UnsupportedOperationException ignored) { /* Windows uses ACLs. */ }
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        System.out.println("Imported " + count + " AWS public certificates into " + output);
    }
}
