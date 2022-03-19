package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business.contacts

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityNewPartnerBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.business.BusinessViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.business.BusinessViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class NewPartnerActivity :
    BaseActivity(),
    KodeinAware
{

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityNewPartnerBinding
    private lateinit var viewModel: BusinessViewModel
    private val factory: BusinessViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_partner)
        viewModel = ViewModelProvider(this, factory).get(BusinessViewModel::class.java)

        initObservers()
        initListeners()
    }

    private fun initObservers() {
        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is BusinessViewModel.UiUpdate.UpdatedNewPartner -> {
                   hideProgressDialog()
                   if (event.isSuccessful) {
                       onBackPressed()
                   } else {
                       showErrorSnackBar("Failed to send request. Try again later", true)
                   }
                }
            }
        }
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivSend.setOnClickListener {
                if (dataValidated()) {
                    val newPartnersMap = hashMapOf<String, String>()
                    newPartnersMap["name"] = etPartnerName.text.toString().trim()
                    newPartnersMap["business"] = etPartnerName.text.toString().trim()
                    newPartnersMap["phone"] = etPartnerName.text.toString().trim()
                    newPartnersMap["mail"] = etPartnerName.text.toString().trim()
                    newPartnersMap["social"] = etPartnerName.text.toString().trim()
                    newPartnersMap["description"] = etPartnerName.text.toString().trim()

                    showProgressDialog()
                    viewModel.sendNewPartnerRequest(newPartnersMap)
                }
            }
        }
    }

    private fun dataValidated(): Boolean {
        binding.apply {
            return when {
                etPartnerName.text.toString().isNullOrEmpty() -> {
                    showErrorSnackBar("Partner Name is Mandatory", true)
                    false
                }
                etBusinessName.text.toString().isNullOrEmpty() -> {
                    showErrorSnackBar("Business Name is Mandatory", true)
                    false
                }
                etPhoneNumber.text.toString().isNullOrEmpty() -> {
                    showErrorSnackBar("Phone Number is Mandatory", true)
                    false
                }
                etEmailID.text.toString().isNullOrEmpty() -> {
                    showErrorSnackBar("Email ID is Mandatory", true)
                    false
                }
                etShortDescription.text.toString().isNullOrEmpty() -> {
                    showErrorSnackBar("Short description of business proposal is Mandatory", true)
                    false
                }
                else -> true
            }
        }
    }
}