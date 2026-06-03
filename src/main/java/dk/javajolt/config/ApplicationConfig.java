package dk.javajolt.config;

import dk.javajolt.controllers.SecurityController;
import dk.javajolt.exceptions.NotFoundException;
import dk.javajolt.routes.Routes;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    private static final Set<String> PUBLIC_ROUTES = Set.of(
            "POST /api/auth/register",
            "POST /api/auth/login",
            "GET /api/stackoverflow/questions"
    );

    private ApplicationConfig() {}

    public static Javalin createApp() {
        HibernateConfig.getEntityManagerFactory();
        SecurityController securityController = new SecurityController();

        return Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.allowHost("http://localhost:3000"));
                cors.addRule(it -> it.allowHost("http://localhost:5173"));
                cors.addRule(it -> it.allowHost("https://www.javajolt.dk"));
            });
            config.routes.before(ctx -> {
                if (ctx.method().name().equals("OPTIONS")) return;
                String routeKey = ctx.method().name() + " " + ctx.path();
                if (PUBLIC_ROUTES.contains(routeKey)) return;
                securityController.authenticate(ctx);
            });
            config.routes.exception(NotFoundException.class, (e, ctx) ->
                    ctx.status(404).json(Map.of("error", e.getMessage())));
            config.routes.exception(NumberFormatException.class, (e, ctx) ->
                    ctx.status(400).json(Map.of("error", "Invalid ID format")));
            config.routes.exception(IllegalArgumentException.class, (e, ctx) ->
                    ctx.status(400).json(Map.of("error", e.getMessage())));
            config.routes.exception(UnauthorizedResponse.class, (e, ctx) ->
                    ctx.status(401).json(Map.of("error", e.getMessage())));
            config.routes.exception(Exception.class, (e, ctx) -> {
                logger.error("Unhandled exception: {}", e.getMessage(), e);
                ctx.status(500).json(Map.of("error", "Internal server error"));
            });
            new Routes().addRoutes(config);
        });
    }

    public static void start(int port) {
        createApp().start(port);
        logger.info("JavaJolt API started on http://localhost:{}", port);
        logger.info("CORS enabled for http://localhost:3000");
    }
}