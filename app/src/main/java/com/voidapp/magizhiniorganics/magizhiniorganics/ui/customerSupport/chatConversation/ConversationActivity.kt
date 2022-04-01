package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ConversationAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messages
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.NotificationData
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.PushNotification
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityConversationBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.RetrofitInstance
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOMER_SUPPORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.IMAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ONLINE_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.TEXT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ConversationActivity :
    BaseActivity(),
    KodeinAware,
    ConversationAdapter.ConversationItemClickListener
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityConversationBinding
    private val factory: ConversationViewModelFactory by instance()
    private lateinit var viewModel: ConversationViewModel
    private lateinit var adapter: ConversationAdapter

    private var isOnline: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        viewModel = ViewModelProvider(this, factory).get(ConversationViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.supportID = intent?.getStringExtra(CUSTOMER_SUPPORT).toString()

        initData()
        initRecyclerView()
        initObservers()
        clickListeners()
    }

    private fun initData() {
        showProgressDialog()
        viewModel.getToken()
        viewModel.getProfileData()
        viewModel.supportStatusListener()
        internetConnectionChecked()
    }

    private fun internetConnectionChecked(){
        NetworkManagerUtil(this).observe(this) { isNetworkAvailable ->
            isNetworkAvailable?.let {
                if (it && !isOnline) {
                    showErrorSnackBar("Connection Established", false)
                }
                if (!it && isOnline) {
                    showErrorSnackBar("Please check your Network Connection", true)
                }
                isOnline = it            }
        }
    }

    private fun initObservers() {
        viewModel.conversation.observe(this) {
            adapter.updateMessage(it)
            adapter.currentUserID = viewModel.profile.id
            binding.rvConversation.scrollToPosition(it.size - 1)
            hideProgressDialog()
        }
        viewModel.liveSupportProfile.observe(this) {
            viewModel.supportProfile = it
            setTypingStatus(it)
            checkOnlineStatus(it)
        }
        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> showProgressDialog()
                    else -> Unit
                }
            }
        }
    }

    private fun checkOnlineStatus(profile: SupportProfile) {
        if (profile.online) {
            binding.tvStatus.text = "Online"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_base))
        } else {
            binding.tvStatus.text = TimeUtil().getTimeAgo(profile.timestamp)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.grey))
        }
    }

    private fun initRecyclerView() {
        adapter = ConversationAdapter(
            arrayListOf(),
            "",
            this
        )
        binding.rvConversation.layoutManager = LinearLayoutManager(this)
        binding.rvConversation.adapter = adapter
    }

    private fun clickListeners() {
        with(binding) {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivAddAttachment.setOnClickListener {
                ivAddAttachment.startAnimation(AnimationUtils.loadAnimation(ivAddAttachment.context, R.anim.bounce))
                if (PermissionsUtil.hasStoragePermission(this@ConversationActivity)) {
                    getAction.launch(pickImageIntent)
                } else {
                    showExitSheet(this@ConversationActivity, "The App Needs Storage Permission to access picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
            }
            ivCall.setOnClickListener {
                this@ConversationActivity.callNumberIntent(viewModel.supportProfile.phoneNumber)
            }
            ivsendMessage.setOnClickListener {
                ivsendMessage.startAnimation(AnimationUtils.loadAnimation(ivsendMessage.context, R.anim.bounce))
                if (edtMessageInputBox.text.isNullOrEmpty()) {
                    return@setOnClickListener
                } else {
                    generateMessageObjectAndSend(TEXT, edtMessageInputBox.text.toString().trim())
                }
            }
            edtMessageInputBox.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        binding.rvConversation.scrollToPosition(viewModel.messages.size-1)
                        viewModel.updateTypingStatus(true)
                    } else {
                        this@ConversationActivity.hideKeyboard()
                        viewModel.updateTypingStatus(false)
                    }
                }
        }
    }

    private fun generateMessageObjectAndSend(
        type: String,
        messageContent: String
        ) {
        if (isOnline) {
            val message = Messages(
                id = "",
                fromId = viewModel.profile.id,
                toId = viewModel.supportID,
                message = messageContent,
                type = type,
                timeStamp = System.currentTimeMillis(),
                seen = false
            )
            viewModel.sendMessage(message)
            if (type == TEXT) {
                sendNotification(messageContent)
            } else {
                sendNotification("Sent an image")
            }
            binding.edtMessageInputBox.setText("")
        } else {
            showToast(this, "Please check your Internet Connection")
        }
    }

    private fun setTypingStatus(supportProfile: SupportProfile) {
        binding.apply {
            ivProfileImage.loadImg(supportProfile.thumbnailUrl) {}
            tvProfileName.text = supportProfile.profileName
            if (supportProfile.typing) {
                tvStatus.text = "typing..."
                tvStatus.setTextColor(ContextCompat.getColor(this@ConversationActivity, R.color.green_base))
            } else {
                checkOnlineStatus(supportProfile)
            }
        }
    }
    override fun openImage(url: String, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, ViewCompat.getTransitionName(thumbnail)!!)
            startActivity(intent, options.toBundle())
        }
    }

    override fun finishAfterTransition() {
        super.finish()
    }

    override fun onBackPressed() {
//        finishAfterTransition()
        viewModel.updateTypingStatus(false)
        super.onBackPressed()
    }

    private fun sendNotification(message: String) {
        val title = viewModel.profile.name
        PushNotification(
            NotificationData(title, message, "", CUSTOMER_SUPPORT),
            viewModel.token
        ).also {
            sendNotification(it)
        }
    }

    private fun sendNotification(notification: PushNotification) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                hideProgressDialog()
                showToast(this@ConversationActivity, "Message Broadcast complete")
            } else {
                Log.e("TAG", response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
        }
    }
    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "image" -> {
                generateMessageObjectAndSend(IMAGE, data as String)
                showToast(this, "Image sent")
                hideProgressDialog()
            }
        }
        viewModel.setEmptyStatus()
    }

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "image" -> {
                hideProgressDialog()
                showToast(this, "Server Error! Failed to upload Image")
            }
            "token" -> showErrorSnackBar(data as String, true, LONG)
            "hide" -> {
                hideProgressDialog()
                delay(200)
                showErrorSnackBar("Sorry there is a problem with server. Please contact later", true)
            }
        }
        viewModel.setEmptyStatus()
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val imageMessage = result.data?.data
        imageMessage?.let {
            compressImageToNewFile(this, it)?.let { file ->
                viewModel.tempImageFile = file
                viewModel.updateProfileWithPic(
                    file.toUri(),
                    ".jpg"
                )}
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            if(
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showToast(this, "Storage Permission Granted")
                getAction.launch(pickImageIntent)
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

    override fun onPause() {
        SharedPref(this).putData(ONLINE_STATUS, BOOLEAN, false)
        viewModel.updateProfileStatus(false, System.currentTimeMillis())
        super.onPause()
    }

    override fun onResume() {
        SharedPref(this).putData(ONLINE_STATUS, BOOLEAN, true)
        viewModel.updateProfileStatus( true)
        super.onResume()
    }
}