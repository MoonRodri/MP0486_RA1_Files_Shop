package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.Amount;
import model.Employee;
import model.Product;

public class DaoImplJDBC implements Dao {
    Connection connection;

    @Override
    public void connect() {
        // Define connection parameters
        String url = "jdbc:mysql://localhost:3306/shop";
        String user = "root";
        String pass = "";
        try {
            this.connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[DaoImplJDBC] Connected to database: " + url);
        } catch (SQLException e) {
            System.err.println("[DaoImplJDBC] Failed to connect to database: " + e.getMessage());
            this.connection = null;
        }

    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DaoImplJDBC] Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        Employee employee = null;
        String query = "select * from employee where employeeId= ? and password = ? ";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            ps.setString(2, password);
            //System.out.println(ps.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    employee = new Employee(rs.getInt(1), rs.getString(2), rs.getString(3));
                }
            }
        } catch (SQLException e) {
            // in case error in SQL
            e.printStackTrace();
        }
        return employee;
    }

    @Override
    public ArrayList<Product> getInventory() {
        ArrayList<Product> inventory = new ArrayList<>();
        if (this.connection == null) {
            // try to establish connection lazily
            System.out.println("[DaoImplJDBC] Connection is null, attempting to connect...");
            this.connect();
            if (this.connection == null) {
                System.err.println("[DaoImplJDBC] Cannot load inventory because DB connection is not available.");
                return inventory; // empty
            }
        }

        String query = "SELECT id, name, wholesalerPrice, available, stock FROM inventory";
        int maxId = 0;
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double wholesaler = rs.getDouble("wholesalerPrice");
                boolean available = rs.getBoolean("available");
                int stock = rs.getInt("stock");

                Product p = new Product(name, new Amount(wholesaler), available, stock);
                p.setId(id);
                inventory.add(p);
                if (id > maxId) maxId = id;
            }
            // synchronize Product.totalProducts with max id from DB to avoid id clashes
            if (maxId > 0) {
                Product.setTotalProducts(maxId);
            }
            System.out.println("[DaoImplJDBC] Loaded inventory size=" + inventory.size());
        } catch (SQLException e) {
            System.err.println("[DaoImplJDBC] Error loading inventory: " + e.getMessage());
            e.printStackTrace();
        }
        return inventory;
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventory) {
        if (this.connection == null) {
            System.out.println("[DaoImplJDBC] Connection is null, attempting to connect before writeInventory...");
            this.connect();
            if (this.connection == null) {
                System.err.println("[DaoImplJDBC] Cannot write historical inventory because DB connection is not available.");
                return false;
            }
        }

        String insert = "INSERT INTO historical_inventory (id_product, name, wholesalerPrice, available, stock, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            for (Product p : inventory) {
                ps.setInt(1, p.getId());
                ps.setString(2, p.getName());
                ps.setDouble(3, p.getWholesalerPrice().getValue());
                ps.setBoolean(4, p.isAvailable());
                ps.setInt(5, p.getStock());
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void addProduct(Product product) {
        if (this.connection == null) {
            this.connect();
            if (this.connection == null) {
                System.err.println("[DaoImplJDBC] Cannot add product, DB connection not available.");
                return;
            }
        }
        String insert = "INSERT INTO inventory (name, wholesalerPrice, available, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setDouble(2, product.getWholesalerPrice().getValue());
            ps.setBoolean(3, product.isAvailable());
            ps.setInt(4, product.getStock());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    product.setId(generatedId);
                    // update Product.totalProducts to reflect DB state
                    if (generatedId > Product.getTotalProducts()) {
                        Product.setTotalProducts(generatedId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateProduct(Product product) {
        if (this.connection == null) {
            this.connect();
            if (this.connection == null) {
                System.err.println("[DaoImplJDBC] Cannot update product, DB connection not available.");
                return;
            }
        }
        String update = "UPDATE inventory SET stock = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setInt(1, product.getStock());
            ps.setInt(2, product.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteProduct(Product product) {
        if (this.connection == null) {
            this.connect();
            if (this.connection == null) {
                System.err.println("[DaoImplJDBC] Cannot delete product, DB connection not available.");
                return;
            }
        }
        String delete = "DELETE FROM inventory WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(delete)) {
            ps.setInt(1, product.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}