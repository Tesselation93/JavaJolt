package dk.javajolt;

import dk.javajolt.config.ApplicationConfig;
import dk.javajolt.config.HibernateConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class JavaJoltApiTest {

    private static Javalin app;
    private static final int TEST_PORT = 7777;
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    public static void setUpAll() {
        System.setProperty("test", "true");
        System.setProperty("JWT_SECRET", "test-secret-long-enough-for-hmac-256");
        System.setProperty("JWT_SECRET", "test-secret");
        HibernateConfig.getEntityManagerFactory();

        try (var em = HibernateConfig.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE progress, exercises, lessons, courses, user_roles, users CASCADE").executeUpdate();
            em.getTransaction().commit();
        }

        app = ApplicationConfig.createApp();
        app.start(TEST_PORT);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = TEST_PORT;

        given()
                .contentType("application/json")
                .body("{\"username\":\"admin\",\"email\":\"admin@test.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        adminToken = given()
                .contentType("application/json")
                .body("{\"email\":\"admin@test.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        given()
                .contentType("application/json")
                .body("{\"username\":\"regularuser\",\"email\":\"user@test.com\",\"password\":\"user123\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        userToken = given()
                .contentType("application/json")
                .body("{\"email\":\"user@test.com\",\"password\":\"user123\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    @AfterAll
    public static void tearDownAll() {
        app.stop();
    }

    @Test
    public void testRegisterNewUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"newuser\",\"email\":\"new@test.com\",\"password\":\"pass123\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("username", equalTo("newuser"));
    }

    @Test
    public void testRegisterDuplicateEmail() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"other\",\"email\":\"admin@test.com\",\"password\":\"pass123\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    public void testLoginSuccess() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"admin@test.com\",\"password\":\"admin123\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("userId", notNullValue());
    }

    @Test
    public void testLoginWrongPassword() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"admin@test.com\",\"password\":\"wrongpassword\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    public void testProtectedRouteWithoutToken() {
        given()
                .when()
                .get("/api/courses")
                .then()
                .statusCode(401);
    }

    @Test
    public void testProtectedRouteWithToken() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/courses")
                .then()
                .statusCode(200);
    }

    @Test
    public void testCreateCourse() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\"name\":\"Test Course\",\"programmingLanguage\":\"Java\",\"description\":\"Test\",\"difficulty\":\"BEGINNER\"}")
                .when()
                .post("/api/courses")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Course"))
                .body("difficulty", equalTo("BEGINNER"));
    }

    @Test
    public void testGetCourseById() {
        int id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\"name\":\"Find Me\",\"programmingLanguage\":\"Python\",\"description\":\"Test\",\"difficulty\":\"ADVANCED\"}")
                .when()
                .post("/api/courses")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/courses/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Find Me"));
    }

    @Test
    public void testGetCourseNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/courses/999999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testInvalidIdFormat() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/courses/abc")
                .then()
                .statusCode(400);
    }

    @Test
    public void testStackOverflowPublicRoute() {
        given()
                .when()
                .get("/api/stackoverflow/questions?tag=java&size=5")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    public void testUpdateCourse() {
        int id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\"name\":\"Update Me\",\"programmingLanguage\":\"Java\",\"description\":\"Test\",\"difficulty\":\"BEGINNER\"}")
                .when()
                .post("/api/courses")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\"name\":\"Updated\",\"programmingLanguage\":\"Java\",\"description\":\"Test\",\"difficulty\":\"BEGINNER\"}")
                .when()
                .put("/api/courses/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated"));
    }

    @Test
    public void testDeleteCourse() {
        int id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\"name\":\"Delete Me\",\"programmingLanguage\":\"Java\",\"description\":\"Test\",\"difficulty\":\"BEGINNER\"}")
                .when()
                .post("/api/courses")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/api/courses/" + id)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/courses/" + id)
                .then()
                .statusCode(404);
    }
}