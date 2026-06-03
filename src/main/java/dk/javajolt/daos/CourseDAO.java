package dk.javajolt.daos;
import dk.javajolt.entities.Course;
import jakarta.persistence.EntityManager;
import java.util.List;
public class CourseDAO extends BaseDAO<Course> {
    private static volatile CourseDAO instance;
    private CourseDAO() {
        super(Course.class);
    }
    public static synchronized CourseDAO getInstance() {
        if (instance == null) {
            instance = new CourseDAO();
        }
        return instance;
    }

    public List<Course> findByLanguage(String language) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Course c WHERE c.programmingLanguage = :language AND c.isDeleted = false", Course.class)
                    .setParameter("language", language)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    public List<Course> findByDifficulty(String difficulty) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Course c WHERE c.difficulty = :difficulty AND c.isDeleted = false", Course.class)
                    .setParameter("difficulty", Course.Difficulty.valueOf(difficulty.toUpperCase()))
                    .getResultList();
        } finally {
            em.close();
        }
    }
}