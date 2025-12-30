package com.example.a762map.data.checkin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CheckinDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    CheckinContract.DB_NAME,
    null,
    CheckinContract.DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CheckinContract.SQL_CREATE_TABLE)
        db.execSQL(CheckinContract.SQL_CREATE_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 -> v1 暂无升级
    }
}
