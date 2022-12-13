package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ContactsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityChatBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ChatActivity :
    BaseActivity(),
    KodeinAware,
    ContactsAdapter.ContactItemClickListener
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityChatBinding
    private val factory: ChatViewModelFactory by instance()
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        showProgressDialog(true)

        initData()
        initObservers()
        clickListeners()
    }

    private fun initObservers() {
        viewModel.currentUserProfile.observe(this) { profile ->
            //setting the toolbar data - profile pic and user name
            hideProgressDialog()
            profile?.let {
                 if (!it.profilePicUrl.isNullOrEmpty()) {
                    binding.ivProfileImage.loadImg(it.profilePicUrl) {}
                }
                binding.tvProfileName.text = it.name
            } ?: let {
               binding.tvProfileName.text = "Not Signed In"
            }
            loadSupportProfiles()
       }
    }

    private fun loadSupportProfiles() {
        val supportProfiles = mutableListOf<SupportProfile>()
        SupportProfile(
            "Ganesh",
            "7299827393"
        ).let {
            supportProfiles.add(it)
        }
        SupportProfile(
            "Ramasubramanian",
            "9486598819"
        ).let {
            supportProfiles.add(it)
        }
        SupportProfile(
            "Vetri",
            "8124360179"
        ).let {
            supportProfiles.add(it)
        }
        SupportProfile(
            "Jagadeesh",
            "7904939372"
        ).let {
            supportProfiles.add(it)
        }
        
        ContactsAdapter(
            this,
            supportProfiles,
            this
        ).also {
            binding.rvCustomerSupport.adapter = it
            binding.rvCustomerSupport.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun clickListeners() {
        with(binding) {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun initData() {
        viewModel.getProfileData()
    }

    override fun getHelp(number: String) {
        val message = ""
         startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://api.whatsapp.com/send?phone=+91$number&text=$message"
                )
            )
        )
    }
}