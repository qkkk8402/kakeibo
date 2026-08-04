package com.example.kakeibo.data

import androidx.room.TypeConverter
import com.example.kakeibo.model.TransactionType

class Converters {
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
