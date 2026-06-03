package dk.javajolt.util;
import dk.javajolt.config.HibernateConfig;
import dk.javajolt.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
public class DataSeeder {
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final EntityManagerFactory emf;
    public DataSeeder() {
        this.emf = HibernateConfig.getEntityManagerFactory();
    }
    public void seed() {
        EntityManager em = emf.createEntityManager();
        Long userCount = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        em.close();
        if (userCount > 0) {
            logger.info("Database already seeded - skipping");
            return;
        }
        logger.info("Seeding database...");
        seedRoles();
        User admin = seedUser("admin", "admin@javajolt.com", "admin123");
        addRoleToUser(admin.getId(), "ADMIN");
        addRoleToUser(admin.getId(), "SUPER_ADMIN");
        User alice = seedUser("alice", "alice@example.com", "password123");
        User bob = seedUser("bob", "bob@example.com", "password123");
        Course c1 = seedCourse("Java Fundamentals", "Java", "Learn the basics of Java", Course.Difficulty.BEGINNER);
        Course c2 = seedCourse("Object Oriented Programming", "Java", "Deep dive into OOP", Course.Difficulty.INTERMEDIATE);
        Course c3 = seedCourse("Advanced Java Patterns", "Java", "Design patterns and best practices", Course.Difficulty.ADVANCED);
        Lesson l1 = seedLesson("Variables and Types", "Every variable in Java must be declared with a type.", 1, c1);
        Lesson l2 = seedLesson("Control Flow", "If statements, loops and switch expressions.", 2, c1);
        Lesson l3 = seedLesson("Classes and Objects", "Creating and using classes in Java.", 1, c2);
        Lesson l4 = seedLesson("Inheritance", "Extending classes and overriding methods.", 2, c2);
        Exercise e1 = seedExercise("Declare a String", "Declare a String variable called name", "String name = ___;", "String name = \"Alice\";", "BEGINNER", l1);
        Exercise e2 = seedExercise("Fix the loop", "This for loop never runs - fix it", "for (int i = 10; i < 5; i++) {}", "for (int i = 0; i < 5; i++) {}", "BEGINNER", l2);
        Exercise e3 = seedExercise("Create a class", "Create a class called Animal with a name field", "class ___ { }", "class Animal { String name; }", "INTERMEDIATE", l3);
        Exercise e4 = seedExercise("Override toString", "Override toString to return the animal name", "public String toString() { return ___; }", "public String toString() { return name; }", "INTERMEDIATE", l4);
        seedProgress(alice, e1, true, 100);
        seedProgress(alice, e2, true, 85);
        seedProgress(alice, e3, false, null);
        seedProgress(bob, e1, true, 90);
        seedProgress(bob, e2, false, null);
        logger.info("Seeding complete: 3 users, 3 courses, 4 lessons, 4 exercises, 5 progress records");
    }
    private void seedRoles() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (String role : new String[]{"USER", "ADMIN", "SUPER_ADMIN"}) {
            Long count = em.createQuery("SELECT COUNT(r) FROM Role r WHERE r.name = :name", Long.class).setParameter("name", role).getSingleResult();
            if (count == 0) em.persist(new Role(role));
        }
        em.getTransaction().commit();
        em.close();
    }
    private User seedUser(String username, String email, String password) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        User user = new User(username, email, password, false);
        em.persist(user);
        em.getTransaction().commit();
        Long id = user.getId();
        em.close();
        addRoleToUser(id, "USER");
        return user;
    }
    private void addRoleToUser(Long userId, String roleName) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        User user = em.find(User.class, userId);
        Role role = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class).setParameter("name", roleName).getSingleResult();
        user.addRole(role);
        em.getTransaction().commit();
        em.close();
    }
    private Course seedCourse(String name, String language, String description, Course.Difficulty difficulty) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Course course = new Course(name, language, description, difficulty);
        em.persist(course);
        em.getTransaction().commit();
        em.close();
        return course;
    }
    private Lesson seedLesson(String title, String content, int order, Course course) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Course managed = em.find(Course.class, course.getId());
        Lesson lesson = new Lesson(title, content, order, managed);
        em.persist(lesson);
        em.getTransaction().commit();
        em.close();
        return lesson;
    }
    private Exercise seedExercise(String title, String description, String starter, String solution, String difficulty, Lesson lesson) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Lesson managed = em.find(Lesson.class, lesson.getId());
        Exercise exercise = new Exercise(title, description, starter, solution, difficulty, managed);
        em.persist(exercise);
        em.getTransaction().commit();
        em.close();
        return exercise;
    }
    private void seedProgress(User user, Exercise exercise, boolean completed, Integer score) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        User u = em.find(User.class, user.getId());
        Exercise ex = em.find(Exercise.class, exercise.getId());
        Progress progress = new Progress(u, ex);
        progress.setCompleted(completed);
        if (score != null) progress.setScore(score);
        if (completed) progress.setCompletedAt(LocalDateTime.now());
        em.persist(progress);
        em.getTransaction().commit();
        em.close();
    }
}
