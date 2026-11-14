import java.net.HttpURLConnection;
import java.net.URI;

/** Local readiness request; never sends credentials or logs response bodies. */
public class DockerHealthcheck {
    public static void main(String[] args) {
        try {
            var url = URI.create(System.getenv("HEALTHCHECK_URL")).toURL();
            var connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(4000);
            connection.setInstanceFollowRedirects(false);
            int expected = Integer.parseInt(System.getenv().getOrDefault("HEALTHCHECK_STATUS", "200"));
            int actual = connection.getResponseCode();
            connection.disconnect();
            if (actual != expected) {
                System.err.println("Unexpected HTTP readiness status: " + actual);
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println("Readiness failed: " + ex.getClass().getSimpleName());
            System.exit(1);
        }
    }
}
