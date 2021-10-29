package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ncorti.slidetoact.SlideToActView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants

open class BaseActivity : AppCompatActivity() {

    private lateinit var mProgressDialog: Dialog
    private lateinit var ExitBottomSheetdialog: BottomSheetDialog
    private lateinit var mSuccessDialog: Dialog
    private lateinit var mDescriptionBottomSheet: BottomSheetDialog
    private lateinit var mSwipeConfirmationBottomSheet: BottomSheetDialog
    private lateinit var listBottomSheetdialog: BottomSheetDialog


    fun View.show() {
        this.visibility = View.VISIBLE
    }

    fun View.hide() {
        this.visibility = View.INVISIBLE
    }

    fun View.gone() {
        this.visibility = View.GONE
    }

    fun View.enable() {
        this.isEnabled = true
    }

    fun View.disable() {
        this.isEnabled = false
    }

    fun showToast(context: Context ,message: String, type: String) {
        when(type) {
            Constants.SHORT -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            Constants.LONG -> Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun openLink(content: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.data = Uri.parse(content)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

    /**
     * A function to show the success and error messages in snack bar component.
     */
    fun showErrorSnackBar(message: String, errorMessage: Boolean, duration: String = "short") {
        val snackBar = if (duration == "short") {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        } else {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        }

        val snackBarView = snackBar.view
        val text = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.setTextColor(ContextCompat.getColor(text.context, R.color.white))

        if (errorMessage) {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.matteRed
                )
            )
        }else{
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
    fun showProgressDialog() {

        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog.setContentView(R.layout.dialog_loading)

//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.animate()

        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)
        mProgressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        //Start the dialog and display it on screen.
        mProgressDialog.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    fun hideProgressDialog() {
//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.cancelAnimation()
        mProgressDialog.dismiss()
    }

    fun showExitSheet(activity: Activity, confirmation: String, data: Any = "") {

        ExitBottomSheetdialog = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomExitConfirmationBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_bottom_exit_confirmation,
                null,
                false)

        ExitBottomSheetdialog.setCancelable(true)
        ExitBottomSheetdialog.setContentView(view.root)

        when (data) {
            "Cancel for some days" -> view.tvCancelText.text = data as String
            "cs" -> {
                view.tvConfirmationText.setTextColor(ContextCompat.getColor(view.tvConfirmationText.context, R.color.gray700))
                view.tvCancelText.text = "DONE"
                view.tvCancelText.setTextColor(ContextCompat.getColor(view.tvConfirmationText.context, R.color.matteRed))
            }
        }

        view.tvConfirmationText.text = confirmation
        view.tvConfirmationText.setOnClickListener {
            when(activity) {
                is ProfileActivity -> activity.exitProfileWithoutChange()
                is PurchaseHistoryActivity -> {
                    when(data) {
                        "cs" -> activity.moveToCustomerSupport()
                        else -> activity.cancellationConfirmed()

                    }
                }
                is SubscriptionHistoryActivity -> activity.confirmCancellation()
            }
        }

        view.tvCancelText.setOnClickListener {
            hideExitSheet()
        }

        ExitBottomSheetdialog.show()
    }

    fun hideExitSheet() {
        ExitBottomSheetdialog.dismiss()
    }

    fun showSuccessDialog(title: String = getString(R.string.msg_profile_activated),
                          body: String = getString(R.string.msg_welcome),
                          content: String = "") {

        mSuccessDialog = Dialog(this, R.style.CustomAlertDialog)
        val view: DialogSuccessBinding = DataBindingUtil.inflate(
                                            LayoutInflater.from(baseContext),
                                            R.layout.dialog_success,
                                            null,
                                            false)

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
        when(content) {
            "limited" -> {
                view.ltAnimImg.setAnimation(R.raw.validating_purchase)
                view.tvTitle.gone()
            }
            "order" -> {
                view.ltAnimImg.setAnimation(R.raw.placing_order)
                view.tvTitle.gone()
            }
            "complete" -> {
                view.tvTitle.gone()
            }
            "dates" -> {
                view.ltAnimImg.setAnimation(R.raw.validating_dates)
                view.tvTitle.gone()
            }
            "wallet" -> {
                view.ltAnimImg.setAnimation(R.raw.piggy_bank)
                view.tvTitle.gone()
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
                false)

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
        listBottomSheetdialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogBottomListBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_bottom_list,
                null,
                false
            )

        val listView = view.lvBottomList

        val adapter : ArrayAdapter<String> =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_activated_1,
                listItems
            )

        listView.adapter = adapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener {
                    parent: AdapterView<*>?,
                    _: View?,
                    position: Int,
                    _: Long ->

                val selectedItem = parent!!.getItemAtPosition(position) as String

                when(activity) {
                    is ShoppingMainActivity -> {
                        hideListBottomSheet()
                        activity.categoryFilter = selectedItem
                        activity.setFilteredProducts()
                    }
                    is WalletActivity -> {
                        hideListBottomSheet()
                        activity.setMonthFilter(selectedItem)
                    }
                    is PurchaseHistoryActivity -> {
                        hideListBottomSheet()
                        if (data == "years") {
                            activity.setYearFilter(selectedItem)
                        } else if (data == "months") {
                            activity.setMonthFilter(selectedItem)
                        }
                    }
                    is SubscriptionProductActivity -> {
                        hideListBottomSheet()
                        activity.setPaymentFilter(selectedItem)
                    }
                    is SubscriptionHistoryActivity -> {
                        hideListBottomSheet()
                        when(data) {
                            "filter" -> activity.setSubscriptionFilter(selectedItem)
                            "payment" -> activity.selectedPaymentMode(selectedItem)
                        }
                    }
                }

            }

        listBottomSheetdialog.setContentView(view.root)
        listBottomSheetdialog.setCanceledOnTouchOutside(true)
        listBottomSheetdialog.show()
    }


    private fun hideListBottomSheet() {
        listBottomSheetdialog.dismiss()
    }

    fun showSwipeConfirmationDialog(activity: Activity, content: String) {
        mSwipeConfirmationBottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogSwipeConfirmationBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_swipe_confirmation,
                null,
                false)

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
                        activity.approved(true)
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