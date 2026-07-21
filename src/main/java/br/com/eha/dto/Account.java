package br.com.eha.dto;

import lombok.Data;

@Data
public class Account {

    private String id;
    private int balance;

    public Account(String id, int amount) {
        this.id = id;
        this.balance = amount;
    }

    public void deposit(int amount) {
        this.balance += amount;
    }

    public boolean withdraw(int amount) {
        if (this.balance < amount) {
            return false;
        }
        this.balance -= amount;
        return true;
    }
}
