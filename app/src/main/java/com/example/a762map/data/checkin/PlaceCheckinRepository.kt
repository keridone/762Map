package com.example.a762map.data.checkin

import android.content.ContentValues
import android.content.Context

class PlaceCheckinRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = CheckinDbHelper(appContext)

    fun list(placeKey: String): List<CheckinPhoto> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            CheckinContract.TABLE_PHOTO,
            arrayOf(
                CheckinContract.COL_ID,
                CheckinContract.COL_PLACE_KEY,
                CheckinContract.COL_TITLE,
                CheckinContract.COL_FILE_PATH,
                CheckinContract.COL_CREATED_AT
            ),
            "${CheckinContract.COL_PLACE_KEY}=?",
            arrayOf(placeKey),
            null,
            null,
            "${CheckinContract.COL_CREATED_AT} DESC"
        )

        val out = mutableListOf<CheckinPhoto>()
        cursor.use {
            while (it.moveToNext()) {
                out.add(
                    CheckinPhoto(
                        id = it.getLong(0),
                        placeKey = it.getString(1),
                        title = it.getString(2),
                        filePath = it.getString(3),
                        createdAt = it.getLong(4)
                    )
                )
            }
        }
        return out
    }

    fun insert(placeKey: String, title: String, filePath: String): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(CheckinContract.COL_PLACE_KEY, placeKey)
            put(CheckinContract.COL_TITLE, title)
            put(CheckinContract.COL_FILE_PATH, filePath)
            put(CheckinContract.COL_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(CheckinContract.TABLE_PHOTO, null, cv)
    }
}
