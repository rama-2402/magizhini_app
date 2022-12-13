package com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.WalletAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityWalletBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CalendarFilterDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.CalendarFilerDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.REFERRAL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class WalletActivity : BaseActivity(), KodeinAware, PaymentResultListener,
    CalendarFilerDialogClickListener,
    CustomAlertClickListener
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityWalletBinding
    private lateinit var viewModel: WalletViewModel
    private val factory: WalletViewModelFactory by instance()

    private lateinit var transactionAdapter: WalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wallet)
        viewModel = ViewModelProvider(this, factory).get(WalletViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)

        with(binding) {
            cvWalletCard.startAnimation(
                AnimationUtils.loadAnimation(
                    cvWalletCard.context,
                    R.anim.slide_in_right_bounce
                )
            )
            clBody.startAnimation(AnimationUtils.loadAnimation(clBody.context, R.anim.slide_up))
            tvTransactionDate.isSelected = true
            tvLastRechargeDate.isSelected = true
        }

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            onBackPressed()
        }

        Checkout.preload(applicationContext)

        lifecycleScope.launch {
            delay(1250)
            initRecyclerView()
            updateChip(WALLET)
            liveDataObservers()
            clickListeners()
        }
    }

    private fun initData() {
        showProgressDialog(true)
        showShimmer()
        val tempUserID =
            SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()
        viewModel.userID = if (tempUserID == "") {
            showSkippedUserDialog()
            ""
        } else {
            tempUserID
        }
        viewModel.getWallet()
    }

    private fun showSkippedUserDialog() {
        CustomAlertDialog(
            this,
            "User Not Signed In",
            "You must be signed in with your Google Account to use Magizhini Wallet Feature. Please click SIGN IN WITH GOOGLE button to signin with your google account.",
            "Sign In with Google",
            "newID",
            this
        ).show()
    }

    private fun initReferral() {
        showProgressDialog(true)
        showShimmer()
        viewModel.getReferralTransactions()
    }

    private fun liveDataObservers() {
        viewModel.uiUpdate.observe(this) { event ->
            when (event) {
                is WalletViewModel.UiUpdate.PopulateWalletData -> {
                    hideProgressDialog()
                    event.wallet?.let {
                        viewModel.userWallet = it
                        displayWalletDataToScreen(it)
                    } ?: showErrorSnackBar(event.message.toString(), true)

                    viewModel.getTransactions()
                }
                is WalletViewModel.UiUpdate.PopulateTransactions -> {
                    if (event.transactions.isNullOrEmpty()) {
                        hideShimmer()
                        binding.llEmptyLayout.visible()
                    } else {
                        binding.llEmptyLayout.remove()
                        transactionAdapter.transactions = event.transactions
                        transactionAdapter.notifyDataSetChanged()
                        hideShimmer()
                    }
                }
                is WalletViewModel.UiUpdate.UpdateUserProfileData -> {
                    hideProgressDialog()
                    event.profile?.let {
                        with(it) {
                            viewModel.moneyToAddInWallet?.let { amount ->
                                startPayment(
                                    this@WalletActivity,
                                    mailId,
                                    amount * 100,
                                    name,
                                    id,
                                    phNumber
                                ).also { status ->
                                    if (!status) {
                                        Toast.makeText(
                                            this@WalletActivity,
                                            "Error in processing payment. Try Later ",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } ?: showErrorSnackBar(
                                "Failed to add money to wallet. Please contact customer support for further assistance",
                                true
                            )
                        }
                    } ?: showErrorSnackBar("Failed to fetch your profile data. Try later", true)
                }
                is WalletViewModel.UiUpdate.TransactionStatus -> {
                    if (event.status) {
                        updateLoadStatusDialog("Wallet Recharged Successfully!", event.data!!)
                    } else {
                        updateLoadStatusDialog(event.message.toString(), "fail")
                        showExitSheet(
                            this,
                            "Server Error! Could not record wallet transaction. \n \n If Money is already debited from account, Please contact customer support and the transaction will be reverted in 24 Hours",
                            "cs"
                        )
                    }
                }
                is WalletViewModel.UiUpdate.ShowLoadStatusDialog -> {
                    LoadStatusDialog.newInstance("", event.message!!, event.data!!).show(
                        supportFragmentManager,
                        Constants.LOAD_DIALOG
                    )
                }
                is WalletViewModel.UiUpdate.UpdateLoadStatusDialog ->
                    updateLoadStatusDialog(event.message!!, event.data!!)
                is WalletViewModel.UiUpdate.DismissDialog -> {
                    dismissLoadStatusDialog()
                    showProgressDialog(false)
                    viewModel.getWallet()
                    showShimmer()
                }
                is WalletViewModel.UiUpdate.PopulateReferralBonusDetails -> {
                    hideProgressDialog()
                    binding.apply {
                        tvTransactionText.text = "Referral Earnings: "
                        tvLastRecharge.text = "Total Referrals: "
                        event.data?.let {
                            tvTransactionDate.text = "Rs: ${Utils.roundPrice(it.totalBonus.toFloat())}"
                            tvLastRechargeDate.text = if (it.referrals.isNullOrEmpty()) "None" else it.referrals.toString()
                        } ?: let {
                            tvTransactionDate.text = "Rs: 0.00"
                            tvLastRechargeDate.text = "0"
                        }
                    }
                }
                is WalletViewModel.UiUpdate.HowToVideo -> {
                    hideProgressDialog()
                    if (event.url == "") {
                        showToast(
                            this,
                            "demo video will be available soon. sorry for the inconvenience."
                        )
                    } else {
                        openInBrowser(event.url)
                    }

                }
                is WalletViewModel.UiUpdate.EmptyUI -> return@observe
                else -> viewModel.setEmptyStatus()
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

    private fun displayWalletDataToScreen(wallet: Wallet) {
        with(binding) {
            tvWalletTotal.text = wallet.amount.toString()
            tvTransactionText.text = "Last Transaction: "
            tvLastRecharge.text = "Last Recharge: "
            if (wallet.lastRecharge == 0L) {
                tvLastRechargeDate.text = "-"
            } else {
                tvLastRechargeDate.text = TimeUtil().getCustomDate(dateLong = wallet.lastRecharge)
            }
            if (wallet.lastTransaction == 0L) {
                tvTransactionDate.text = "-"
            } else {
                tvTransactionDate.text = TimeUtil().getCustomDate(dateLong = wallet.lastTransaction)
            }
        }
    }

    private fun initRecyclerView() {

        transactionAdapter = WalletAdapter(
            this,
            listOf()
        )

        binding.rvTransactionHistory.adapter = transactionAdapter
        binding.rvTransactionHistory.layoutManager = LinearLayoutManager(this)
//        val divider: DividerItemDecoration = DividerItemDecoration(binding.rvTransactionHistory.context,
//        DividerItemDecoration.VERTICAL)
//        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_line)!!)
//        binding.rvTransactionHistory.addItemDecoration(divider)
    }

    private fun clickListeners() {
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.ivCart.setOnClickListener {
            Intent(this, InvoiceActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }

        binding.ivWalletFilter.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            showCalendarFilterDialog(viewModel.filteredMonth, viewModel.filteredYear)
        }

        binding.ivInfo.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            if (binding.tvTransactionText.text == "Referral Earnings: ") {
                showDescriptionBs("ABOUT MAGIZHINI REFERRAL REWARDS \n \n \n With Magizhini Referral Rewards Program you will be rewarded every time any one of your referrals makes a Purchases in Magizhini Organics. This is a smart way of having a PASSIVE INCOME by referring Magizhini Organics to new customers using your mobile number as a referral ID. \n \n \n Referral Rewards will be added to your Magizhini Wallet which can be later used for your future purchases. For more Information please contact Magizhini Customer Support.")
            } else {
                showDescriptionBs("ABOUT MAGIZHINI WALLET \n \n \n Magizhini Wallet provides you a safe way of transaction for Purchases and Subscriptions. All your promotional cashback and refunds will be transferred to wallet and can be used for purchasing any item in Magizhini Store.")
            }
        }

        binding.rvTransactionHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                super.onScrolled(recyclerView, up, down)
                if (down < 0 && binding.fabAddMoney.isGone) {
                    binding.fabAddMoney.startAnimation(scaleBig)
                    binding.fabAddMoney.visibility = View.VISIBLE
                } else if (down > 0 && binding.fabAddMoney.isVisible) {
                    binding.fabAddMoney.startAnimation(scaleSmall)
                    binding.fabAddMoney.visibility = View.GONE
                }
            }
        })

        binding.fabAddMoney.setOnClickListener {
            if (!NetworkHelper.isOnline(this)) {
                showErrorSnackBar("Please check your Internet Connection", true)
                return@setOnClickListener
            }
            //BS to add Amount number
            val dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

            val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(
                LayoutInflater.from(applicationContext),
                R.layout.dialog_bottom_add_referral,
                null,
                false
            )
            dialogBsAddReferral.setCancelable(true)
            dialogBsAddReferral.setContentView(view.root)
            dialogBsAddReferral.dismissWithAnimation = true

            view.etlReferralNumber.hint = "Amount"
            view.etlReferralNumber.startIconDrawable =
                ContextCompat.getDrawable(view.etlReferralNumber.context, R.drawable.ic_wallet)
            view.btnApply.text = "RECHARGE"

            //verifying if the amount number is empty and sending for payment
            view.btnApply.setOnClickListener {
                val code = view.etReferralNumber.text.toString().trim()
                if (code.isEmpty()) {
                    view.etlReferralNumber.error = "* Enter valid Amount"
                    return@setOnClickListener
                } else {
                    if (!NetworkHelper.isOnline(this)) {
                        showToast(this, "Please check your Internet Connection")
                        return@setOnClickListener
                    }
                    showProgressDialog(false)
                    viewModel.moneyToAddInWallet = view.etReferralNumber.text.toString().toFloat()
                    dialogBsAddReferral.dismiss()
                    viewModel.getUserProfileData()
                }
            }
            dialogBsAddReferral.show()
        }

        binding.cpReferral.setOnClickListener {
            updateChip(REFERRAL)
        }

        binding.cpWallet.setOnClickListener {
            updateChip(WALLET)
        }

//        binding.ivHowTo.setOnClickListener {
//            showProgressDialog(true)
//            viewModel.getHowToVideo("Wallet")
//        }
    }

    override fun onPaymentSuccess(orderID: String?) {
        viewModel.moneyToAddInWallet?.let {
            viewModel.makeTransactionFromWallet(
                it,
                viewModel.userID,
                orderID!!,
                "Add"
            )
        } ?: showExitSheet(
            this,
            "Transaction Complete. If the amount is not reflecting in your wallet, Please contact customer support for further assistance",
            "cs"
        )
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun updateLoadStatusDialog(message: String, data: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    fun moveToCustomerSupport() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
        }
    }

    private fun showCalendarFilterDialog(month: String, year: Long) {
        CalendarFilterDialog.newInstance(month, year.toInt()).show(
            supportFragmentManager,
            "calendar"
        )
    }

    private fun dismissCalendarFilterDialog() {
        (supportFragmentManager.findFragmentByTag("calendar") as? DialogFragment)?.dismiss()
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            flShimmerPlaceholder.startShimmer()
            rvTransactionHistory.hide()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            flShimmerPlaceholder.stopShimmer()
            rvTransactionHistory.visible()
        }
    }

    private fun updateChip(chip: String) {
        binding.apply {
            when (chip) {
                WALLET -> {
                    cpWallet.chipBackgroundColor =
                        resources.getColorStateList(R.color.matteRed, null)
                    cpWallet.setTextColor(
                        ContextCompat.getColor(
                            this@WalletActivity,
                            R.color.white
                        )
                    )
                    cpReferral.chipBackgroundColor =
                        resources.getColorStateList(R.color.green_light, null)
                    cpReferral.setTextColor(
                        ContextCompat.getColor(
                            this@WalletActivity,
                            R.color.green_base
                        )
                    )
                    fabAddMoney.show()
                    initData()
                }
                REFERRAL -> {
                    cpReferral.chipBackgroundColor =
                        resources.getColorStateList(R.color.matteRed, null)
                    cpReferral.setTextColor(
                        ContextCompat.getColor(
                            this@WalletActivity,
                            R.color.white
                        )
                    )
                    cpWallet.chipBackgroundColor =
                        resources.getColorStateList(R.color.green_light, null)
                    cpWallet.setTextColor(
                        ContextCompat.getColor(
                            this@WalletActivity,
                            R.color.green_base
                        )
                    )
                    fabAddMoney.hide()
                    initReferral()
                }
            }
        }
    }

    override fun selectedFilter(month: String, year: String) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        dismissCalendarFilterDialog()
        showShimmer()
        viewModel.filteredMonth = month
        viewModel.filteredYear = year.toLong()
        if (binding.tvTransactionText.text == "Referral Earnings: ") {
            viewModel.filterTransactions(REFERRAL)
        } else {
            viewModel.filterTransactions()
        }
    }

    override fun cancelDialog() {
        dismissCalendarFilterDialog()
    }

    override fun onClick() {

    }

    override fun goToSignIn() {
        Intent(this, SignInActivity::class.java).also {
            it.putExtra("goto", "wallet")
            startActivity(it)
            finish()
        }
    }

    override fun closeActivity() {
       finish()
    }
}