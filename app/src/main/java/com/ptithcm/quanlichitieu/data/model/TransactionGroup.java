package com.ptithcm.quanlichitieu.data.model;

import java.util.List;

public class TransactionGroup {

    private final String dayOfWeek;
    private final String date;
    private final long dayTotal;
    private final List<Transaction> transactions;

    public TransactionGroup(String dayOfWeek, String date, long dayTotal, List<Transaction> transactions) {
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.dayTotal = dayTotal;
        this.transactions = transactions;
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public String getDate() { return date; }
    public long getDayTotal() { return dayTotal; }
    public List<Transaction> getTransactions() { return transactions; }
}
