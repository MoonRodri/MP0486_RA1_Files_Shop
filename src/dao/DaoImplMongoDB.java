package dao;

import model.Amount;
import model.Employee;
import model.Product;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class DaoImplMongoDB implements Dao {

    // use reflection so code compiles if mongo-java-driver is not on the classpath
    private Object mongoClientObj = null; // com.mongodb.MongoClient
    private Object dbObj = null; // com.mongodb.DB
    private boolean driverAvailable = true;

    @Override
    public void connect() {
        if (mongoClientObj != null) return;
        try {
            Class<?> mcClass = Class.forName("com.mongodb.MongoClient");
            Constructor<?> ctor = mcClass.getConstructor(String.class, int.class);
            mongoClientObj = ctor.newInstance("localhost", 27017);
            Method getDB = mcClass.getMethod("getDB", String.class);
            dbObj = getDB.invoke(mongoClientObj, "shop");
            driverAvailable = true;
        } catch (ClassNotFoundException cnfe) {
            driverAvailable = false;
            mongoClientObj = null;
            dbObj = null;
        } catch (Exception e) {
            driverAvailable = false;
            mongoClientObj = null;
            dbObj = null;
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (mongoClientObj != null) {
            try {
                Method close = mongoClientObj.getClass().getMethod("close");
                close.invoke(mongoClientObj);
            } catch (Exception e) {
                // ignore
            }
            mongoClientObj = null;
            dbObj = null;
        }
    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        if (!isMongoReachable()) return null;
        connect();
        if (!driverAvailable || dbObj == null) return null;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object usersColl = getCollection.invoke(dbObj, "users");
            Class<?> basicDBObjectClass = Class.forName("com.mongodb.BasicDBObject");
            Object query = basicDBObjectClass.getConstructor().newInstance();
            Method put = basicDBObjectClass.getMethod("put", String.class, Object.class);
            put.invoke(query, "employeeId", employeeId);
            put.invoke(query, "password", password);
            Method findOne = usersColl.getClass().getMethod("findOne", Class.forName("com.mongodb.DBObject"));
            Object found = findOne.invoke(usersColl, query);
            if (found != null) {
                Method get = found.getClass().getMethod("get", String.class);
                Object nameObj = get.invoke(found, "name");
                String name = nameObj != null ? nameObj.toString() : "";
                return new Employee(employeeId, name, password);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ArrayList<Product> getInventory() {
        ArrayList<Product> list = new ArrayList<>();
        if (!isMongoReachable()) return list;
        connect();
        if (!driverAvailable || dbObj == null) return list;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object inventoryColl = getCollection.invoke(dbObj, "inventory");
            Method find = inventoryColl.getClass().getMethod("find");
            Object cursor = find.invoke(inventoryColl);
            Class<?> cursorClass = Class.forName("com.mongodb.DBCursor");
            Method hasNext = cursorClass.getMethod("hasNext");
            Method next = cursorClass.getMethod("next");
            while ((Boolean) hasNext.invoke(cursor)) {
                Object obj = next.invoke(cursor);
                Product p = new Product();
                Method get = obj.getClass().getMethod("get", String.class);
                Object idObj = get.invoke(obj, "id");
                if (idObj != null) {
                    int id = (idObj instanceof Number) ? ((Number) idObj).intValue() : Integer.parseInt(idObj.toString());
                    p.setId(id);
                }
                Object nameObj = get.invoke(obj, "name");
                if (nameObj != null) p.setName(nameObj.toString());
                Object availableObj = get.invoke(obj, "available");
                if (availableObj != null) p.setAvailable(Boolean.parseBoolean(availableObj.toString()));
                Object stockObj = get.invoke(obj, "stock");
                if (stockObj != null) {
                    int stock = (stockObj instanceof Number) ? ((Number) stockObj).intValue() : Integer.parseInt(stockObj.toString());
                    p.setStock(stock);
                }
                Object wholesalerObj = get.invoke(obj, "wholesalerPrice");
                if (wholesalerObj != null) {
                    Method wpGet = wholesalerObj.getClass().getMethod("get", String.class);
                    Object valueObj = wpGet.invoke(wholesalerObj, "value");
                    double value = 0.0;
                    if (valueObj != null) value = (valueObj instanceof Number) ? ((Number) valueObj).doubleValue() : Double.parseDouble(valueObj.toString());
                    p.setWholesalerPrice(new Amount(value));
                }
                list.add(p);
            }
            try { Method close = cursor.getClass().getMethod("close"); close.invoke(cursor); } catch (Exception ex) { }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventoryList) {
        // first check TCP availability
        if (!isMongoReachable()) return false;
        connect();
        if (!driverAvailable || dbObj == null) return false;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object histColl = getCollection.invoke(dbObj, "historical_inventory");
            Class<?> basicDBObjectClass = Class.forName("com.mongodb.BasicDBObject");
            Method remove = histColl.getClass().getMethod("remove", Class.forName("com.mongodb.DBObject"));
            Object empty = basicDBObjectClass.getConstructor().newInstance();
            remove.invoke(histColl, empty);
            // prefer the overload that accepts a java.util.List to avoid varargs/NoSuchMethod reflection issues
            Method insert = histColl.getClass().getMethod("insert", Class.forName("java.util.List"));
            Method put = basicDBObjectClass.getMethod("put", String.class, Object.class);
            for (Product p : inventoryList) {
                Object doc = basicDBObjectClass.getConstructor().newInstance();
                put.invoke(doc, "id", p.getId());
                put.invoke(doc, "name", p.getName());
                Object wp = basicDBObjectClass.getConstructor().newInstance();
                double value = (p.getWholesalerPrice() != null) ? p.getWholesalerPrice().getValue() : 0.0;
                put.invoke(wp, "value", value);
                put.invoke(wp, "currency", "€");
                put.invoke(doc, "wholesalerPrice", wp);
                put.invoke(doc, "available", p.isAvailable());
                put.invoke(doc, "stock", p.getStock());
                put.invoke(doc, "created_at", new Date());
                insert.invoke(histColl, java.util.Arrays.asList(doc));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void addProduct(Product product) {
        if (!isMongoReachable()) return;
        connect();
        if (!driverAvailable || dbObj == null) return;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object inventoryColl = getCollection.invoke(dbObj, "inventory");
            Class<?> basicDBObjectClass = Class.forName("com.mongodb.BasicDBObject");
            Object doc = basicDBObjectClass.getConstructor().newInstance();
            Method put = basicDBObjectClass.getMethod("put", String.class, Object.class);
            put.invoke(doc, "id", product.getId());
            put.invoke(doc, "name", product.getName());
            Object wp = basicDBObjectClass.getConstructor().newInstance();
            double value = (product.getWholesalerPrice() != null) ? product.getWholesalerPrice().getValue() : 0.0;
            put.invoke(wp, "value", value);
            put.invoke(wp, "currency", "€");
            put.invoke(doc, "wholesalerPrice", wp);
            put.invoke(doc, "available", product.isAvailable());
            put.invoke(doc, "stock", product.getStock());
            // prefer the overload that accepts a java.util.List to avoid varargs/NoSuchMethod reflection issues
            Method insert = inventoryColl.getClass().getMethod("insert", Class.forName("java.util.List"));
            insert.invoke(inventoryColl, java.util.Arrays.asList(doc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateProduct(Product product) {
        if (!isMongoReachable()) return;
        connect();
        if (!driverAvailable || dbObj == null) return;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object inventoryColl = getCollection.invoke(dbObj, "inventory");
            Class<?> basicDBObjectClass = Class.forName("com.mongodb.BasicDBObject");
            Object query = basicDBObjectClass.getConstructor().newInstance();
            Method put = basicDBObjectClass.getMethod("put", String.class, Object.class);
            put.invoke(query, "id", product.getId());
            Object updateObj = basicDBObjectClass.getConstructor().newInstance();
            Object setObj = basicDBObjectClass.getConstructor().newInstance();
            put.invoke(setObj, "stock", product.getStock());
            Method update = inventoryColl.getClass().getMethod("update", Class.forName("com.mongodb.DBObject"), Class.forName("com.mongodb.DBObject"));
            put.invoke(updateObj, "$set", setObj);
            update.invoke(inventoryColl, query, updateObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteProduct(Product product) {
        if (!isMongoReachable()) return;
        connect();
        if (!driverAvailable || dbObj == null) return;
        try {
            Class<?> dbClass = Class.forName("com.mongodb.DB");
            Method getCollection = dbClass.getMethod("getCollection", String.class);
            Object inventoryColl = getCollection.invoke(dbObj, "inventory");
            Class<?> basicDBObjectClass = Class.forName("com.mongodb.BasicDBObject");
            Object query = basicDBObjectClass.getConstructor().newInstance();
            Method put = basicDBObjectClass.getMethod("put", String.class, Object.class);
            put.invoke(query, "id", product.getId());
            Method remove = inventoryColl.getClass().getMethod("remove", Class.forName("com.mongodb.DBObject"));
            remove.invoke(inventoryColl, query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isMongoReachable() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 27017), 1500);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) try { socket.close(); } catch (Exception ex) { }
        }
    }

}