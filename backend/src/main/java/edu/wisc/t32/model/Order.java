package edu.wisc.t32.model;

import edu.wisc.t32.enums.OrderStatus;
import edu.wisc.t32.model.OrderItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an order in the database.
 * Mapped to the "orders" table; acts as a parent for multiple OrderItems.
 */
@Entity
@Table(name = "orders")
public class Order {

    // -- Values --
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // -- Constructors -- 
    protected Order() {}

    public Order(User user) {
        this.user = user;
    }

    public Order(User user, OrderStatus orderStatus) {
        this.user = user;
        this.orderStatus = orderStatus;
    }

    // -- Getters --
    public Integer getOrderId() { return orderId; }
    public User getUser() { return user; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // -- Setters --
    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    // -- Other --
    /**
     * Do everything necessary to finalize the ordering process:
     * - Calculate total price
     * - Set order status to AWAITING_CONFIRMATION
     */
    public void finalizeOrder() {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItem item : items) {
            totalPrice = totalPrice.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        this.totalPrice = totalPrice;
        this.orderStatus = OrderStatus.AWAITING_CONFIRMATION;
    }

    public void addItemToOrder(Item item, Integer quantity) {
        OrderItem newItem = new OrderItem(this, item, quantity, item.getPrice());
        this.items.add(newItem);
        this.totalPrice = this.totalPrice.add(newItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
    }
}