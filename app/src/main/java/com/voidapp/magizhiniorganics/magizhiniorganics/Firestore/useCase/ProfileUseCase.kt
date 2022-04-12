package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity

class ProfileUseCase(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository,
    val profile: UserProfileEntity? = null,
    val update: String? = null,
)
