package com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet

import android.content.Intent
import android.content.res.ColorStateList
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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.DatePickerLib
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.*
import org.json.JSONObject
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
    private var mMoneyToAddInWallet: Long = 0L

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
            if (mWallet.reminder) {
                ivReminder.setImageDrawable(ContextCompat.getDrawable(this@WalletActivity, R.drawable.ic_notify_on))
                ivReminder.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@WalletActivity, R.color.green_base))
            } else {
                ivReminder.setImageDrawable(ContextCompat.getDrawable(this@WalletActivity, R.drawable.ic_notify_on))
                ivReminder.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@WalletActivity, R.color.green_base))
            }
            if (mWallet.nextRecharge == 0L) {
                tvExpectedDate.text = "-"
                ivReminder.setImageDrawable(ContextCompat.getDrawable(this@WalletActivity, R.drawable.ic_notify_off))
                ivReminder.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@WalletActivity, R.color.gray700))
            } else {
                tvExpectedDate.text = Time().getCustomDate(dateLong = mWallet.nextRecharge)
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

        binding.ivReminder.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            binding.ivReminder.setImageResource(R.drawable.ic_notify_on)
            binding.ivReminder.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
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
                    mMoneyToAddInWallet = view.etReferralNumber.text.toString().toLong()
                    dialogBsAddReferral.dismiss()
                    startPayment(mMoneyToAddInWallet)
                }
            }
            dialogBsAddReferral.show()
        }
    }

    private fun startPayment(amount: Long) {
        /*
        *  You need to pass current activity in order to let Razorpay create CheckoutActivity
        * */
        val co = Checkout()
        val payment = amount * 100

        val email = if (mProfile.mailId.isEmpty()) "magizhiniorganics2018@gmail.com" else mProfile.mailId

        try {
            val options = JSONObject()
            options.put("name","${mProfile.name}")
            options.put("description","Adding Money to Wallet for ${mProfile.id}")
            //You can omit the image option to fetch the image from dashboard
            options.put("image","https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/icon_sh_4.png?alt=media&token=71cf0e67-2f00-4a0f-8950-15459ee02137")
            options.put("theme.color", "#86C232");
            options.put("currency","INR");
//            options.put("order_id", "orderIDkjhasgdfkjahsdf");
            options.put("amount","$payment")//pass amount in currency subunits

//            val retryObj = JSONObject();
//            retryObj.put("enabled", true);
//            retryObj.put("max_count", 4);
//            options.put("retry", retryObj);

            val prefill = JSONObject()
            prefill.put("email","$email")  //this place should have customer name
            prefill.put("contact","${mProfile.phNumber}")     //this place should have customer phone number

            options.put("prefill",prefill)
            co.open(this,options)
        }catch (e: Exception){
            Toast.makeText(this,"Error in payment: "+ e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
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
                    Time().getMonth(),
                    Time().getYear().toLong(),
                    mMoneyToAddInWallet.toFloat(),
                    id,
                    orderID,
                    Constants.SUCCESS,
                    Constants.ADD_MONEY,
                    orderID
                ).also {
                    mTransactions.add(it)
                    mTransactions.sortBy {
                        it.timestamp
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
            val transactionDate = Time().getCustomDate(dateLong = transaction.timestamp)
            val filteredDate = Time().getCustomDate(dateLong = date)
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