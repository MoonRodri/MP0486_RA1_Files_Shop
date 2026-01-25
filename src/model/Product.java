package model;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "inventory")
public class Product implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    // use wrapper Double to allow null values when reading existing rows
    @Column(name = "price")
    private Double price;

    @Column(name = "available")
    private boolean available;

    @Column(name = "stock")
    private int stock;

    @Transient
    private Amount publicPrice;

    @Transient
    private Amount wholesalerPrice;

    private static int totalProducts;

    public final static double EXPIRATION_RATE = 0.60;

    // No-arg constructor required by Hibernate
    public Product() {
    }

    public Product(String name, Amount wholesalerPrice, boolean available, int stock) {
        super();
        this.name = name;
        this.wholesalerPrice = wholesalerPrice;
        this.price = wholesalerPrice != null ? wholesalerPrice.getValue() : 0.0;
        this.publicPrice = new Amount(this.price * 2);
        this.available = available;
        this.stock = stock;
        // id will be assigned by DB when persisted via Hibernate
        totalProducts++;
    }

    @PostLoad
    private void postLoad() {
        // initialize transient Amount fields after Hibernate loads entity
        double base = (this.price != null) ? this.price : 0.0;
        this.wholesalerPrice = new Amount(base);
        this.publicPrice = new Amount(base * 2);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Amount getPublicPrice() {
        double base = (this.price != null) ? this.price : 0.0;
        if (publicPrice == null) {
            publicPrice = new Amount(base * 2);
        }
        return publicPrice;
    }

    public void setPublicPrice(Amount publicPrice) {
        this.publicPrice = publicPrice;
    }

    public Amount getWholesalerPrice() {
        double base = (this.price != null) ? this.price : 0.0;
        if (wholesalerPrice == null) {
            wholesalerPrice = new Amount(base);
        }
        return wholesalerPrice;
    }

    public void setWholesalerPrice(Amount wholesalerPrice) {
        this.wholesalerPrice = wholesalerPrice;
        if (wholesalerPrice != null) {
            this.price = wholesalerPrice.getValue();
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public static int getTotalProducts() {
        return totalProducts;
    }

    public static void setTotalProducts(int totalProducts) {
        Product.totalProducts = totalProducts;
    }

    public void expire() {
        // adjust transient public price by expiration rate
        if (getPublicPrice() != null) {
            this.publicPrice.setValue(this.getPublicPrice().getValue() * EXPIRATION_RATE);
        }
    }

    @Override
    public String toString() {
        return "Product [name=" + name + ", publicPrice=" + getPublicPrice() + ", wholesalerPrice=" + getWholesalerPrice()
                + ", available=" + available + ", stock=" + stock + "]";
    }

}