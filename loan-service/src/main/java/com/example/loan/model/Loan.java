package com.example.loan.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    private String id;
    private String userId;
    private Double amount;
    private String status;

    public Loan() {}

    public Loan(String id, String userId, Double amount, String status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
