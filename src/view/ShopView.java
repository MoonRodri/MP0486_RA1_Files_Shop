package view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import main.Shop;
import utils.Constants;

public class ShopView extends JFrame implements ActionListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	private Shop shop;

	private JPanel contentPane;
	private JButton btnShowCash;
	private JButton btnAddProduct;
	private JButton btnAddStock;
	private JButton btnRemoveProduct;
	private JButton btnViewInventory;
	private JButton btnExportInventory;

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ShopView frame = new ShopView();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ShopView()
	{
		setTitle("MiTienda.com - Menú principal");
		addKeyListener(this);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);

		shop = new Shop();
		shop.loadInventory();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblShowCash = new JLabel("Seleccione o pulse una opción:");
		lblShowCash.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblShowCash.setBounds(57, 20, 300, 14);
		contentPane.add(lblShowCash);

		btnExportInventory = new JButton("0. Exportar el inventario");
		btnExportInventory.setHorizontalAlignment(SwingConstants.LEFT);
		btnExportInventory.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnExportInventory.setBounds(99, 40, 236, 40);
		btnExportInventory.addActionListener(this);
		contentPane.add(btnExportInventory);

		btnShowCash = new JButton("1. Contar caja");
		btnShowCash.setHorizontalAlignment(SwingConstants.LEFT);
		btnShowCash.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnShowCash.setBounds(99, 90, 236, 40);
		btnShowCash.addActionListener(this);
		contentPane.add(btnShowCash);

		btnAddProduct = new JButton("2. Añadir producto");
		btnAddProduct.setHorizontalAlignment(SwingConstants.LEFT);
		btnAddProduct.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnAddProduct.setBounds(99, 140, 236, 40);
		btnAddProduct.addActionListener(this);
		contentPane.add(btnAddProduct);

		btnAddStock = new JButton("3. Añadir stock");
		btnAddStock.setHorizontalAlignment(SwingConstants.LEFT);
		btnAddStock.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnAddStock.setBounds(99, 190, 236, 40);
		btnAddStock.addActionListener(this);
		contentPane.add(btnAddStock);

		btnViewInventory = new JButton("4. Ver inventario");
		btnViewInventory.setHorizontalAlignment(SwingConstants.LEFT);
		btnViewInventory.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnViewInventory.setBounds(99, 240, 236, 40);
		btnViewInventory.addActionListener(this);
		contentPane.add(btnViewInventory);

		btnRemoveProduct = new JButton("9. Eliminar producto");
		btnRemoveProduct.setHorizontalAlignment(SwingConstants.LEFT);
		btnRemoveProduct.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnRemoveProduct.setBounds(99, 290, 236, 40);
		btnRemoveProduct.addActionListener(this);
		contentPane.add(btnRemoveProduct);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		char c = e.getKeyChar();
		if (c == '0')
			exportInventory();
		if (c == '1')
			openCashView();
		if (c == '2')
			openProductView(Constants.OPTION_ADD_PRODUCT);
		if (c == '3')
			openProductView(Constants.OPTION_ADD_STOCK);
		if (c == '4')
			openInventoryView();
		if (c == '9')
			openProductView(Constants.OPTION_REMOVE_PRODUCT);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		if (src == btnExportInventory)
			exportInventory();
		else if (src == btnShowCash)
			openCashView();
		else if (src == btnAddProduct)
			openProductView(Constants.OPTION_ADD_PRODUCT);
		else if (src == btnAddStock)
			openProductView(Constants.OPTION_ADD_STOCK);
		else if (src == btnViewInventory)
			openInventoryView();
		else if (src == btnRemoveProduct)
			openProductView(Constants.OPTION_REMOVE_PRODUCT);
	}

	private void exportInventory()
	{
		boolean ok = shop.writeInventory();
		if (ok)
			JOptionPane.showMessageDialog(this, "Inventario exportado !!", "Éxito",
						JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, "Error al exportar inventario.", "Error", JOptionPane.ERROR_MESSAGE);
	}

	public void openCashView()
	{
		CashView dialog = new CashView(shop);
		dialog.setSize(400, 300);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public void openProductView(int option)
	{
		ProductView dialog = new ProductView(shop, option);
		dialog.setSize(400, 400);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public void openInventoryView()
	{
		// reload inventory from DB before showing
		shop.loadInventory();
		// Fallback inventory dialog showing inventory in a scrollable text area
		JDialog dialog = new JDialog(this, "Inventario", true);
		dialog.setLayout(new BorderLayout());
		JTextArea ta = new JTextArea();
		ta.setEditable(false);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < shop.getInventory().size(); i++) {
			if (shop.getInventory().get(i) != null) {
				sb.append(shop.getInventory().get(i).toString()).append("\n");
			}
		}
		ta.setText(sb.toString());
		JScrollPane sp = new JScrollPane(ta);
		dialog.add(sp, BorderLayout.CENTER);
		dialog.setSize(800, 400);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}
}