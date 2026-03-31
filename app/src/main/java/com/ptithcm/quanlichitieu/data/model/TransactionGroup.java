package com.ptithcm.quanlichitieu.data.model;

import java.util.List;

public class TransactionGroup {

    private final String dayOfWeek;
    private final String date;
    private final double dayTotal;
    private final List<Transaction> transactions;

    public TransactionGroup(String dayOfWeek, String date, double dayTotal, List<Transaction> transactions) {
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.dayTotal = dayTotal;
        this.transactions = transactions;
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public String getDate() { return date; }
    public double getDayTotal() { return dayTotal; }
    public List<Transaction> getTransactions() { return transactions; }
}
