package dk.javajolt;
import dk.javajolt.config.ApplicationConfig;
import dk.javajolt.util.DataSeeder;
public class Main {
    public static void main(String[] args) {
        new DataSeeder().seed();
        ApplicationConfig.start(7070);
    }
}
