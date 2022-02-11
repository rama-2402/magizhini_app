package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ActionProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.protobuf.Value
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarFilterBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import java.lang.RuntimeException

class AddressDialog: BottomSheetDialogFragment() {

    private var _binding: DialogBottomAddressBinding? = null
    private val binding get() = _binding!!

    private var onItemClickListener: AddressDialogClickListener? = null

    private var addressData: Address? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBottomAddressBinding.inflate(inflater, container, false)
        addressData = arguments?.getParcelable<Address>("address")

        populateData(addressData)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveAddress.setOnClickListener {
            when {
                validateAddress(binding.etProfileName.text.toString()) -> binding.etProfileName.error =
                    "*required"
                validateAddress(binding.etAddressOne.text.toString()) -> binding.etAddressOne.error =
                    "*required"
                validateAddress(binding.etAddressTwo.text.toString()) -> binding.etAddressTwo.error =
                    "*required"
                else -> sendUpdatedAddress()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        onItemClickListener = if (context is AddressDialogClickListener) {
            context
        } else {
            throw RuntimeException (
                context.toString() + "AddressDialogClickListener needed"
                    )
        }
    }

    private fun populateData(address: Address?) {
        address?.let {
            binding.etProfileName.setText(it.userId)
            binding.etAddressOne.setText(it.addressLineOne)
            binding.etAddressTwo.setText(it.addressLineTwo)
            binding.spArea.setSelection(it.LocationCodePosition)
        }
    }

    private fun sendUpdatedAddress() {
        val addressMap: HashMap<String, Any> = hashMapOf()
        addressMap["userId"] = binding.etProfileName.text.toString().trim()
        addressMap["addressLineOne"] = binding.etAddressOne.text.toString().trim()
        addressMap["addressLineTwo"] = binding.etAddressTwo.text.toString().trim()
        addressMap["LocationCode"] = binding.spArea.selectedItem.toString()
        addressMap["LocationCodePosition"] = binding.spArea.selectedItemPosition
        addressMap["city"] = binding.spCity.selectedItem.toString()

        onItemClickListener?.let {
            if (addressData == null) {
                it.savedAddress(addressMap, true)
            } else {
                it.savedAddress(addressMap, false)
            }
        }
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        addressData = null
        onItemClickListener = null
        _binding = null
    }

    private fun validateAddress(text: String?): Boolean {
        return text.isNullOrBlank()
    }
}