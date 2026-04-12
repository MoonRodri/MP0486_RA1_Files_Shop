// filepath: c:\Users\Rodri\git\MP0486_RA1_Files_Shop\src\dao\DaoImplObjectDB.java
package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import model.Employee;
import model.Product;

/**
 * Lightweight Dao implementation used as a placeholder for ObjectDB-backed DAO.
 * For now it delegates to the file-based DAO for inventory operations and
 * provides a very small in-memory employee lookup compatible with the
 * application's existing test credentials.
 */
public class DaoImplObjectDB implements Dao {

    private EntityManagerFactory emf;
    private EntityManager em;

    
    @Override
    public void connect() {
        emf = Persistence.createEntityManagerFactory("objects/users.odb");
        em = emf.createEntityManager();
        
        // --- SCRIPT TEMPORAL SEGURO ---
        try {
            // Intentamos ver si hay empleados
            long count = em.createQuery("SELECT COUNT(e) FROM Employee e", Long.class).getSingleResult();
            if (count == 0) {
                em.getTransaction().begin();
                em.persist(new Employee(8888, "Admin", "0088"));
                em.getTransaction().commit();
                System.out.println("Usuario de prueba creado en ObjectDB (Count 0).");
            }
        } catch (Exception e) {
            // Si salta un error (ej. la tabla no existe aún), forzamos la creación
            try {
                em.getTransaction().begin();
                em.persist(new Employee(8888, "Admin", "0088"));
                em.getTransaction().commit();
                System.out.println("Usuario de prueba creado en ObjectDB (Tras crear esquema).");
            } catch (Exception ex) {
                System.out.println("Error grave al persistir: " + ex.getMessage());
            }
        }
        // -----------------------------------------
    }

    @Override
    public void disconnect() {
        if (em != null) {
            try {
                if (em.isOpen()) em.close();
            } catch (Exception ignored) {}
            em = null;
        }
        if (emf != null) {
            try {
                emf.close();
            } catch (Exception ignored) {}
            emf = null;
        }
    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        if (em == null || !em.isOpen()) {
            connect();
            if (em == null) return null;
        }
        try {
            TypedQuery<Employee> q = em.createQuery(
                    "SELECT e FROM Employee e WHERE e.employeeId = :id AND e.password = :pw", Employee.class);
            q.setParameter("id", employeeId);
            q.setParameter("pw", password);
            return q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public ArrayList<Product> getInventory() {
        // Implementation left as default (not yet using ObjectDB for products)
        return new ArrayList<>();
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventory) {
        // Not implemented for ObjectDB DAO at the moment
        return false;
    }

    @Override
    public void addProduct(Product product) {
        // no-op for now
    }

    @Override
    public void updateProduct(Product product) {
        // no-op for now
    }

    @Override
    public void deleteProduct(Product product) {
        // no-op for now
    }
}