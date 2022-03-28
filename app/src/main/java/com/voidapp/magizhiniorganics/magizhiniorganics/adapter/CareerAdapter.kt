package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Career
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvCareerItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class CareerAdapter(
    private val careers: List<Career>,
    private val onItemClickListener: CareerItemClickListener
): RecyclerView.Adapter<CareerAdapter.CareerViewHolder>() {

    inner class CareerViewHolder(val binding: RvCareerItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareerViewHolder {
        val view = RvCareerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CareerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CareerViewHolder, position: Int) {
        val career = careers[position]
        holder.binding.apply {
            if (career.thumbnail != "") {
                ivThumbnail.loadImg(career.thumbnail) {}
            }
            tvName.text = career.name
            tvQualification.text = career.qualification
            tvVacancy.text = career.vacancy.toString()

            clBody.setOnClickListener {
                onItemClickListener.selectedCareer(career)
            }
        }
    }

    override fun getItemCount(): Int {
        return careers.size
    }

    interface CareerItemClickListener {
    fun selectedCareer(career: Career)
}
}