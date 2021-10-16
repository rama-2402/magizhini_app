package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref

class AddressAdapter(
    val context: Context,
    var checkedAddressPosition: Int,
    var addressList: ArrayList<Address>,
    val viewModel: CheckoutViewModel
): RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    inner class AddressViewHolder(val itemView: View): RecyclerView.ViewHolder(itemView) {
        val card: ConstraintLayout = itemView.findViewById(R.id.clAddress)
        val userName: TextView = itemView.findViewById(R.id.tvUserName)
        val addressOne: TextView = itemView.findViewById(R.id.tvAddressOne)
        val addressTwo: TextView = itemView.findViewById(R.id.tvAddressTwo)
        val area: TextView = itemView.findViewById(R.id.tvAddressArea)
        val add: RelativeLayout = itemView.findViewById(R.id.rlAddAddress)
        val delete: RelativeLayout = itemView.findViewById(R.id.rlDeleteAddress)
        val uncheck: ImageView = itemView.findViewById(R.id.ivUncheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_address_item, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {

            holder.card.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right_bounce))

            val address = addressList[position]
            val userID: String = SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()
            if (position == checkedAddressPosition) {
                holder.uncheck.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check))
            } else {
                holder.uncheck.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_uncheck))
            }

        with(holder) {
            if (addressList.size == 1) {
                delete.visibility = View.GONE
            } else {
                delete.visibility = View.VISIBLE
            }

            userName.text = address.userId
            addressOne.text = address.addressLineOne
            addressTwo.text = address.addressLineTwo
            area.text = address.LocationCode

            add.setOnClickListener {
                viewModel.addNewAddress(position + 1)
            }

            card.setOnClickListener {
                viewModel.editAddress(address, position)
            }

            delete.setOnClickListener {
                viewModel.deleteAddress(userID, position)
            }
        }

        holder.uncheck.setOnClickListener {
            holder.uncheck.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check))
            viewModel.selectedAddress(address, position)
        }
    }

    override fun getItemCount(): Int {
        return addressList.size
    }
}