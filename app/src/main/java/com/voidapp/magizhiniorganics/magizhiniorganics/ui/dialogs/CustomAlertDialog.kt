package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogDeliveryOutOfServiceBinding

class CustomAlertDialog(
    private val context: Context,
    private val title: String?,
    private val body: String?,
    private val button: String?,
    private val content: String,
    private val onItemClickListener: CustomAlertClickListener
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
            if (content == "order" ) {
                tvwhatsapp.visibility = View.VISIBLE
            }
            if (content == "newID") {
                bottomSheetDialog.setCancelable(false)
                tvwhatsapp.visibility = View.VISIBLE
                tvwhatsapp.text = "Skip Sign In"
            }
            title?.let {
                tvTitle.text = it
            }
            body?.let {
                tvBody.text = it
            }
            button?.let {
                tvClose.text = it
            }
            tvClose.setOnClickListener {
                when (content) {
                    "order" -> {
                        onItemClickListener.goToSignIn()
                        dismiss()
                    }
                    "newID" -> {
                        onItemClickListener.goToSignIn()
                        dismiss()
                    }
                    else -> {
                        onItemClickListener.onClick()
                        dismiss()
                    }
                }
            }
            tvwhatsapp.setOnClickListener {
                if (content == "order") {
                    onItemClickListener.placeOrderWithWhatsapp()
                    dismiss()
                }
                if (content == "newID") {
                    onItemClickListener.closeActivity()
                    dismiss()
                }
           }
        }

    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}

interface CustomAlertClickListener {
    fun onClick()
    fun goToSignIn() {}
    fun placeOrderWithWhatsapp() {}
    fun closeActivity() {}
}