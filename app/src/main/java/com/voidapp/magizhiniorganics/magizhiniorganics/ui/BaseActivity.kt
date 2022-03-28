package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ncorti.slidetoact.SlideToActView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.business.contacts.ContactUsActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SHORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadOutAnimation
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil

open class BaseActivity : AppCompatActivity() {

    private lateinit var mProgressDialog: Dialog
    private lateinit var exitBottomSheetDialog: BottomSheetDialog
    private lateinit var mSuccessDialog: Dialog
    private lateinit var mDescriptionBottomSheet: BottomSheetDialog
    private lateinit var mSwipeConfirmationBottomSheet: BottomSheetDialog
    private lateinit var listBottomSheetDialog: BottomSheetDialog

    fun View.visible() {
        fadInAnimation(300) {
            this.visibility = View.VISIBLE
        }
    }

    fun View.hide() {
        this.visibility = View.INVISIBLE
    }

    fun View.remove() {
        fadOutAnimation(300) {
            this.visibility = View.GONE
        }
    }

    fun View.enable() {
        this.isEnabled = true
    }

    fun View.disable() {
        this.isEnabled = false
    }

    val pickImageIntent = Intent(
        Intent.ACTION_PICK,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    )

    fun Activity.openAppSettingsIntent() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${this.packageName}")
    ).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }

    fun Activity.callNumberIntent(number: String) = Intent(Intent.ACTION_DIAL).also {
        it.data = Uri.parse("tel:$number")
        startActivity(it)
    }

//    fun isKeyboardVisible(activity: Activity): Boolean {
//        var isVisible: Boolean = false
//        val listener = KeyboardVisibilityEvent.setEventListener(activity) {
//            it
//        }
//        KeyboardVisibilityEvent.setEventListener(activity) { isOpen ->
//            isVisible = isOpen
//        }
//
//        return isVisible
//    }

    fun Activity.hideKeyboard() = UIUtil.hideKeyboard(this)

    fun showToast(context: Context, message: String, type: String = Constants.SHORT) {
        when (type) {
            Constants.SHORT -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            Constants.LONG -> Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun openLink(content: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(content)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

    /**
     * A function to show the success and error messages in snack bar component.
     */
    fun showErrorSnackBar(message: String, errorMessage: Boolean, duration: String = SHORT) {
        val snackBar = if (duration == SHORT) {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        } else {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        }

        val snackBarView = snackBar.view
        val text =
            snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.setTextColor(ContextCompat.getColor(text.context, R.color.white))

        if (errorMessage) {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.matteRed
                )
            )
        } else {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.green_base
                )
            )
        }
        snackBar.show()
    }

    /**
     * This function is used to show the progress dialog with the title and message to user.
     */
    open fun showProgressDialog() {

        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog.setContentView(R.layout.dialog_loading)

//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.animate()

        mProgressDialog.setCancelable(true)
        mProgressDialog.setCanceledOnTouchOutside(false)
        mProgressDialog.window?.setDimAmount(0f)
        mProgressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //Start the dialog and display it on screen.
        mProgressDialog.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    open fun hideProgressDialog() {
//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.cancelAnimation()
        if (mProgressDialog.isShowing) {
            mProgressDialog.dismiss()
        }
    }

    fun showExitSheet(activity: Activity, confirmation: String, data: Any = "") {

        exitBottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomExitConfirmationBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_bottom_exit_confirmation,
                null,
                false
            )

        exitBottomSheetDialog.setCancelable(true)
        exitBottomSheetDialog.setContentView(view.root)

        when (data) {
            "Cancel for some days" -> view.tvCancelText.text = data as String
            "cs" -> {
                view.tvConfirmationText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.gray700
                    )
                )
                view.tvCancelText.text = "Contact Support"
                view.tvCancelText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.matteRed
                    )
                )
            }
            "close" -> {
                exitBottomSheetDialog.setCancelable(false)
                view.tvConfirmationText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.gray700
                    )
                )
                view.tvCancelText.text = "Close"
                view.tvCancelText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.matteRed
                    )
                )
            }
            "okay" -> {
                view.tvConfirmationText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.gray700
                    )
                )
                view.tvCancelText.text = "Proceed"
                view.tvCancelText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.matteRed
                    )
                )
            }
            else -> {
                view.tvConfirmationText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.gray700
                    )
                )
                view.tvCancelText.text = "Proceed"
                view.tvCancelText.setTextColor(
                    ContextCompat.getColor(
                        view.tvConfirmationText.context,
                        R.color.matteRed
                    )
                )
            }
        }

        view.tvConfirmationText.text = confirmation
        view.tvConfirmationText.setOnClickListener {
            hideExitSheet()
            when (activity) {
                is ProfileActivity -> activity.exitProfileWithoutChange()
                is QuickOrderActivity -> activity.onBackPressed()
            }
        }

        view.tvCancelText.setOnClickListener {
            hideExitSheet()
            when (activity) {
                is ProfileActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                        "referral" -> activity.openReferral()
                    }
                }
                is SubscriptionProductActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                        "purchaseHistory" -> activity.navigateToOtherPage(data as String)
                    }
                }
                is ProductActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                    }
                }
                is DishActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                    }
                }
                is PurchaseHistoryActivity -> {
                    when (data) {
                        "cs" -> activity.moveToCustomerSupport()
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                        "close" -> hideExitSheet()
                        "" -> activity.cancellationConfirmed()
                    }
                }
                is ConversationActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                    }
                }
                is QuickOrderActivity -> {
                    when (data) {
                        "permission" -> activity.proceedToRequestPermission()
                        "setting" -> activity.proceedToRequestManualPermission()
                        "estimate" -> activity.sendEstimateRequest()
                        "cs" -> activity.moveToCustomerSupport()
                        "delete" -> activity.deleteQuickOrder()
                        "close" -> activity.onBackPressed()
                        else -> hideExitSheet()
                    }
                }
                is SubscriptionHistoryActivity -> {
                    when (data) {
                        "cs" -> activity.moveToCustomerSupport()
                        "price" -> activity.showPaymentMethod()
                        "close" -> hideExitSheet()
                        "" -> activity.confirmCancellation()
                    }
                }
                is HomeActivity -> activity.showReferralOptions()
                is ContactUsActivity -> {
                    when (data) {
                        "business" -> activity.addNewPartnerAccount()
                        "career" -> activity.sendCareerMail()
                    }
                }
            }
        }
        exitBottomSheetDialog.show()
    }

    private fun hideExitSheet() {
        exitBottomSheetDialog.dismiss()
    }

    fun showSuccessDialog(
        title: String = getString(R.string.msg_profile_activated),
        body: String = getString(R.string.msg_welcome),
        content: String = ""
    ) {

        mSuccessDialog = Dialog(this, R.style.CustomAlertDialog)
        val view: DialogSuccessBinding = DataBindingUtil.inflate(
            LayoutInflater.from(baseContext),
            R.layout.dialog_success,
            null,
            false
        )

        val windowParams = WindowManager.LayoutParams()
        windowParams.copyFrom(mSuccessDialog.window?.attributes)
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.dimAmount = 0.7f
        windowParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

        mSuccessDialog.setContentView(view.root)
        mSuccessDialog.setCanceledOnTouchOutside(false)

        view.tvTitle.text = title
        view.tvBody.text = body
        when (content) {
            "limited" -> {
                view.ltAnimImg.setAnimation(R.raw.validating_purchase)
                view.tvTitle.remove()
            }
            "order" -> {
                view.ltAnimImg.setAnimation(R.raw.placing_order)
                view.tvTitle.remove()
            }
            "complete" -> {
                view.tvTitle.remove()
            }
            "dates" -> {
                view.ltAnimImg.setAnimation(R.raw.validating_dates)
                view.tvTitle.remove()
            }
            "wallet" -> {
                view.ltAnimImg.setAnimation(R.raw.piggy_bank)
                view.tvTitle.remove()
            }
            "upload" -> {
                view.ltAnimImg.setAnimation(R.raw.piggy_bank)
                view.tvTitle.remove()
            }
        }

        mSuccessDialog.show()
        mSuccessDialog.window?.attributes = windowParams
//        mSuccessDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun hideSuccessDialog() {
        mSuccessDialog.dismiss()
    }

    fun showDescriptionBs(content: String) {
        mDescriptionBottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogBottomDescriptionBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_bottom_description,
                null,
                false
            )

        mDescriptionBottomSheet.setCancelable(true)
        mDescriptionBottomSheet.setCanceledOnTouchOutside(true)
        mDescriptionBottomSheet.setContentView(view.root)

        view.tvContent.text = content

        mDescriptionBottomSheet.show()

    }

    fun hideDescriptionBs() {
        mDescriptionBottomSheet.dismiss()
    }

    fun showListBottomSheet(activity: Activity, listItems: ArrayList<String>, data: String = "") {
        listBottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogBottomListBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_bottom_list,
                null,
                false
            )

        val listView = view.lvBottomList

        val adapter: ArrayAdapter<String> =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_activated_1,
                listItems
            )

        listView.adapter = adapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>?,
                                              _: View?,
                                              position: Int,
                                              _: Long ->

                val selectedItem = parent!!.getItemAtPosition(position) as String

                when (activity) {
                    is ShoppingMainActivity -> {
                        hideListBottomSheet()
//                        activity.categoryFilter = selectedItem
                        activity.selectedCategory(selectedItem)
                    }
                    is HomeActivity -> {
                        hideListBottomSheet()
                        when (data) {
                            "referral" -> activity.referralAction(selectedItem)
                            "developer" -> activity.selectedContactMethodForDeveloper(selectedItem)
                        }
                    }
                    is SubscriptionProductActivity -> {
                        hideListBottomSheet()
                        activity.selectedPaymentMode(selectedItem)
                    }
                    is SubscriptionHistoryActivity -> {
                        hideListBottomSheet()
                        when (data) {
                            "filter" -> activity.setSubscriptionFilter(selectedItem)
                            "payment" -> activity.selectedPaymentMode(selectedItem)
                        }
                    }
                    is QuickOrderActivity -> {
                        hideListBottomSheet()
                        activity.selectedPaymentMode(selectedItem)
                    }
                    is InvoiceActivity -> {
                        hideListBottomSheet()
                        activity.selectedPaymentMode(selectedItem)
                    }
                    is ContactUsActivity -> {
                        activity.selectedContactMethod(selectedItem)
                    }
                }

            }

        listBottomSheetDialog.setContentView(view.root)
        listBottomSheetDialog.setCanceledOnTouchOutside(true)
        listBottomSheetDialog.show()
    }


    private fun hideListBottomSheet() {
        listBottomSheetDialog.dismiss()
    }

    fun showSwipeConfirmationDialog(activity: Activity, content: String) {
        mSwipeConfirmationBottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogSwipeConfirmationBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_swipe_confirmation,
                null,
                false
            )

        view.swipe.text = content

        view.swipe.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                when (activity) {
                    is SubscriptionProductActivity -> {
                        mSwipeConfirmationBottomSheet.dismiss()
                        activity.approved(true)
                    }
                    is InvoiceActivity -> {
                        mSwipeConfirmationBottomSheet.dismiss()
                        activity.approved()
                    }
                    is SubscriptionHistoryActivity -> {
                        mSwipeConfirmationBottomSheet.dismiss()
                        activity.approved(true)
                    }
                }
            }
        }

        mSwipeConfirmationBottomSheet.dismissWithAnimation = true
        mSwipeConfirmationBottomSheet.setCancelable(true)
        mSwipeConfirmationBottomSheet.setCanceledOnTouchOutside(true)
        mSwipeConfirmationBottomSheet.setContentView(view.root)

        mSwipeConfirmationBottomSheet.show()
    }
}