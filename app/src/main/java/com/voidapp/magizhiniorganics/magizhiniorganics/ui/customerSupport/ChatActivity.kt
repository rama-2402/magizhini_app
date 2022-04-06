package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ContactsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ChatViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityChatBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ChatActivity :
    BaseActivity(),
    KodeinAware,
    ContactsAdapter.ContactItemClickListener {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityChatBinding
    private val factory: ChatViewModelFactory by instance()
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            onBackPressed()
        }

        initData()
        initObservers()
        initViewPager()
        clickListeners()
    }

    private fun initObservers() {
        viewModel.navigateToConversation.observe(this) {
            moveToConversationPage(it)
        }
        viewModel.currentUserProfile.observe(this) {
            //setting the toolbar data - profile pic and user name
            if (!it.profilePicUrl.isNullOrEmpty()) {
                binding.ivProfileImage.loadImg(it.profilePicUrl) {}
            }
            binding.tvProfileName.text = it.name
        }
    }

    private fun initViewPager() {
        val adapter = ChatViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when (position) {
                0 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_chat)
                    tab.icon?.setTint(ContextCompat.getColor(this, R.color.matteRed))
                     tab.text = "Messages"
                }
                1 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_contacts)
//                    tab.icon!!.setTint(ContextCompat.getColor(this, R.color.white))
                    tab.text = "Support"
                }
            }
        }.attach()
    }


    private fun clickListeners() {
        with(binding) {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            binding.tlTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when(tab?.position) {
                        0 -> {
                            tab.icon?.setTint(ContextCompat.getColor(this@ChatActivity, R.color.matteRed))
                            binding.tlTabLayout.getTabAt(1)?.icon?.setTint(Color.WHITE)
                        }
                        1 -> {
                            tab.icon?.setTint(ContextCompat.getColor(this@ChatActivity, R.color.matteRed))
                            binding.tlTabLayout.getTabAt(0)?.icon?.setTint(Color.WHITE)
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }
            })
            //setting the filter action based on the data currently displayed.
            //if we are currently displaying messages then we get all contacts and accordingly
        }
    }

    private fun initData() {
        viewModel.getProfileData()
    }

    private fun moveToConversationPage(supportProfile: SupportProfile) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.profile?.let { profile ->
            Intent(this, ConversationActivity::class.java).also {
                it.putExtra(Constants.CUSTOMER_SUPPORT, supportProfile.id)
                it.putExtra(Constants.PROFILE_NAME, profile.id)
                startActivity(it)
            }
        }
    }

    override fun onPause() {
        viewModel.updateProfileStatus(false, System.currentTimeMillis())
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.updateProfileStatus(false, System.currentTimeMillis())
        super.onDestroy()
    }

    override fun onResume() {
        viewModel.updateProfileStatus(true)
        super.onResume()
    }

    override fun onRestart() {
        viewModel.updateProfileStatus(true)
        super.onRestart()
    }

    override fun moveToConversations(supportProfile: SupportProfile) {
        moveToConversationPage(supportProfile)
    }
}