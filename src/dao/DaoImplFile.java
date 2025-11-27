package dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;

import model.Amount;
import model.Employee;
import model.Product;

public class DaoImplFile implements Dao {

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public Employee getEmployee(int employeeId, String password) {
        // Not implemented for file-based DAO
        return null;
    }

    @Override
    public ArrayList<Product> getInventory() {
        ArrayList<Product> inventory = new ArrayList<>();
        File f = new File(System.getProperty("user.dir") + File.separator + "files" + File.separator + "inputInventory.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] sections = line.split(";");
                String name = "";
                double wholesalerPrice = 0.0;
                int stock = 0;

                for (String section : sections) {
                    if (!section.contains(":")) continue;
                    String[] kv = section.split(":", 2);
                    String key = kv[0].trim();
                    String value = kv[1].trim();

                    switch (key) {
                        case "Product":
                            name = value;
                            break;
                        case "Wholesaler Price":
                        case "WholesalerPrice":
                        case "Price":
                            try {
                                wholesalerPrice = Double.parseDouble(value);
                            } catch (NumberFormatException ignored) { /* keep default */ }
                            break;
                        case "Stock":
                            try {
                                stock = Integer.parseInt(value);
                            } catch (NumberFormatException ignored) { /* keep default */ }
                            break;
                        default:
                            // ignore unknown keys (e.g., leading counter)
                            break;
                    }
                }

                inventory.add(new Product(name, new Amount(wholesalerPrice), true, stock));
            }
        } catch (FileNotFoundException e) {
            System.err.println(" ! El archivo no fue encontrado en el inventario: " + f.getAbsolutePath());
        } catch (IOException e) {
            System.err.println(" ! Error al leer el archivo de inventario.");
            e.printStackTrace();
        }

        return inventory;
    }

    @Override
    public boolean writeInventory(ArrayList<Product> inventory) {
        LocalDate today = LocalDate.now();
        String fileName = "inventory_" + today.toString() + ".txt";
        File f = new File(System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName);

        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            int counter = 1;
            for (Product product : inventory) {
                pw.println(counter + ";Product:" + product.getName() + ";Stock:" + product.getStock() + ";");
                counter++;
            }
            pw.println("Total number of products:" + inventory.size() + ";");
            return true;
        } catch (IOException e) {
            System.err.println(" ! Error exportando el inventario: " + e.getMessage());
            return false;
        }
    }

    // Empty implementations to satisfy new Dao interface methods. Kept intentionally blank for file-based DAO.
    @Override
    public void addProduct(Product product) {
        // no-op for file DAO
    }

    @Override
    public void updateProduct(Product product) {
        // no-op for file DAO
    }

    @Override
    public void deleteProduct(Product product) {
        // no-op for file DAO
    }
}