package com.hash.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by KngLv
 * @time 2026/1/15 10:07
 * @description
 */

@Entity(tableName = "gson_factory_parse_exception")
class GsonFactoryPaseExceptionEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var isDebug: Boolean = false
    var message: String? = null
    var timestamp: Long = System.currentTimeMillis()
}