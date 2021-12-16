package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ContactsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ChatViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityChatBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
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
        binding.viewmodel = viewModel

        initData()
        initViewPager()
        initObservers()
        clickListeners()
    }

    private fun initObservers() {
        viewModel.navigateToConversation.observe(this, {
            moveToConversationPage(it)
        })
        viewModel.currentUserProfile.observe(this, {
            //setting the toolbar data - profile pic and user name
            GlideLoader().loadUserPicture(this@ChatActivity, it.profilePicUrl, binding.ivProfileImage)
            binding.tvProfileName.text = it.name
        })
    }

    private fun initViewPager() {
        val adapter = ChatViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when(position) {
                0 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_chat)
                    tab.icon!!.setTint(ContextCompat.getColor(this, R.color.matteGreen))
                    tab.text = "Messages"
                }
                1 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_contacts)
                    tab.icon!!.setTint(ContextCompat.getColor(this, R.color.matteGreen))
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
            //setting the filter action based on the data currently displayed.
            //if we are currently displaying messages then we get all contacts and accordingly
        }
    }

    private fun initData() {
        viewModel.getProfileData()
    }

    private fun moveToConversationPage(supportProfile: SupportProfile) {
        Intent(this, ConversationActivity::class.java).also {
            it.putExtra(Constants.CUSTOMER_SUPPORT, supportProfile.id)
            it.putExtra(Constants.PROFILE_NAME, viewModel.profile.id)
            startActivity(it)
            finish()
        }
    }

    override fun onPause() {
        viewModel.updateProfileStatus( false, System.currentTimeMillis())
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.updateProfileStatus( false, System.currentTimeMillis())
        super.onDestroy()
    }

    override fun onResume() {
        viewModel.updateProfileStatus( true)
        super.onResume()
    }

    override fun onRestart() {
        viewModel.updateProfileStatus( true)
        super.onRestart()
    }

    override fun moveToConversations(supportProfile: SupportProfile) {
        moveToConversationPage(supportProfile)
    }
}