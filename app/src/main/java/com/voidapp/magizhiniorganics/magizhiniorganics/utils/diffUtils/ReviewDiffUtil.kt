package com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils

import androidx.recyclerview.widget.DiffUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review

class ReviewDiffUtil(
    private val oldList: ArrayList<Review>,
    private val newList: ArrayList<Review>
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
            oldList[oldItemPosition].id == newList[newItemPosition].id -> {
                true
            }
            oldList[oldItemPosition].userName == newList[newItemPosition].userName -> {
                true
            }
            oldList[oldItemPosition].userProfilePicUrl == newList[newItemPosition].userProfilePicUrl -> {
                true
            }
            oldList[oldItemPosition].timeStamp == newList[newItemPosition].timeStamp -> {
                true
            }
            oldList[oldItemPosition].rating == newList[newItemPosition].rating -> {
                true
            }
            oldList[oldItemPosition].review == newList[newItemPosition].review  -> {
                true
            }
            oldList[oldItemPosition].reviewImageUrl == newList[newItemPosition].reviewImageUrl -> {
                true
            }
            else -> true
        }
    }
}