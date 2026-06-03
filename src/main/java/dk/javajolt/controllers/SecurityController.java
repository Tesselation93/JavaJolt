package dk.javajolt.controllers;
import dk.javajolt.daos.UserDAO;
import dk.javajolt.security.TokenUtils;
import dk.javajolt.services.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Set;

public class SecurityController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);
    private final UserDAO userDAO;
    private final UserService userService;

    public SecurityController() {
        this.userDAO = UserDAO.getInstance();
        this.userService = new UserService();
    }
    public void register(Context ctx) {
        logger.info("POST /api/auth/register");
        Map<String, String> body = ctx.bodyAsClass(Map.class);
            try {
            ctx.status(201).json(userService.register(
                    body.get("username"),
                    body.get("email"),
                    body.get("password")
            ));
         } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already")) {
                ctx.status(409).json(Map.of("error", e.getMessage()));
            } else {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            }
        }
    }

    public void login(Context ctx) {
        logger.info("POST /api/auth/login");
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        try {
            ctx.status(200).json(userService.login(
                    body.get("email"),
                    body.get("password")
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(401).json(Map.of("error", e.getMessage()));
        }
    }
    public void authenticate(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Authentication required");
        }
        try {
            Claims claims = TokenUtils.validateToken(authHeader.substring(7));
            ctx.attribute("userId", claims.get("userId", Long.class));
            ctx.attribute("isAdmin", claims.get("isAdmin", Boolean.class));
            ctx.attribute("roles", claims.get("roles", String.class));
        } catch (JwtException e) {
            throw new UnauthorizedResponse("Invalid or expired token");
        }
    }
    public void authorize(Context ctx, Set<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) return;
        String rolesAttr = ctx.attribute("roles");
        if (rolesAttr == null) {
            throw new UnauthorizedResponse("No roles found in token");
        }
        boolean hasRole = requiredRoles.stream().anyMatch(r -> rolesAttr.contains(r));
        if (!hasRole) {
            ctx.status(403).json(Map.of("error", "Forbidden: insufficient role"));
        }
    }
}