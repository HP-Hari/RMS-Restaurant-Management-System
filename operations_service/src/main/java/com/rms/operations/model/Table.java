package com.rms.operations.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "core_table")
public class Table {

    @Id
    private String id;

    private Integer table_number;
    private Integer capacity;
    private Boolean is_occupied;

    public Table() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getTable_number() { return table_number; }
    public void setTable_number(Integer table_number) { this.table_number = table_number; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Boolean getIs_occupied() { return is_occupied; }
    public void setIs_occupied(Boolean is_occupied) { this.is_occupied = is_occupied; }
}
