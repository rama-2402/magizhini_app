package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvDishItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader

class AllCWMAdapter(
    private val context: Context,
    var dishes: MutableList<CWMFood>,
    private val onItemClickListener: CWMClickListener
): RecyclerView.Adapter<AllCWMAdapter.CWMViewHolder>() {

    inner class CWMViewHolder(val binding: RvDishItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CWMViewHolder {
        val view = RvDishItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CWMViewHolder(view)
    }

    override fun onBindViewHolder(holder: CWMViewHolder, position: Int) {
        val dish = dishes[position]
        holder.binding.apply {
            GlideLoader().loadUserPicture(context, dish.thumbnailUrl, ivThumbnail)
            tvDishName.text = dish.dishName

            clDish.setOnClickListener {
                onItemClickListener.selectedItem(dish)
            }
        }
    }

    override fun getItemCount(): Int {
        return dishes.size
    }

    interface CWMClickListener {
        fun selectedItem(dish: CWMFood)
    }
}