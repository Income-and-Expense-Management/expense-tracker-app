package com.ptithcm.quanlichitieu.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.ArrayList;
import java.util.List;

public class BudgetDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "budget.db";
    private static final int DATABASE_VERSION = 1;

    // Wallet Table
    private static final String TABLE_WALLET = "wallet";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_BALANCE = "balance";

    public BudgetDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_WALLET_TABLE = "CREATE TABLE " + TABLE_WALLET + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_BALANCE + " REAL)";
        db.execSQL(CREATE_WALLET_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WALLET);
        onCreate(db);
    }

    // CRUD for Wallet

    public long addWallet(Wallet wallet) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, wallet.getName());
        values.put(COLUMN_BALANCE, wallet.getBalance());
        long id = db.insert(TABLE_WALLET, null, values);
        db.close();
        return id;
    }

    public void updateWallet(Wallet wallet) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, wallet.getName());
        values.put(COLUMN_BALANCE, wallet.getBalance());
        db.update(TABLE_WALLET, values, COLUMN_ID + " = ?", new String[]{String.valueOf(wallet.getId())});
        db.close();
    }

    public List<Wallet> getAllWallets() {
        List<Wallet> wallets = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_WALLET;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE));
                wallets.add(new Wallet(id, name, balance));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return wallets;
    }

    // New helper method: Get the first wallet or null if none
    public Wallet getFirstWallet() {
        Wallet wallet = null;
        String selectQuery = "SELECT * FROM " + TABLE_WALLET + " LIMIT 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
             int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
             String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
             double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE));
             wallet = new Wallet(id, name, balance);
        }
        cursor.close();
        db.close();
        return wallet;
    }
}
