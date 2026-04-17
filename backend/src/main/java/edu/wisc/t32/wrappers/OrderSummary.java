package edu.wisc.t32.wrappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummary {
    private String orderType; // "buy" or "sell"
    private String buyerName;
    private String sellerName;
    private String itemName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String createdAt;

    protected OrderSummary() {}

    public OrderSummary(String orderType, String buyerName, String sellerName, String itemName, Integer quantity,
                        BigDecimal price, String createdAt) {
        this.orderType = orderType;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
        this.createdAt = createdAt;
    }

	public String getOrderType() { return orderType; }
    public String getBuyerName() { return buyerName; }
    public String getSellerName() { return sellerName; }
    public String getItemName() { return itemName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getCreatedAt() { return createdAt; }
}
