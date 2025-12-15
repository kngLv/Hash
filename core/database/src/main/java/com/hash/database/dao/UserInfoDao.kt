package com.hash.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

//@Dao
//interface UserInfoDao {
//
//    /// replace: 如果插入的数据在数据库中已经存在（根据主键或唯一约束判断），则用新的数据替换掉旧的数据。
//    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
//    suspend fun insertUserInfo(infoDao: UserInfoDao)
//
//    @Query("SELECT * FROM user_info WHERE id = :id LIMIT 1")
//    suspend fun getUserInfo(id: Int): UserInfoDao?
//
//    @Query("DELETE FROM user_info WHERE id = :id")
//    suspend fun deleteUserInfo(id: Int)
//}