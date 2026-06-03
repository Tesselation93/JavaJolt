package dk.javajolt.config;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
public class HibernateConfig {
    private static volatile EntityManagerFactory emf;
    private HibernateConfig() {}
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            if (System.getProperty("test") != null) {
                emf = Persistence.createEntityManagerFactory("javajolt-test-pu");
            } else if (System.getenv("DEPLOYED") != null) {
                Map<String, String> props = new HashMap<>();
                props.put("jakarta.persistence.jdbc.url", String.format(System.getenv("JDBC_CONNECTION_STRING"), System.getenv("JDBC_DB")));
                props.put("jakarta.persistence.jdbc.user", System.getenv("JDBC_USER"));
                props.put("jakarta.persistence.jdbc.password", System.getenv("JDBC_PASSWORD"));
                emf = Persistence.createEntityManagerFactory("javajolt-pu", props);
            } else {
                Properties config = loadConfig();
                Map<String, String> props = new HashMap<>();
                props.put("jakarta.persistence.jdbc.url", config.getProperty("DB_URL"));
                props.put("jakarta.persistence.jdbc.user", config.getProperty("DB_USER"));
                props.put("jakarta.persistence.jdbc.password", config.getProperty("DB_PASSWORD"));
                emf = Persistence.createEntityManagerFactory("javajolt-pu", props);
            }
        }
        return emf;
    }
    public static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream input = HibernateConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) throw new IllegalStateException("config.properties not found on classpath");
            props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties: " + e.getMessage());
        }
        return props;
    }
    public static void closeEntityManagerFactory() {
        if (emf != null && emf.isOpen()) emf.close();
    }
}