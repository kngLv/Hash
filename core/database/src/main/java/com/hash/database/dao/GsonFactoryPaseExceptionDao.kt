package com.hash.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hash.database.entity.GsonFactoryPaseExceptionEntity

/**
 * Created by KngLv
 * @time 2026/1/15 10:10
 * @description
 */

@Dao
interface GsonFactoryPaseExceptionDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GsonFactoryPaseExceptionEntity)

    @Query("SELECT * FROM gson_factory_parse_exception ORDER BY timestamp DESC")
    suspend fun getAll(): List<GsonFactoryPaseExceptionEntity>


    @Query("DELETE FROM gson_factory_parse_exception WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM gson_factory_parse_exception WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)


    @Query("DELETE FROM gson_factory_parse_exception")
    suspend fun deleteAll()
}