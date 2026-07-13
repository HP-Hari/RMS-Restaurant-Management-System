package com.rms.operations.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "core_order")
public class Order {
    @Id
    private String id;
    private Integer table_number;
    private Double total_amount;
    private Double discount_amount;
    private String discount_name;
    private String order_type;
    private String payment_method;
    private String payment_status;
    private String customer_name;
    private String customer_phone;
    private String status;
    private LocalDateTime created_at;
    private String items_json;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getTable_number() { return table_number; }
    public void setTable_number(Integer table_number) { this.table_number = table_number; }
    public Double getTotal_amount() { return total_amount; }
    public void setTotal_amount(Double total_amount) { this.total_amount = total_amount; }
    public Double getDiscount_amount() { return discount_amount; }
    public void setDiscount_amount(Double discount_amount) { this.discount_amount = discount_amount; }
    public String getDiscount_name() { return discount_name; }
    public void setDiscount_name(String discount_name) { this.discount_name = discount_name; }
    public String getOrder_type() { return order_type; }
    public void setOrder_type(String order_type) { this.order_type = order_type; }
    public String getPayment_method() { return payment_method; }
    public void setPayment_method(String payment_method) { this.payment_method = payment_method; }
    public String getPayment_status() { return payment_status; }
    public void setPayment_status(String payment_status) { this.payment_status = payment_status; }
    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }
    public String getCustomer_phone() { return customer_phone; }
    public void setCustomer_phone(String customer_phone) { this.customer_phone = customer_phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }
    public String getItems_json() { return items_json; }
    public void setItems_json(String items_json) { this.items_json = items_json; }
}
