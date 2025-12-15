package com.hash.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hash.common.IApp
//import com.hash.database.dao.UserInfoDao
//import com.hash.database.entity.UserInfoEntity

//@Database(entities = [UserInfoEntity::class], version = 1)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDataBase : RoomDatabase() {

//    abstract fun userInfoDao(): UserInfoDao

    companion object {
        const val DATABASE_NAME = "hash_database.db"

        val db by lazy {
            Room.databaseBuilder(
                IApp.instant, AppDataBase::class.java, DATABASE_NAME,
            ).build()
        }
    }

}