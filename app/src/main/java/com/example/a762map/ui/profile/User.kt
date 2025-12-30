package com.example.a762map.ui.profile

data class User(
    val id: Long = 0,
    var username: String,
    var phone: String,
    var email: String,
    var password: String,
    var avatarRes: Int,
    var role: String,      // "normal" | "vip" | "admin"
    var vipLevel: Int,
    var totalMileage: Double,
    var yearMileage: Double,
    var navCount: Int,
    var litCities: Int
)
