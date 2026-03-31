package com.ptithcm.quanlichitieu.data.repository;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.List;

public interface TransactionRepository {

    List<TransactionGroup> getTransactionsByMonth(int monthOffset);

    double getTotalExpense();

    double getTotalIncome();

    double getTotalBalance();
}
