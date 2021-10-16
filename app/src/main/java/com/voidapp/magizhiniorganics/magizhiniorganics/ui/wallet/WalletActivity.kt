package com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.PurchaseHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.WalletAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.WalletEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityWalletBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.DatePickerLib
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.*
import org.kodein.di.Constant
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class WalletActivity : BaseActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityWalletBinding
    private lateinit var viewModel: WalletViewModel
    private val factory: WalletViewModelFactory by instance()

    private lateinit var transactionAdapter: WalletAdapter
    private var mWallet: WalletEntity = WalletEntity()

    private val TAG: String = "qqqq"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wallet)
        viewModel = ViewModelProvider(this, factory).get(WalletViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)

        binding.cvWalletCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right_bounce))

        showShimmer()
        clickListeners()
        initLiveData()
        liveDataObservers()
        initRecyclerView()
    }

    private fun liveDataObservers() {
        viewModel.wallet.observe(this, {

            val transactSucc = TransactionHistory(
                id = "pitBuPB3iiUpoydc65FXl3dUmel1",
                timestamp = System.currentTimeMillis(),
                amount = 100f,
                status = Constants.SUCCESS,
                purpose = Constants.ADD_MONEY,
                transactionFor = "pitBuPB3iiUpoydc65FXl3dUmel1"
            )
            val transactFail = TransactionHistory(
                id = "pitBuPB3iiUpoydc65FXl3dUmel1",
                timestamp = System.currentTimeMillis(),
                amount = 100f,
                status = Constants.FAILED,
                purpose = Constants.PURCHASE,
                transactionFor = "pitBuPB3iiUpoydc65FXl3dUmel1"
            )
            val transactRecei = TransactionHistory(
                id = "pitBuPB3iiUpoydc65FXl3dUmel1",
                timestamp = System.currentTimeMillis(),
                amount = 100f,
                status = Constants.SUCCESS,
                purpose = Constants.SUBSCRIPTION,
                transactionFor = "pitBuPB3iiUpoydc65FXl3dUmel1"
            )
            val transactPending = TransactionHistory(
                id = "pitBuPB3iiUpoydc65FXl3dUmel1",
                timestamp = System.currentTimeMillis(),
                amount = 100f,
                status = Constants.PENDING,
                purpose = Constants.SUBSCRIPTION,
                transactionFor = "pitBuPB3iiUpoydc65FXl3dUmel1"
            )

            it.transactionHistory = listOf<TransactionHistory>(
                transactSucc,
                transactRecei,
                transactFail,
                transactPending
            )

            mWallet = it
            displayWalletDataToScreen()
            transactionAdapter.transactions = it.transactionHistory
            transactionAdapter.notifyDataSetChanged()
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
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
        viewModel.getWallet()
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
            Intent(this, CheckoutActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }

        binding.ivWalletFilter.setOnClickListener {
            DatePickerLib().pickSingleDate(this)
        }

        binding.ivReminder.setOnClickListener {
            binding.ivReminder.setImageResource(R.drawable.ic_notify_on)
            binding.ivReminder.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
        }

        binding.tvMonthFilter.setOnClickListener {
            showListBottomSheet(
                this,
                resources.getStringArray(R.array.months_name).toList() as ArrayList<String>
            )
        }

        binding.ivInfo.setOnClickListener {
            //TODO: Show some info about wallet and it's functions
        }
    }

    fun filterDate(date: Long) = lifecycleScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            showShimmer()
        }
        val filteredTransactions = mutableListOf<TransactionHistory>()
        for (transaction in mWallet.transactionHistory) {
            val transactionDate = Time().getCustomDate(dateLong = transaction.timestamp)
            val filteredDate = Time().getCustomDate(dateLong = date)
            if (transactionDate == filteredDate) {
                filteredTransactions.add(transaction)
            }
        }
        withContext(Dispatchers.Main) {
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
        for (transaction in mWallet.transactionHistory) {
            if (transaction.month == month) {
                filteredTransactions.add(transaction)
            }
        }
        withContext(Dispatchers.Main) {
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