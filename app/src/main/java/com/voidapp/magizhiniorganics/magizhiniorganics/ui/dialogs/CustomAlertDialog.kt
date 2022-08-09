package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
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
                onItemClickListener.onClick()
                dismiss()
            }
        }

    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}

interface CustomAlertClickListener {
    fun onClick()
}