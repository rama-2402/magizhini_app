package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ConversationAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messages
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityConversationBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ONLINE_STATUS
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.IOException

class ConversationActivity : BaseActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityConversationBinding
    private val factory: ConversationViewModelFactory by instance()
    private lateinit var viewModel: ConversationViewModel
    private lateinit var adapter: ConversationAdapter

    private var mSelectedProfile: SupportProfile = SupportProfile()
    private var mCurrentUserId: String = ""
    private val mMessages: ArrayList<Messages> = arrayListOf()
    private var isZoomed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        viewModel = ViewModelProvider(this, factory).get(ConversationViewModel::class.java)
        binding.viewmodel = viewModel

        mSelectedProfile = intent.getParcelableExtra(Constants.CUSTOMER_SUPPORT)!!
        viewModel.currentUserName = intent.getStringExtra(Constants.PROFILE_NAME)!!
        mCurrentUserId = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()
        SharedPref(this).putData(ONLINE_STATUS, Constants.BOOLEAN, true)

        setRecyclerView()
        initLiveData()
        setDataToViews()
        clickListeners()
    }

    private fun initLiveData() {
        viewModel.getConversation(mCurrentUserId, mSelectedProfile.id)
        viewModel.updateProfileStatus(mCurrentUserId, true)
        viewModel.getSupportProfileUpdates(mSelectedProfile.id)
        viewModel.conversation.observe(this, {
            mMessages.add(it)
            adapter.messages = mMessages
            adapter.notifyItemChanged(mMessages.size)
            binding.rvCustomerId.scrollToPosition(mMessages.size-1)
        })
        viewModel.supportProfileStatus.observe(this, {
            checkTypingStatus(it)
        })
        viewModel.keyboardVisibile.observe(this, {
            UIUtil.hideKeyboard(this@ConversationActivity)
        })
        viewModel.imageUploadStatus.observe(this, {
            if (it == "failed") {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Image Update failed. Try later", true)
            } else {
                hideProgressDialog()
                generateMessageObjectAndSend(Constants.IMAGE, it)
            }
        })
        viewModel.imageUrl.observe(this, {
            GlideLoader().loadUserPictureWithoutCrop(binding.ivReviewImageChat.context, it, binding.ivReviewImageChat)
            binding.ivReviewImageChat.startAnimation(Animations.scaleBig)
            binding.ivReviewImageChat.visible()
            isZoomed = true
        })
    }

    private fun checkTypingStatus(profile: SupportProfile) {
        if (profile.typing) {
            binding.tvStatus.text = "typing..."
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.matteGreen))
        } else {
           checkOnlineStatus(profile)
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

    private fun setRecyclerView() {
        val messages: ArrayList<Messages> = arrayListOf()
        adapter = ConversationAdapter(
            this,
            messages,
            viewModel
        )
        binding.rvCustomerId.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerId.adapter = adapter
        binding.rvCustomerId.scrollToPosition(mMessages.size-1)
    }

    private fun clickListeners() {
        with(binding) {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivAddAttachment.setOnClickListener {
                ivAddAttachment.startAnimation(AnimationUtils.loadAnimation(ivAddAttachment.context, R.anim.bounce))
//                PermissionsUtil.checkStoragePermission(this@ConversationActivity)
            }
            ivsendMessage.setOnClickListener {
                ivsendMessage.startAnimation(AnimationUtils.loadAnimation(ivsendMessage.context, R.anim.bounce))
                validateText()
            }
            edtMessageInputBox.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        UIUtil.hideKeyboard(this@ConversationActivity)
                        binding.rvCustomerId.scrollToPosition(mMessages.size-1)
                        viewModel.updateTypingStatus(mCurrentUserId, true)
                    } else {
                        UIUtil.hideKeyboard(this@ConversationActivity)
                        viewModel.updateTypingStatus(mCurrentUserId, false)
                    }
                }
            ivReviewImageChat.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun validateText() {
        with(binding) {
            if (edtMessageInputBox.text.isNotEmpty()) {
                generateMessageObjectAndSend(messageContent = binding.edtMessageInputBox.text.toString().trim())
            } else {
                UIUtil.hideKeyboard(this@ConversationActivity)
            }
        }
    }

    private fun generateMessageObjectAndSend(
        type: String = Constants.TEXT,
        messageContent: String
        ) {
        val message = Messages(
            id = "",
            fromId = mCurrentUserId,
            toId = mSelectedProfile.id,
            message = messageContent,
            type = type,
            timeStamp = System.currentTimeMillis(),
            seen = false
        )
        viewModel.sendMessage(message)
        binding.edtMessageInputBox.setText("")
    }

    private fun setDataToViews() {
        with(binding) {
            GlideLoader().loadUserPicture(this@ConversationActivity, mSelectedProfile.thumbnailUrl, ivProfileImage)
            tvProfileName.text = mSelectedProfile.profileName
        }
    }

    override fun onBackPressed() {
        when {
            isZoomed -> {
                binding.ivReviewImageChat.startAnimation(Animations.scaleSmall)
                binding.ivReviewImageChat.remove()
                isZoomed = false
            }
            else -> {
                viewModel.updateTypingStatus(mCurrentUserId, false)
                SharedPref(this).putData(ONLINE_STATUS, Constants.BOOLEAN, false)
                Intent(this, ChatActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GlideLoader().showImageChooser(this)
            } else {
                //Displaying another toast if permission is not granted
                showErrorSnackBar("Storage Permission Denied!", true)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.PICK_IMAGE_REQUEST_CODE) {
                if (data != null) {
                    try {
                        // The uri of selected image from phone storage.
                        showProgressDialog()
                        viewModel.uploadImageToStorage(
                            mCurrentUserId ,
                            data.data!!,
                            GlideLoader().imageExtension(this,  data.data!!)!!
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                        showErrorSnackBar("Image selection failed!", true)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // A log is printed when user close or cancel the image selection.
            Log.e("Request Cancelled", "Image selection cancelled")
        }
    }
}