package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvTransactionItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil

class WalletAdapter (
    val context: Context,
    var transactions: List<TransactionHistory>
        ) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    inner class WalletViewHolder(val binding: RvTransactionItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WalletAdapter.WalletViewHolder {
        val view = RvTransactionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalletAdapter.WalletViewHolder, position: Int) {
        val transaction  = transactions[position]
        with(holder.binding) {
            tvTransactionID.text = transaction.id
            tvTransactionAmount.text = transaction.amount.toString()
            tvDateStamp.text = TimeUtil().getCustomDate(dateLong = transaction.timestamp)
            tvTimestamp.text = TimeUtil().timeStamp(transaction.timestamp)
            tvOrderID.text = transaction.transactionFor
            checkTransactionType(holder, transaction)
            displayTransactionType(holder, transaction)
        }
    }

    private fun displayTransactionType(holder: WalletAdapter.WalletViewHolder, transaction: TransactionHistory) {
        with(holder.binding) {
            when(transaction.purpose) {
                Constants.ADD_MONEY -> tvOrderType.text = "Added to Wallet"
                Constants.SUBSCRIPTION -> tvOrderType.text = "Subscription"
                else -> tvOrderType.text = "Migizhini Purchase"
            }
        }
    }

    private fun displayOrderStatus(holder: WalletAdapter.WalletViewHolder, status: String, type: String) {
        with(holder.binding) {
            when (status) {
                Constants.SUCCESS -> {
                    if (type == Constants.ADD_MONEY) {
                        ivTransactionType.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_received
                            )
                        )
                    } else {
                        ivTransactionType.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_sent
                            )
                        )
                    }
                }
                Constants.FAILED -> ivTransactionType.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_error))
                Constants.PENDING -> ivTransactionType.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pending))
            }
        }
    }

    private fun checkTransactionType(holder: WalletAdapter.WalletViewHolder, transaction: TransactionHistory) {
        with(holder.binding) {
            when(transaction.purpose) {
                Constants.ADD_MONEY -> {
                    tvTransactionAmountText.text = "Received: "
                    displayOrderStatus(holder, transaction.status, transaction.purpose)
                }
                else -> {
                    tvTransactionAmountText.text = "Paid: "
                    displayOrderStatus(holder, transaction.status, transaction.purpose)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }
}