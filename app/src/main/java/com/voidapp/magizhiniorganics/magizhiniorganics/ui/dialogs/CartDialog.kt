package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomCartBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.CartDialogClickListener
import java.lang.RuntimeException

class CartDialog(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var onItemClickListener: CartDialogClickListener
) {

    private var _binding: DialogBottomCartBinding? = null
    private val binding get() = _binding!!

    private val bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(context,  R.style.BottomSheetDialog)
    private val bottomSheetBehavior: BottomSheetBehavior<LinearLayout> = BottomSheetBehavior()

    init {
        _binding =
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_bottom_cart,
                null,
                false
            )
        bottomSheetDialog.setContentView(binding.root)
    }

    companion object {
        private val updatedCartDialog: MutableLiveData<Boolean> = MutableLiveData()
        val _updatedCartDialog: LiveData<Boolean> = updatedCartDialog

        var cartPrice: String = ""

        fun currentDialogState(dialog: CartDialog): Int = dialog.cartBottomSheetDialogState()

        fun updateDialogState(dialog: CartDialog) = dialog.updateCartBottomDialogState()
    }

    private fun cartBottomSheetDialogState(): Int {
        return bottomSheetBehavior.state
    }

    private fun updateCartBottomDialogState() {
        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun show() {
        onItemClickListener = if (context is CartDialogClickListener) {
            context
        } else {
            throw RuntimeException (
                context.toString() + "AddressDialogClickListener needed"
            )
        }
        updatedCartDialog.observe(lifecycleOwner) {
            updateCartDialogData()
        }
        bottomSheetDialog.show()
    }

    private fun updateCartDialogData() {

    }

    fun dismiss() {

    }
}
