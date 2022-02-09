package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarFilterBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity

class AddressDialog(
    private val context: Context,
    private val activity: Activity,
    private val address: Address?
){
    private val bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)

    init {
        val view: DialogBottomAddressBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_bottom_address,
                null,
                false)

        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setContentView(view.root)

        with(view) {
                address?.let {
                    etProfileName.setText(address.userId)
                    etAddressOne.setText(address.addressLineTwo)
                    etAddressTwo.setText(address.addressLineTwo)
                    spArea.setSelection(address.LocationCodePosition)
                }

                btnSaveAddress.setOnClickListener {
                    when {
                        validateAddress(view.etProfileName.text.toString()) -> view.etProfileName.error =
                            "*required"
                        validateAddress(view.etAddressOne.text.toString()) -> view.etAddressOne.error =
                            "*required"
                        validateAddress(view.etAddressTwo.text.toString()) -> view.etAddressTwo.error =
                            "*required"
                        else -> {
                            Address(
                                etProfileName.text.toString().trim(),
                                etAddressOne.text.toString().trim(),
                                etAddressTwo.text.toString().trim(),
                                spArea.selectedItem.toString(),
                                spArea.selectedItemPosition,
                                spCity.selectedItem.toString()
                            ).let { newAddress ->
                                when(activity) {
                                    is QuickOrderActivity -> {
                                        if (address == null) {
                                            activity.newUpdatedAddress(newAddress, true)
                                        } else {
                                            activity.newUpdatedAddress(newAddress, false)
                                        }
                                    }
                                }
                                dismiss()
                            }
                        }
                    }
                }
            }
        }

    private fun validateAddress(text: String?): Boolean {
        return text.isNullOrBlank()
    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}