package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Partners
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvPartnerItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class PartnersAdapter(
    val partners: List<Partners>,
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
            ivThumbnail.loadImg(partner.imageUrl) {}
            tvName.text = partner.partnerName

            clPartnerItem.setOnClickListener {
                onItemClickListener.selectedPartner(partner, ivThumbnail)
            }
        }
    }

    override fun getItemCount(): Int {
        return partners.size
    }

    interface PartnersItemClickListener {
        fun selectedPartner(partner: Partners, thumbnail: ShapeableImageView)
    }

}