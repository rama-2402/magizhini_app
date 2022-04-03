package com.voidapp.magizhiniorganics.magizhiniorganics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.UserProfileDao
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters
import org.kodein.di.Volatile

@Database(
    entities = [
        UserProfileEntity::class,
        ProductEntity::class,
        ProductCategoryEntity::class,
        CouponEntity::class,
        BannerEntity::class,
        OrderEntity::class,
        CartEntity::class,
        PinCodesEntity::class,
        SubscriptionEntity::class,
        Favorites::class,
        ActiveOrders::class,
        ActiveSubscriptions::class,
        BestSellers::class,
        SpecialsOne::class,
        SpecialsTwo::class,
        SpecialsThree::class,
        SpecialBanners::class,
        UserNotificationEntity::class,
        TestimonialsEntity::class
               ],

    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UserDatabase : RoomDatabase() {

    abstract fun getUserProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var instance: UserDatabase? = null

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
                .fallbackToDestructiveMigration()
                .build()
    }
}