package dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import model.Amount;
import model.Employee;
import model.Product;

import java.util.ArrayList;
import java.util.Date;

public class DaoImplMongoDB implements Dao {

    private MongoClient mongoClient;
    private DB db;

    @Override
    public void connect() {
        if (mongoClient == null) {
            mongoClient = new MongoClient("localhost", 27017);
            db = mongoClient.getDB("shop");
        }
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            db = null;
        }
    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        if (db == null) {
            connect();
        }
        DBCollection users = db.getCollection("users");
        BasicDBObject query = new BasicDBObject();
        query.put("employeeId", employeeId);
        query.put("password", password);
        DBObject found = users.findOne(query);
        if (found != null) {
            String name = "";
            Object nameObj = found.get("name");
            if (nameObj != null) {
                name = nameObj.toString();
            }
            return new Employee(employeeId, name, password);
        }
        return null;
    }

    @Override
    public ArrayList<Product> getInventory() {
        ArrayList<Product> list = new ArrayList<>();
        if (db == null) {
            connect();
        }
        DBCollection inventory = db.getCollection("inventory");
        DBCursor cursor = null;
        try {
            cursor = inventory.find();
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                Product p = new Product();
                Object idObj = obj.get("id");
                if (idObj != null) {
                    int id = (idObj instanceof Number) ? ((Number) idObj).intValue() : Integer.parseInt(idObj.toString());
                    p.setId(id);
                }
                Object nameObj = obj.get("name");
                if (nameObj != null) {
                    p.setName(nameObj.toString());
                }
                Object availableObj = obj.get("available");
                if (availableObj != null) {
                    p.setAvailable(Boolean.parseBoolean(availableObj.toString()));
                }
                Object stockObj = obj.get("stock");
                if (stockObj != null) {
                    int stock = (stockObj instanceof Number) ? ((Number) stockObj).intValue() : Integer.parseInt(stockObj.toString());
                    p.setStock(stock);
                }
                // wholesalerPrice is nested document with 'value' and 'currency'
                Object wholesalerObj = obj.get("wholesalerPrice");
                if (wholesalerObj instanceof DBObject) {
                    DBObject wp = (DBObject) wholesalerObj;
                    Object valueObj = wp.get("value");
                    double value = 0.0;
                    if (valueObj != null) {
                        value = (valueObj instanceof Number) ? ((Number) valueObj).doubleValue() : Double.parseDouble(valueObj.toString());
                    }
                    p.setWholesalerPrice(new Amount(value));
                }
                list.add(p);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventoryList) {
        if (db == null) {
            connect();
        }
        DBCollection hist = db.getCollection("historical_inventory");
        try {
            // clear collection before exporting
            hist.remove(new BasicDBObject());
            for (Product p : inventoryList) {
                BasicDBObject doc = new BasicDBObject();
                doc.put("id", p.getId());
                doc.put("name", p.getName());
                BasicDBObject wp = new BasicDBObject();
                double value = (p.getWholesalerPrice() != null) ? p.getWholesalerPrice().getValue() : 0.0;
                wp.put("value", value);
                wp.put("currency", "€");
                doc.put("wholesalerPrice", wp);
                doc.put("available", p.isAvailable());
                doc.put("stock", p.getStock());
                doc.put("created_at", new Date());
                hist.insert(doc);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void addProduct(Product product) {
        if (db == null) {
            connect();
        }
        DBCollection inventory = db.getCollection("inventory");
        BasicDBObject doc = new BasicDBObject();
        doc.put("id", product.getId());
        doc.put("name", product.getName());
        double value = (product.getWholesalerPrice() != null) ? product.getWholesalerPrice().getValue() : 0.0;
        BasicDBObject wp = new BasicDBObject();
        wp.put("value", value);
        wp.put("currency", "€");
        doc.put("wholesalerPrice", wp);
        doc.put("available", product.isAvailable());
        doc.put("stock", product.getStock());
        inventory.insert(doc);
    }

    @Override
    public void updateProduct(Product product) {
        if (db == null) {
            connect();
        }
        DBCollection inventory = db.getCollection("inventory");
        BasicDBObject query = new BasicDBObject("id", product.getId());
        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("stock", product.getStock()));
        inventory.update(query, update);
    }

    @Override
    public void deleteProduct(Product product) {
        if (db == null) {
            connect();
        }
        DBCollection inventory = db.getCollection("inventory");
        BasicDBObject query = new BasicDBObject("id", product.getId());
        inventory.remove(query);
    }

}
