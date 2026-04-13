package edu.wisc.t32.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Entity representing an order item in the database.
 * Mapped to the "order_items" table and stores a single item as
 * part of a larger order.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    // -- Values --
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal unitPrice;

    // -- Constructors -- 
    protected OrderItem() {}

    public OrderItem(Order order, Item item, Integer quantity, BigDecimal unitPrice) {
        this.order = order;
        this.item = item;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // -- Getters --
    public Integer getOrderItemId() { return orderItemId; }
    public Order getOrder() { return order; }
    public Item getItem() { return item; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    // -- Setters --
    /**
     * Internal setter used by the Order parent class.
     */
    protected void setOrder(Order order) {
        this.order = order;
    }
}