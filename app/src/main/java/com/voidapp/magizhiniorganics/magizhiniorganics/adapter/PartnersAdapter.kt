package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Partners
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvPartnerItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadSimple

class PartnersAdapter(
    var partners: List<Partners>,
    val onItemClickListener: PartnersItemClickListener
): RecyclerView.Adapter<PartnersAdapter.PartnersViewHolder>() {

    inner class PartnersViewHolder(val binding: RvPartnerItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartnersViewHolder {
        val view = RvPartnerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PartnersViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartnersViewHolder, position: Int) {
        val partner = partners[position]
        holder.binding.apply {
            ivThumbnail.loadSimple(partner.imageUrl)
            tvName.text = partner.partnerName

            clPartnerItem.setOnClickListener {
                onItemClickListener.selectedPartner(partner, ivThumbnail)
            }
        }
    }

    override fun getItemCount(): Int {
        return partners.size
    }


    fun setPartnersData(newList: List<Partners>) {
        val diffUtil = PartnersDiffUtil(partners, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        partners = newList as ArrayList<Partners>
        diffResult.dispatchUpdatesTo(this)
    }

    class PartnersDiffUtil(
        private val oldList: List<Partners>,
        private val newList: List<Partners>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].id != newList[newItemPosition].id -> false
                else -> true
            }
        }
    }

    interface PartnersItemClickListener {
        fun selectedPartner(partner: Partners, thumbnail: ShapeableImageView)
    }

}