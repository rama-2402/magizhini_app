package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityContactUsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*

class ContactUsActivity :
    BaseActivity(),
    KodeinAware
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityContactUsBinding
    private lateinit var viewModel: BusinessViewModel
    private val factory: BusinessViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_us)
        viewModel = ViewModelProvider(this, factory).get(BusinessViewModel::class.java)

        initListeners()
        initObservers()
    }

    private fun initObservers() {
        viewModel.uiUpdate.observe(this){ event ->
            when(event) {
                is BusinessViewModel.UiUpdate.OpenUrl -> openInBrowser("")
                else -> BusinessViewModel.UiUpdate.Empty
            }
        }
        viewModel.setEmptyUI()
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivContactUs.setOnClickListener {
                showListBottomSheet(this@ContactUsActivity, arrayListOf("Call", "WhatsApp", "E-Mail"))
            }
            ivBecomePartner.setOnClickListener {
                showExitSheet(this@ContactUsActivity, "Become a Business Partner with Magihini Organics by sending a request to us with your contact details and a short description of your business. We will Contact you shortly once reviewing your profile. Please click PROCEED to continue", "business")
            }
            ivCareers.setOnClickListener {
                if (!NetworkHelper.isOnline(this@ContactUsActivity)) {
                    showErrorSnackBar("Please check your Internet Connection", true)
                    return@setOnClickListener
                }
                showProgressDialog()
                openInBrowser("")
            }
        }
    }


    fun selectedContactMethod(selectedItem: String) {
        when(selectedItem) {
            "Call" -> {
                this.callNumberIntent("7299827393")
            }
            "WhatsApp" -> {
                val message = ""
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://api.whatsapp.com/send?phone=+917299827393&text=$message"
                        )
                    )
                )
            }
            "E-Mail" -> {
                shareToGMail(arrayOf("magizhiniOrganics2018@gmail.com"), "", "")
            }
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(url)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

    private fun shareToGMail(email: Array<String?>?, subject: String?, content: String?) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        try {
            emailIntent.putExtra(Intent.EXTRA_EMAIL, email)
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_TEXT, content)
            val pm: PackageManager = this.packageManager
            val matches = pm.queryIntentActivities(emailIntent, 0)
            var best: ResolveInfo? = null
            for (info in matches) if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.lowercase(
                    Locale.getDefault()
                )
                    .contains("gmail")
            ) best = info
            if (best != null) emailIntent.setClassName(
                best.activityInfo.packageName,
                best.activityInfo.name
            )
            this.startActivity(emailIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "Email App is not installed in your phone.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun addNewPartnerAccount() {
        openInBrowser("https://forms.gle/eaCWzYVCetunTigd9")
//        Intent(this, NewPartnerActivity::class.java).also {
//            startActivity(it)
//        }
    }

    fun sendCareerMail() {
        shareToGMail(arrayOf("magizhiniOrganics2018@gmail.com"), "Looking for Job in Magizhini Organcis", "")
    }
}