package dao;

import model.Employee;
import java.util.ArrayList;
import model.Product;

public interface Dao {
	
	public void connect();

	public void disconnect();

	public Employee getEmployee(int employeeId, String password);
	
	public ArrayList<Product> getInventory();
	
	public boolean writeInventory(ArrayList<Product> inventory);

	public void addProduct(Product product);

	public void updateProduct(Product product);

	public void deleteProduct(Product product);
}