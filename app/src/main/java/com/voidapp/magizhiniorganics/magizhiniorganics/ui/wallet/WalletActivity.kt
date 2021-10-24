package com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import kotlinx.coroutines.*
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
    private var mProfile = UserProfile()
    private var mMoneyToAddInWallet: Float = 0f

    private val TAG: String = "qqqq"

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
        clickListeners()
        initLiveData()
        liveDataObservers()
        initRecyclerView()
    }

    private fun liveDataObservers() {
        viewModel.wallet.observe(this, {
            mWallet = it
            displayWalletDataToScreen()

        })
        viewModel.transactions.observe(this, {
            mTransactions.clear()
            mTransactions.addAll(it)
            it.sortedBy { trans ->
                trans.timestamp
            }
            transactionAdapter.transactions = it
            transactionAdapter.notifyDataSetChanged()
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.profile.observe(this, {
            mProfile = it
        })
    }

    private fun displayWalletDataToScreen() {
        with(binding) {
            tvWalletTotal.text = mWallet.amount.toString()
            if (mWallet.lastRecharge == 0L) {
                tvLastRechargeDate.text = "-"
            } else {
                tvLastRechargeDate.text = TimeUtil().getCustomDate(dateLong = mWallet.lastTransaction)
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
            DatePickerLib().pickSingleDate(this)
        }

        binding.tvMonthFilter.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            showListBottomSheet(
                this,
                resources.getStringArray(R.array.months_name).toList() as ArrayList<String>
            )
        }

        binding.ivInfo.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            //TODO: Show some info about wallet and it's functions
        }

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
        lifecycleScope.launch(Dispatchers.Main) {
            val id = viewModel.createTransaction(mMoneyToAddInWallet.toFloat(), mProfile.id, orderID!!)
            if (id == "failed") {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar("Server Error! If money is debited please contact customer support", false)
                return@launch
            } else {
                delay(1500)
                hideSuccessDialog()
                showSuccessDialog("", "Wallet Updated successfully!", "complete")
                TransactionHistory (
                    id,
                    System.currentTimeMillis(),
                    TimeUtil().getMonth(),
                    TimeUtil().getYear().toLong(),
                    mMoneyToAddInWallet.toFloat(),
                    id,
                    orderID,
                    Constants.SUCCESS,
                    Constants.ADD_MONEY,
                    orderID
                ).also {
                    mTransactions.add(it)
                    mTransactions.sortByDescending { transaction ->
                        transaction.timestamp
                    }
                    transactionAdapter.transactions = mTransactions
                    transactionAdapter.notifyDataSetChanged()
                    binding.tvWalletTotal.text = (mWallet.amount + mMoneyToAddInWallet).toString()
                    delay(1500)
                    hideSuccessDialog()
                    hideShimmer()
                }
            }
        }

    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    fun filterDate(date: Long) = lifecycleScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            showShimmer()
        }
        val filteredTransactions = mutableListOf<TransactionHistory>()
        for (transaction in mTransactions) {
            val transactionDate = TimeUtil().getCustomDate(dateLong = transaction.timestamp)
            val filteredDate = TimeUtil().getCustomDate(dateLong = date)
            if (transactionDate == filteredDate) {
                filteredTransactions.add(transaction)
            }
        }
        withContext(Dispatchers.Main) {
            filteredTransactions.sortBy {
                it.timestamp
            }
            transactionAdapter.transactions = filteredTransactions
            transactionAdapter.notifyDataSetChanged()
            hideShimmer()
        }
    }

    fun setMonthFilter(month: String) = lifecycleScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            showShimmer()
        }
        binding.tvMonthFilter.text = month
        val filteredTransactions = mutableListOf<TransactionHistory>()
        for (transaction in mTransactions) {
            if (transaction.month == month) {
                filteredTransactions.add(transaction)
            }
        }
        withContext(Dispatchers.Main) {
            filteredTransactions.sortBy {
                it.timestamp
            }
            transactionAdapter.transactions = filteredTransactions
            transactionAdapter.notifyDataSetChanged()
            hideShimmer()
        }
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.show()
            rvTransactionHistory.hide()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.gone()
            rvTransactionHistory.show()
        }
    }
}