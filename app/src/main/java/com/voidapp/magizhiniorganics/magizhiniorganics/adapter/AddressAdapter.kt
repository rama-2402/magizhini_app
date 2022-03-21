package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address

class AddressAdapter(
    val context: Context,
    var checkedAddressPosition: Int,
    var addressList: ArrayList<Address>,
    private val onItemClickListener: OnAddressClickListener
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

            val address = addressList[position]
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
                add.startAnimation(AnimationUtils.loadAnimation(add.context, R.anim.bounce))
                onItemClickListener.addAddress(position)
            }

            card.setOnClickListener {
                onItemClickListener.updateAddress(position)
            }

            delete.setOnClickListener {
                delete.startAnimation(AnimationUtils.loadAnimation(delete.context, R.anim.bounce))
                onItemClickListener.deleteAddress(position)
            }
        }

        holder.uncheck.setOnClickListener {
            holder.uncheck.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check))
            holder.uncheck.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            onItemClickListener.selectedAddress(position)
        }
    }

    override fun getItemCount(): Int {
        return addressList.size
    }

    interface OnAddressClickListener {
        fun selectedAddress(position: Int)
        fun addAddress(position: Int)
        fun deleteAddress(position: Int)
        fun updateAddress(position: Int)
    }

    fun deleteAddress(position: Int) {
        var items: MutableList<Address>? = addressList.map { it.copy() } as MutableList<Address>
        items?.let{
            it.removeAt(position)
            setAddressData(it)
        }
        items = null
    }

    fun updateAddress(position: Int, newAddress: Address) {
        var items: MutableList<Address>? = addressList.map { it.copy() } as MutableList
        items?.let {
            it[position] = newAddress
            setAddressData(it)
        }
        items = null
    }

    fun addAddress(position: Int, newAddress: Address) {
        var items: MutableList<Address>? = addressList.map { it.copy() } as MutableList
        items?.let {
            it.add(newAddress)
            setAddressData(it)
        }
        items = null
    }

    fun setAddressData(newList: List<Address>) {
        val diffUtil = AddressDiffUtil(addressList, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        addressList = newList as ArrayList<Address>
        diffResult.dispatchUpdatesTo(this)
    }

    class AddressDiffUtil(
        private val oldList: List<Address>,
        private val newList: List<Address>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].gpsAddress == newList[newItemPosition].gpsAddress
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].userId != newList[newItemPosition].userId -> false
                oldList[oldItemPosition].addressLineOne != newList[newItemPosition].addressLineOne -> false
                oldList[oldItemPosition].addressLineTwo != newList[newItemPosition].addressLineTwo -> false
                oldList[oldItemPosition].LocationCode != newList[newItemPosition].LocationCode -> false
                oldList[oldItemPosition].LocationCodePosition != newList[newItemPosition].LocationCodePosition -> false
                oldList[oldItemPosition].city != newList[newItemPosition].city -> false
                oldList[oldItemPosition].gpsLatitude != newList[newItemPosition].gpsLatitude -> false
                oldList[oldItemPosition].gpsLongitude != newList[newItemPosition].gpsLongitude -> false
                oldList[oldItemPosition].gpsAddress != newList[newItemPosition].gpsAddress -> false
                else -> true
            }
        }
    }
}