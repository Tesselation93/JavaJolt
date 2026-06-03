package dk.javajolt.daos;
import dk.javajolt.entities.Role;
import dk.javajolt.entities.User;
import jakarta.persistence.EntityManager;

public class UserDAO extends BaseDAO<User> {
    private static volatile UserDAO instance;
    private UserDAO() {
        super(User.class);
    }

    public static synchronized UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    public User findByEmail(String email) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
    public User findByUsername(String username) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
    public Role createRole(String roleName) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Role existing = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", roleName.toUpperCase())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (existing != null) return existing;
            Role role = new Role(roleName);
            em.persist(role);
            em.getTransaction().commit();
            return role;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to create role: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
    public User addRole(String username, String roleName) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            Role role = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", roleName.toUpperCase())
                    .getSingleResult();
            user.addRole(role);
            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to add role: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}