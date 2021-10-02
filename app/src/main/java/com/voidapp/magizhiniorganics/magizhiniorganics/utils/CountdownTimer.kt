package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.app.Activity
import android.graphics.Color
import android.view.View
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SignInActivity
import ticker.views.com.ticker.widgets.circular.timer.callbacks.CircularViewCallback
import ticker.views.com.ticker.widgets.circular.timer.view.CircularView

class CountdownTimer {

    fun initTimer(activity: SignInActivity) {
       val builderWithTimer = CircularView.OptionsBuilder()
            .shouldDisplayText(true)
            .setCounterInSeconds(59)
            .setCircularViewCallback(object : CircularViewCallback {
                override fun onTimerFinish() {
                    // Will be called if times up of countdown timer
                    activity.onOtpTimeOut()
                }

                override fun onTimerCancelled() {

                }
            })
        activity.binding.cvTimer.setOptions(builderWithTimer)
    }
}