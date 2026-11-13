package com.portfolio.splitsync.data.local

import androidx.room.TypeConverter
import com.portfolio.splitsync.domain.model.SplitType
import com.portfolio.splitsync.domain.model.SyncStatus

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(",")

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSplitType(type: SplitType): String = type.name

    @TypeConverter
    fun toSplitType(value: String): SplitType = SplitType.valueOf(value)


    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String =
        map.entries.joinToString(";") { "${it.key}:${it.value}" }

    @TypeConverter
    fun toStringMap(data: String): Map<String, String> {
        if (data.isBlank()) return emptyMap()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

}
