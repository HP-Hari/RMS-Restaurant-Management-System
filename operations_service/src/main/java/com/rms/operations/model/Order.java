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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }
    public String getItems_json() { return items_json; }
    public void setItems_json(String items_json) { this.items_json = items_json; }
}
