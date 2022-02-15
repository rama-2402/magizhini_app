package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.view.animation.Animation
import android.view.animation.ScaleAnimation

//object Animations {
    val scaleSmall = ScaleAnimation(
        1f,
        0f,
        1f,
        0f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).also {
        it.duration = 350 // animation duration in milliseconds
    }

    val scaleBig: ScaleAnimation = ScaleAnimation(
        0f,
        1f,
        0f,
        1f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).also {
        it.duration = 350 // animation duration in milliseconds
        //            it.fillAfter = true
        // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
    }

//}
