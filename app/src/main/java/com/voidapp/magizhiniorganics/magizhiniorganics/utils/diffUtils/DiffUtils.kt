package com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils

import androidx.recyclerview.widget.DiffUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import kotlin.collections.listOf as listOf

class DiffUtils(
    private val oldList: List<ProductEntity>,
    private val newList: List<ProductEntity>
): DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        oldList
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when {
            oldList[oldItemPosition].id !== newList[newItemPosition].id -> {
                false
            }
            oldList[oldItemPosition].name !== newList[newItemPosition].name -> {
                false
            }
            oldList[oldItemPosition].category !== newList[newItemPosition].category -> {
                false
            }
            oldList[oldItemPosition].thumbnailUrl !== newList[newItemPosition].thumbnailUrl -> {
                false
            }
            oldList[oldItemPosition].thumbnailName !== newList[newItemPosition].thumbnailName -> {
                false
            }
            oldList[oldItemPosition].rating !== newList[newItemPosition].rating -> {
                false
            }
            oldList[oldItemPosition].description !== newList[newItemPosition].description -> {
                false
            }
            oldList[oldItemPosition].descType !== newList[newItemPosition].descType -> {
                false
            }
            oldList[oldItemPosition].status !== newList[newItemPosition].status -> {
                false
            }
            oldList[oldItemPosition].discountAvailable !== newList[newItemPosition].discountAvailable -> {
                false
            }
            oldList[oldItemPosition].defaultVariant !== newList[newItemPosition].defaultVariant -> {
                false
            }
            oldList[oldItemPosition].variants !== newList[newItemPosition].variants -> {
                false
            }
            oldList[oldItemPosition].activated !== newList[newItemPosition].activated -> {
                false
            }
            else -> true
        }
    }
}