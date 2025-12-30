package com.example.a762map.ui.profile

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VER) {

    companion object {
        private const val DB_NAME = "a762map.db"
        private const val DB_VER = 2
        const val T_USER = "users"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_USER(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              username TEXT NOT NULL,
              phone TEXT NOT NULL UNIQUE,
              email TEXT NOT NULL UNIQUE,
              password TEXT NOT NULL,

              avatarRes INTEGER NOT NULL,
              avatarUri TEXT,

              role TEXT NOT NULL,          -- normal/vip/admin
              vipLevel INTEGER NOT NULL,

              totalMileage REAL NOT NULL,
              yearMileage REAL NOT NULL,
              navCount INTEGER NOT NULL,
              litCities INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seedInitialUsers(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 兼容老库：如果旧表没有 avatarUri，则补上
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE $T_USER ADD COLUMN avatarUri TEXT") } catch (_: Exception) {}
        }
    }

    // ========== 初始化数据 ==========
    private fun seedInitialUsers(db: SQLiteDatabase) {
        insertUserDb(
            db,
            User(
                username = "admin",
                phone = "15111111111",
                email = "admin@a762map.com",
                password = "123456",
                avatarRes = android.R.drawable.sym_def_app_icon,
                avatarUri = null,
                role = "admin",
                vipLevel = 0,
                totalMileage = 999.0,
                yearMileage = 88.0,
                navCount = 66,
                litCities = 20
            )
        )

        val demo = listOf(
            User(username="用户1", phone="18500000001", email="u1@test.com", password="111111",
                avatarRes=android.R.drawable.sym_def_app_icon, avatarUri=null, role="normal", vipLevel=0,
                totalMileage=9.7, yearMileage=2.0, navCount=3, litCities=1),
            User(username="用户2", phone="18500000002", email="u2@test.com", password="111111",
                avatarRes=android.R.drawable.sym_def_app_icon, avatarUri=null, role="normal", vipLevel=0,
                totalMileage=20.0, yearMileage=5.5, navCount=12, litCities=3),
            User(username="VIP1", phone="18500000003", email="vip1@test.com", password="111111",
                avatarRes=android.R.drawable.sym_def_app_icon, avatarUri=null, role="vip", vipLevel=1,
                totalMileage=120.0, yearMileage=30.0, navCount=60, litCities=8),
            User(username="VIP2", phone="18500000004", email="vip2@test.com", password="111111",
                avatarRes=android.R.drawable.sym_def_app_icon, avatarUri=null, role="vip", vipLevel=2,
                totalMileage=66.6, yearMileage=11.0, navCount=22, litCities=6),
            User(username="用户3", phone="18500000005", email="u3@test.com", password="111111",
                avatarRes=android.R.drawable.sym_def_app_icon, avatarUri=null, role="normal", vipLevel=0,
                totalMileage=3.2, yearMileage=1.0, navCount=1, litCities=1),
        )
        demo.forEach { insertUserDb(db, it) }
    }

    private fun ContentValues.putNullableString(key: String, value: String?) {
        if (value.isNullOrEmpty()) putNull(key) else put(key, value)
    }

    private fun insertUserDb(db: SQLiteDatabase, u: User) {
        val cv = ContentValues().apply {
            put("username", u.username)
            put("phone", u.phone)
            put("email", u.email)
            put("password", u.password)

            put("avatarRes", u.avatarRes)
            putNullableString("avatarUri", u.avatarUri) // ✅ 关键修复点

            put("role", u.role)
            put("vipLevel", u.vipLevel)

            put("totalMileage", u.totalMileage)
            put("yearMileage", u.yearMileage)
            put("navCount", u.navCount)
            put("litCities", u.litCities)
        }
        db.insert(T_USER, null, cv)
    }

    // ========== 登录 ==========
    fun loginByPhone(phone: String, pwd: String): User? =
        readableDatabase.rawQuery(
            "SELECT * FROM $T_USER WHERE phone=? AND password=?",
            arrayOf(phone, pwd)
        ).use { c -> if (c.moveToFirst()) c.toUser() else null }

    fun loginByEmail(email: String, pwd: String): User? =
        readableDatabase.rawQuery(
            "SELECT * FROM $T_USER WHERE email=? AND password=?",
            arrayOf(email, pwd)
        ).use { c -> if (c.moveToFirst()) c.toUser() else null }

    // ========== 用户查询 ==========
    fun getUserById(id: Long): User? =
        readableDatabase.rawQuery("SELECT * FROM $T_USER WHERE id=?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) c.toUser() else null }

    // ========== 注册 ==========
    fun registerUser(username: String, phone: String, email: String, password: String): Long {
        val cv = ContentValues().apply {
            put("username", username)
            put("phone", phone)
            put("email", email)
            put("password", password)

            put("avatarRes", android.R.drawable.sym_def_app_icon)
            putNull("avatarUri") // ✅ 关键修复点：不能 put("avatarUri", null)

            put("role", "normal")
            put("vipLevel", 0)
            put("totalMileage", 0.0)
            put("yearMileage", 0.0)
            put("navCount", 0)
            put("litCities", 0)
        }
        return writableDatabase.insert(T_USER, null, cv)
    }

    // ========== 用户自改信息 ==========
    fun updateUserSelf(id: Long, username: String, phone: String, email: String, password: String): Boolean {
        val cv = ContentValues().apply {
            put("username", username)
            put("phone", phone)
            put("email", email)
            put("password", password)
        }
        return writableDatabase.update(T_USER, cv, "id=?", arrayOf(id.toString())) > 0
    }

    // ========== 修改头像 ==========
    fun updateAvatar(userId: Long, uri: String?): Boolean {
        val cv = ContentValues().apply {
            putNullableString("avatarUri", uri) // ✅ 支持清空头像
        }
        return writableDatabase.update(T_USER, cv, "id=?", arrayOf(userId.toString())) > 0
    }

    // ========== 普通用户充值升级 VIP ==========
    fun upgradeToVip(id: Long, vipLevel: Int = 1): Boolean {
        val cv = ContentValues().apply {
            put("role", "vip")
            put("vipLevel", vipLevel)
        }
        return writableDatabase.update(T_USER, cv, "id=?", arrayOf(id.toString())) > 0
    }

    // ========== 管理员：按角色筛选查询 ==========
    fun queryUsers(role: String? = null): List<User> {
        val sql = if (role == null) "SELECT * FROM $T_USER" else "SELECT * FROM $T_USER WHERE role=?"
        val args = if (role == null) null else arrayOf(role)

        val list = mutableListOf<User>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) list.add(c.toUser())
        }
        return list
    }

    // ========== 管理员：新增用户 ==========
    fun adminAddUser(u: User): Long {
        val cv = ContentValues().apply {
            put("username", u.username)
            put("phone", u.phone)
            put("email", u.email)
            put("password", u.password)

            put("avatarRes", u.avatarRes)
            putNullableString("avatarUri", u.avatarUri) // ✅

            put("role", u.role)
            put("vipLevel", u.vipLevel)
            put("totalMileage", u.totalMileage)
            put("yearMileage", u.yearMileage)
            put("navCount", u.navCount)
            put("litCities", u.litCities)
        }
        return writableDatabase.insert(T_USER, null, cv)
    }

    // ========== 管理员：修改用户 ==========
    fun adminUpdateUser(u: User): Boolean {
        val cv = ContentValues().apply {
            put("username", u.username)
            put("phone", u.phone)
            put("email", u.email)
            put("password", u.password)

            put("avatarRes", u.avatarRes)
            putNullableString("avatarUri", u.avatarUri) // ✅

            put("role", u.role)
            put("vipLevel", u.vipLevel)
            put("totalMileage", u.totalMileage)
            put("yearMileage", u.yearMileage)
            put("navCount", u.navCount)
            put("litCities", u.litCities)
        }
        return writableDatabase.update(T_USER, cv, "id=?", arrayOf(u.id.toString())) > 0
    }

    fun adminDeleteUser(id: Long): Boolean =
        writableDatabase.delete(T_USER, "id=?", arrayOf(id.toString())) > 0

    fun adminDeleteAllVip(): Int =
        writableDatabase.delete(T_USER, "role=?", arrayOf("vip"))

    fun adminUpdateStats(
        userId: Long,
        totalMileage: Double,
        yearMileage: Double,
        navCount: Int,
        litCities: Int
    ): Boolean {
        val cv = ContentValues().apply {
            put("totalMileage", totalMileage)
            put("yearMileage", yearMileage)
            put("navCount", navCount)
            put("litCities", litCities)
        }
        return writableDatabase.update(T_USER, cv, "id=?", arrayOf(userId.toString())) > 0
    }

    // ========== Cursor -> User ==========
    private fun Cursor.toUser(): User {
        val avatarUriIndex = getColumnIndex("avatarUri")
        return User(
            id = getLong(getColumnIndexOrThrow("id")),
            username = getString(getColumnIndexOrThrow("username")),
            phone = getString(getColumnIndexOrThrow("phone")),
            email = getString(getColumnIndexOrThrow("email")),
            password = getString(getColumnIndexOrThrow("password")),
            avatarRes = getInt(getColumnIndexOrThrow("avatarRes")),
            avatarUri = if (avatarUriIndex >= 0) getString(avatarUriIndex) else null,
            role = getString(getColumnIndexOrThrow("role")),
            vipLevel = getInt(getColumnIndexOrThrow("vipLevel")),
            totalMileage = getDouble(getColumnIndexOrThrow("totalMileage")),
            yearMileage = getDouble(getColumnIndexOrThrow("yearMileage")),
            navCount = getInt(getColumnIndexOrThrow("navCount")),
            litCities = getInt(getColumnIndexOrThrow("litCities")),
        )
    }
}
