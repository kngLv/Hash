package com.hash.database

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hash.database.dao.GsonFactoryPaseExceptionDao
import com.hash.database.entity.GsonFactoryPaseExceptionEntity

@Database(entities = [GsonFactoryPaseExceptionEntity::class], version = 1)
abstract class AppDataBase : RoomDatabase() {

//    abstract fun userInfoDao(): UserInfoDao

    abstract fun gsonFactoryPaseExceptionDao(): GsonFactoryPaseExceptionDao

    companion object {
        const val DATABASE_NAME = "hash_database.db"
        lateinit var db: AppDataBase

        fun init(context: Application) {
            db = Room.databaseBuilder(context, AppDataBase::class.java, DATABASE_NAME).build()
        }
    }

}