package com.voidapp.magizhiniorganics.magizhiniorganics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.UserProfileDao
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters
import org.kodein.di.Volatile

@Database(
    entities = [UserProfileEntity::class,
                VariantEntity::class,
                ProductEntity::class,
                ProductCategoryEntity::class,
                CouponEntity::class,
                BannerEntity::class,
                OrderEntity::class,
                CartEntity::class,
               WalletEntity::class,
               PinCodesEntity::class],

    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UserDatabase: RoomDatabase() {

    abstract fun getUserProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var instance: UserDatabase? = null

        private val migration_1_2: Migration = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE CartEntity ADD COLUMN variantIndex INTEGER NOT NULL DEFAULT '0'")
            }
        }

        private val LOCK = Any()
        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also {
                instance = it
            }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                UserDatabase::class.java,
                Constants.DATABASE
            )
                .addMigrations(migration_1_2)
                .build()
    }
}