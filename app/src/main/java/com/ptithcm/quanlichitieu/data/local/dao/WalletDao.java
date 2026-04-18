package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.WalletEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.utils.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.ArrayList;
import java.util.List;

public class WalletDao {

    private static final String TAG = "WalletDao";
    private final BudgetDatabaseHelper dbHelper;

    public WalletDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Wallet getWalletById(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(WalletEntry.TABLE_NAME, null,
                WalletEntry.COLUMN_ID + " = ?", new String[]{walletId},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToWallet(cursor);
            }
            return null;
        }
    }



    @Nullable
    public String insert(@NonNull Wallet wallet) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (wallet.getId() == null) {
            wallet.setId(IdGenerator.generateUUID());
        }
        long now = IdGenerator.getCurrentTimestamp();
        if (wallet.getCreatedAt() == 0) {
            wallet.setCreatedAt(now);
        }
        wallet.setUpdatedAt(now);

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_ID, wallet.getId());
        values.put(WalletEntry.COLUMN_USER_ID, wallet.getUserId());
        values.put(WalletEntry.COLUMN_NAME, wallet.getName());
        values.put(WalletEntry.COLUMN_INITIAL_BALANCE, wallet.getInitialBalance());
        values.put(WalletEntry.COLUMN_CURRENCY, wallet.getCurrency());
        values.put(WalletEntry.COLUMN_ICON_ID, wallet.getIconId());
        values.put(WalletEntry.COLUMN_CREATED_AT, wallet.getCreatedAt());
        values.put(WalletEntry.COLUMN_UPDATED_AT, wallet.getUpdatedAt());
        if (wallet.getDeletedAt() != null) {
            values.put(WalletEntry.COLUMN_DELETED_AT, wallet.getDeletedAt());
        } else {
            values.putNull(WalletEntry.COLUMN_DELETED_AT);
        }

        long result = db.insert(WalletEntry.TABLE_NAME, null, values);
        return (result == -1) ? null : wallet.getId();
    }

    public List<Wallet> getByUserId(@Nullable String userId) {
        List<Wallet> wallets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String selection = (userId != null) ? WalletEntry.COLUMN_USER_ID + " = ? AND " + WalletEntry.COLUMN_DELETED_AT + " IS NULL" 
                                                : WalletEntry.COLUMN_USER_ID + " IS NULL AND " + WalletEntry.COLUMN_DELETED_AT + " IS NULL";
            String[] selectionArgs = (userId != null) ? new String[]{userId} : null;

            cursor = db.query(WalletEntry.TABLE_NAME, null, selection, selectionArgs, null, null, WalletEntry.COLUMN_CREATED_AT + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wallets.add(cursorToWallet(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return wallets;
    }

    private Wallet cursorToWallet(Cursor cursor) {
        return new Wallet.Builder()
                .setId(CursorUtils.getString(cursor, WalletEntry.COLUMN_ID))
                .setUserId(CursorUtils.getString(cursor, WalletEntry.COLUMN_USER_ID))
                .setName(CursorUtils.getString(cursor, WalletEntry.COLUMN_NAME))
                .setInitialBalance(CursorUtils.getLong(cursor, WalletEntry.COLUMN_INITIAL_BALANCE))
                .setCurrency(CursorUtils.getString(cursor, WalletEntry.COLUMN_CURRENCY))
                .setIconId(CursorUtils.getString(cursor, WalletEntry.COLUMN_ICON_ID))
                .setCreatedAt(CursorUtils.getLong(cursor, WalletEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, WalletEntry.COLUMN_UPDATED_AT))
                .setDeletedAt(CursorUtils.getLong(cursor, WalletEntry.COLUMN_DELETED_AT) == 0 ? null : CursorUtils.getLong(cursor, WalletEntry.COLUMN_DELETED_AT))
                .build();
    }

    public int delete(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(WalletEntry.TABLE_NAME, WalletEntry.COLUMN_ID + " = ?", new String[]{walletId});
    }

    public int update(@NonNull Wallet wallet) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        wallet.setUpdatedAt(IdGenerator.getCurrentTimestamp());

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_NAME, wallet.getName());
        values.put(WalletEntry.COLUMN_INITIAL_BALANCE, wallet.getInitialBalance());
        values.put(WalletEntry.COLUMN_CURRENCY, wallet.getCurrency());
        values.put(WalletEntry.COLUMN_ICON_ID, wallet.getIconId());
        values.put(WalletEntry.COLUMN_UPDATED_AT, wallet.getUpdatedAt());
        if (wallet.getDeletedAt() != null) {
            values.put(WalletEntry.COLUMN_DELETED_AT, wallet.getDeletedAt());
        } else {
            values.putNull(WalletEntry.COLUMN_DELETED_AT);
        }

        return db.update(WalletEntry.TABLE_NAME, values, WalletEntry.COLUMN_ID + " = ?", new String[]{wallet.getId()});
    }
}
