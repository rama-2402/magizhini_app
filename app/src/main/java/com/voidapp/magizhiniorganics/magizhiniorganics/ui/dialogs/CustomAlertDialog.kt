package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCustomSubDaysBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogDeliveryOutOfServiceBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants

class CustomAlertDialog(
    private val context: Context,
//    private val title: String?,
//    private val body: String?
) {

    private val bottomSheetDialog: BottomSheetDialog =
        BottomSheetDialog(context, R.style.BottomSheetDialog)

    init {
        val view: DialogDeliveryOutOfServiceBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_delivery_out_of_service,
                null,
                false
            )

        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setContentView(view.root)

        view.apply {
            tvClose.setOnClickListener {
                dismiss()
            }
        }

    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}