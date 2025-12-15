//package com.hash.database.entity
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
///**
// * @name UserInfoEntity
// * @package com.hash.database
// * @author 345 QQ:1831712732
// * @time 2024/12/26 22:59
// * @description
// */
//
//@Entity(tableName = "user_info")
//data class UserInfoEntity(
//    val admin: Boolean,
//    val chapterTops: List<Int>,
//    val coinCount: Int,
//    val collectIds: List<Int>,
//    val email: String,
//    val icon: String,
//    @PrimaryKey(autoGenerate = false)
//    val id: Int,
//    val nickname: String,
//    val password: String,
//    val publicName: String,
//    val token: String,
//    val type: Int,
//    val username: String
//)