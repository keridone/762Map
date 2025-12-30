package com.example.a762map.data.checkin

object CheckinContract {
    const val DB_NAME = "checkin.db"
    const val DB_VERSION = 1

    const val TABLE_PHOTO = "place_checkin_photo"

    const val COL_ID = "id"
    const val COL_PLACE_KEY = "place_key"
    const val COL_TITLE = "title"
    const val COL_FILE_PATH = "file_path"
    const val COL_CREATED_AT = "created_at"

    const val SQL_CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_PHOTO (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PLACE_KEY TEXT NOT NULL,
            $COL_TITLE TEXT NOT NULL,
            $COL_FILE_PATH TEXT NOT NULL,
            $COL_CREATED_AT INTEGER NOT NULL
        )
    """
    const val SQL_CREATE_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_place_key_created_at
        ON $TABLE_PHOTO($COL_PLACE_KEY, $COL_CREATED_AT DESC)
    """
}
