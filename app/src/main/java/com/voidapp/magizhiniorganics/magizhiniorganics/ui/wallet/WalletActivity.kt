package com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.WalletAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityWalletBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CHECKOUT_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CalendarFilterDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class WalletActivity : BaseActivity(), KodeinAware, PaymentResultListener {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityWalletBinding
    private lateinit var viewModel: WalletViewModel
    private val factory: WalletViewModelFactory by instance()

    private lateinit var transactionAdapter: WalletAdapter
    private var mWallet: Wallet = Wallet()
    private val mTransactions = mutableListOf<TransactionHistory>()
    private var mCurrentTransaction = TransactionHistory()
    private var mProfile = UserProfile()
    private var mMoneyToAddInWallet: Float = 0f

    private var mFilterMonth: String =  TimeUtil().getMonth(dateLong = System.currentTimeMillis())
    private var mFilterYear: String = TimeUtil().getYear(dateLong = System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wallet)
        viewModel = ViewModelProvider(this, factory).get(WalletViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)

        with(binding) {
            cvWalletCard.startAnimation(AnimationUtils.loadAnimation(cvWalletCard.context, R.anim.slide_in_right_bounce))
            clBody.startAnimation(AnimationUtils.loadAnimation(clBody.context, R.anim.slide_up))
        }

        Checkout.preload(applicationContext)

        showShimmer()
        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()
        lifecycleScope.launch {
            delay(800)
            clickListeners()
            initLiveData()
            liveDataObservers()
            initRecyclerView()
        }
    }

    private fun liveDataObservers() {
        viewModel.transactions.observe(this, {
            if (it.isNullOrEmpty()) {
                hideShimmer()
                binding.llEmptyLayout.visible()
            } else {
                binding.llEmptyLayout.remove()
                mTransactions.clear()
                mTransactions.addAll(it)
                val history = it.filter { transactions ->
                    transactions.month == TimeUtil().getMonth()
                }
                transactionAdapter.transactions = history.sortedByDescending { trans ->
                    trans.timestamp
                }
                transactionAdapter.notifyDataSetChanged()
                hideShimmer()
            }
        })
        viewModel.profile.observe(this, {
            mProfile = it
        })
        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> {
                        if (result.message == "") {
                            showProgressDialog()
                        } else {
                            showSuccessDialog("", result.message, result.data)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun displayWalletDataToScreen() {
        with(binding) {
            tvWalletTotal.text = mWallet.amount.toString()
            if (mWallet.lastRecharge == 0L) {
                tvLastRechargeDate.text = "-"
            } else {
                tvLastRechargeDate.text = TimeUtil().getCustomDate(dateLong = mWallet.lastRecharge)
            }
            if (mWallet.lastTransaction == 0L) {
                tvTransactionDate.text = "-"
            } else {
                tvTransactionDate.text = TimeUtil().getCustomDate(dateLong = mWallet.lastTransaction)
            }

        }
    }

    private fun initLiveData() {
        val id = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()
        viewModel.getTransactions(id)
        viewModel.getWallet(id)
        viewModel.getUserProfileData()
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
            CalendarFilterDialog(this, this, mFilterMonth, mFilterYear).show()
        }

        binding.ivInfo.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            showDescriptionBs("ABOUT MAGIZHINI WALLET \n \n \n Magizhini Wallet provides you a safe way of transaction for Purchases and Subscriptions. All your promotional cashback and refunds will be transferred to wallet and can be used for purchasing any item in Magizhini Store.")
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
            //BS to add Amount number
            val dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

            val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(LayoutInflater.from(applicationContext),R.layout.dialog_bottom_add_referral,null,false)
            dialogBsAddReferral.setCancelable(true)
            dialogBsAddReferral.setContentView(view.root)
            dialogBsAddReferral.dismissWithAnimation = true

            view.etlReferralNumber.hint = "Amount"
            view.etlReferralNumber.setStartIconDrawable(ContextCompat.getDrawable(view.etlReferralNumber.context, R.drawable.ic_wallet))
//            view.etlReferralNumber.setStartIconTintList(ColorStateList.valueOf(ContextCompat.getColor(view.etlReferralNumber.context, R.color.green_base)))
            view.btnApply.text = "PROCEED"

            //verifying if the amount number is empty and sending for payment
            view.btnApply.setOnClickListener {
                val code = view.etReferralNumber.text.toString().trim()
                if (code.isEmpty()) {
                    view.etlReferralNumber.error = "* Enter valid Amount"
                    return@setOnClickListener
                } else {
                    mMoneyToAddInWallet = view.etReferralNumber.text.toString().toFloat()
                    dialogBsAddReferral.dismiss()
                    with(mProfile) {
                        startPayment(
                            this@WalletActivity,
                            mailId,
                            mMoneyToAddInWallet * 100,
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
                    }
                }
            }
            dialogBsAddReferral.show()
        }
    }

    override fun onPaymentSuccess(orderID: String?) {
        showSuccessDialog("","Adding Money to the Wallet...", "wallet")
        showShimmer()
        viewModel.makeTransactionFromWallet(
            mMoneyToAddInWallet,
            mProfile.id,
            orderID!!,
            "Add"
        )
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    fun filterTransactions(month: String, year: String) = lifecycleScope.launch(Dispatchers.Default) {
//        withContext(Dispatchers.Main) {
//            showShimmer()
//        }
//        val filteredTransactions = mutableListOf<TransactionHistory>()
//        for (transaction in mTransactions) {
//            val transactionDate = TimeUtil().getCustomDate(dateLong = transaction.timestamp)
//            val filteredDate = TimeUtil().getCustomDate(dateLong = date)
//            if (transactionDate == filteredDate) {
//                filteredTransactions.add(transaction)
//            }
//        }
//        withContext(Dispatchers.Main) {
//            if (filteredTransactions.isNullOrEmpty()) {
//                binding.llEmptyLayout.visible()
//            } else {
//                binding.llEmptyLayout.remove()
//                filteredTransactions.sortBy {
//                    it.timestamp
//                }
//            }
//            transactionAdapter.transactions = filteredTransactions
//            transactionAdapter.notifyDataSetChanged()
//            hideShimmer()
//        }
        withContext(Dispatchers.Main) {
            showShimmer()
            mFilterMonth = month
            mFilterYear = year
        }
        val filteredTransactions = mutableListOf<TransactionHistory>()
        for (transaction in mTransactions) {
            if (transaction.month == mFilterMonth && transaction.year == mFilterYear.toLong()) {
                filteredTransactions.add(transaction)
            }
        }
        withContext(Dispatchers.Main) {
            if (filteredTransactions.isNullOrEmpty()) {
                binding.llEmptyLayout.visible()
            } else {
                binding.llEmptyLayout.remove()
                filteredTransactions.sortBy {
                    it.timestamp
                }
            }
            transactionAdapter.transactions = filteredTransactions
            transactionAdapter.notifyDataSetChanged()
            hideShimmer()
        }
    }

    fun setMonthFilter(month: String) = lifecycleScope.launch(Dispatchers.Default) {
//        withContext(Dispatchers.Main) {
//            showShimmer()
//            binding.tvMonthFilter.text = month
//        }
//        val filteredTransactions = mutableListOf<TransactionHistory>()
//        for (transaction in mTransactions) {
//            if (transaction.month == month) {
//                filteredTransactions.add(transaction)
//            }
//        }
//        withContext(Dispatchers.Main) {
//            if (filteredTransactions.isNullOrEmpty()) {
//                binding.llEmptyLayout.visible()
//            } else {
//                binding.llEmptyLayout.remove()
//                filteredTransactions.sortBy {
//                    it.timestamp
//                }
//            }
//            transactionAdapter.transactions = filteredTransactions
//            transactionAdapter.notifyDataSetChanged()
//            hideShimmer()
//        }
    }

    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "wallet" -> {
                mWallet = data as Wallet
                displayWalletDataToScreen()
            }
            "transaction" -> {
                delay(1500)
                hideSuccessDialog()
                showSuccessDialog("", "Wallet Updated successfully!", "complete")
                mCurrentTransaction = data as TransactionHistory
                viewModel.updateTransaction(data)
            }
            "transactionID" -> {
                mCurrentTransaction.id = data as String
                mTransactions.add(mCurrentTransaction)
                hideShimmer()
                viewModel.getWallet(mProfile.id)
                delay(1500)
                hideSuccessDialog()
            }
        }

        viewModel.emptyResult()
    }

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "wallet" -> {
                showErrorSnackBar(data as String, true)
            }
            "transaction" -> {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "transactionID" -> {
                delay(1000)
                hideSuccessDialog()
                showExitSheet(this, "Server Error! Could not record wallet transaction. \n \n If Money is already debited from Wallet, Please contact customer support and the transaction will be reverted in 24 Hours", "cs")
            }
        }
        viewModel.emptyResult()
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            rvTransactionHistory.hide()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            rvTransactionHistory.visible()
        }
    }

    override fun onBackPressed() {
        when(viewModel.navigateToPage) {
            CHECKOUT_PAGE -> {
                Intent(this, InvoiceActivity::class.java).also {
                    it.putExtra(NAVIGATION, ALL_PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
            }
            QUICK_ORDER -> finish()
            else -> finish()
        }
    }
}