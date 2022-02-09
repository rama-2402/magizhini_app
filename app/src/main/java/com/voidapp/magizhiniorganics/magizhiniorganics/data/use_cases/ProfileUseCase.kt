package com.voidapp.magizhiniorganics.magizhiniorganics.data.use_cases

import com.google.firebase.crashlytics.internal.breadcrumbs.DisabledBreadcrumbSource
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity

class ProfileUseCase(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository,
    val profile: UserProfileEntity? = null,
    val update: String? = null,
) {




}
