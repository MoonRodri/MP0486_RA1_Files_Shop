package dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import model.Product;
import model.ProductHistory;
import model.Employee;

public class DaoImplHibernate implements Dao {

    private SessionFactory sessionFactory;

    @Override
    public void connect() {
        try {
            if (this.sessionFactory == null) {
                Configuration cfg = new Configuration();
                cfg.configure(); // reads hibernate.cfg.xml from classpath
                this.sessionFactory = cfg.buildSessionFactory();
                System.out.println("[DaoImplHibernate] SessionFactory created");
            }
        } catch (HibernateException ex) {
            System.err.println("[DaoImplHibernate] Failed to create SessionFactory: " + ex.getMessage());
            this.sessionFactory = null;
        }
    }

    @Override
    public void disconnect() {
        if (this.sessionFactory != null) {
            try {
                this.sessionFactory.close();
                this.sessionFactory = null;
                System.out.println("[DaoImplHibernate] SessionFactory closed");
            } catch (HibernateException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        // Employee management not implemented yet
        return null;
    }

    @Override
    public ArrayList<Product> getInventory() {
        ArrayList<Product> inventory = new ArrayList<>();
        if (this.sessionFactory == null) {
            connect();
            if (this.sessionFactory == null) {
                System.err.println("[DaoImplHibernate] Cannot load inventory because SessionFactory is not available.");
                return inventory;
            }
        }

        Session session = null;
        try {
            session = sessionFactory.openSession();
            List<Product> list = session.createQuery("from Product", Product.class).list();
            if (list != null) {
                inventory.addAll(list);
            }
            // synchronize Product.totalProducts with max id
            int maxId = 0;
            for (Product p : inventory) {
                if (p != null && p.getId() > maxId) maxId = p.getId();
            }
            if (maxId > 0) {
                Product.setTotalProducts(maxId);
            }
            System.out.println("[DaoImplHibernate] Loaded inventory size=" + inventory.size());
        } catch (HibernateException ex) {
            System.err.println("[DaoImplHibernate] Error loading inventory: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (session != null) session.close();
        }

        return inventory;
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventory) {
        if (this.sessionFactory == null) {
            connect();
            if (this.sessionFactory == null) {
                System.err.println("[DaoImplHibernate] Cannot write historical inventory because SessionFactory is not available.");
                return false;
            }
        }

        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            for (Product p : inventory) {
                ProductHistory ph = new ProductHistory();
                ph.setIdProduct(p.getId());
                ph.setName(p.getName());
                ph.setStock(p.getStock());
                ph.setPrice(p.getWholesalerPrice() != null ? p.getWholesalerPrice().getValue() : 0.0);
                ph.setAvailable(p.isAvailable());
                ph.setCreatedAt(now);
                session.save(ph);
            }
            tx.commit();
            return true;
        } catch (HibernateException ex) {
            if (tx != null) tx.rollback();
            System.err.println("[DaoImplHibernate] Error writing historical inventory: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public void addProduct(Product product) {
        if (this.sessionFactory == null) {
            connect();
            if (this.sessionFactory == null) {
                System.err.println("[DaoImplHibernate] Cannot add product, SessionFactory not available.");
                return;
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.save(product);
            tx.commit();
        } catch (HibernateException ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public void updateProduct(Product product) {
        if (this.sessionFactory == null) {
            connect();
            if (this.sessionFactory == null) {
                System.err.println("[DaoImplHibernate] Cannot update product, SessionFactory not available.");
                return;
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.merge(product);
            tx.commit();
        } catch (HibernateException ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public void deleteProduct(Product product) {
        if (this.sessionFactory == null) {
            connect();
            if (this.sessionFactory == null) {
                System.err.println("[DaoImplHibernate] Cannot delete product, SessionFactory not available.");
                return;
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            // ensure managed instance
            Product managed = session.get(Product.class, product.getId());
            if (managed != null) {
                session.delete(managed);
            }
            tx.commit();
        } catch (HibernateException ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            if (session != null) session.close();
        }
    }

}
