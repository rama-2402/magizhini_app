package com.voidapp.magizhiniorganics.magizhiniorganics

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Partners
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvPartnerItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation

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
            GlideLoader().loadUserPicture(ivThumbnail.context, partner.imageUrl, ivThumbnail)
            tvName.text = partner.partnerName

            clPartnerItem.setOnClickListener {
                onItemClickListener.selectedPartner(partner)
            }
        }
    }

    override fun getItemCount(): Int {
        return partners.size
    }

    interface PartnersItemClickListener {
        fun selectedPartner(partner: Partners)
    }

}