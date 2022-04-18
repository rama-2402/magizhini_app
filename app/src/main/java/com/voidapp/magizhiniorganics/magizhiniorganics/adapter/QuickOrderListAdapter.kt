package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrderTextItem
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvQuickOrderTextBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvQuickorderListItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class QuickOrderListAdapter(
    private val context: Context,
    var quickOrderList: List<Uri>,
    var quickOrderListUrl: List<String>,
    var addImage: Boolean,
    private val onItemClickListener: QuickOrderClickListener
): RecyclerView.Adapter<QuickOrderListAdapter.QuickOrderViewHolder>() {

//    private val diff = object: DiffUtil.ItemCallback<Uri>() {
//        override fun areItemsTheSame(
//            oldItem: Uri,
//            newItem: Uri
//        ): Boolean {
//            return oldItem == newItem
//        }
//
//        override fun areContentsTheSame(
//            oldItem: Uri,
//            newItem: Uri
//        ): Boolean {
//            return oldItem == newItem
//        }
//    }
//
//    val calcDiff = AsyncListDiffer(this, diff)

    inner class QuickOrderViewHolder(val binding: RvQuickorderListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickOrderViewHolder {
        val view =
            RvQuickorderListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickOrderViewHolder, position: Int) {
        holder.binding.apply {
            if (addImage) {
                if (position == 0) {
                    ivDelete.visibility = View.GONE
                    view.visibility = View.GONE
                    tvListPageNumber.visibility = View.GONE
                    ivListPage.setImageDrawable(
                        ContextCompat.getDrawable(
                            ivListPage.context,
                            R.drawable.ic_add
                        )
                    )
                    ivListPage.imageTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray500))
                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.addImage()
                    }
                } else {
                    val uri = quickOrderList[position - 1]

                    if (uri == Uri.EMPTY) {
                        cvProductItem.visibility = View.INVISIBLE
                        return@apply
                    } else {
                        cvProductItem.visibility = View.VISIBLE
                    }

                    tvListPageNumber.text = "Page: ${position}"

                    //loading the product images
                    ivListPage.loadImg(uri.toString()) {}
//                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.selectedListImage(position - 1, uri, ivListPage)
                    }
                    ivDelete.setOnClickListener {
                        onItemClickListener.deleteListItem(position - 1, uri)
                    }
                }
            } else {
                val url = quickOrderListUrl[position]
                ivDelete.visibility = View.GONE
                tvListPageNumber.text = "Page: ${position + 1}"

                //loading the product images
                ivListPage.loadImg(url) {}
//                ivListPage.clipToOutline = true

                ivListPage.setOnClickListener {
                    onItemClickListener.selectedListImage(position, url, ivListPage)
                }
                ivDelete.setOnClickListener {
                    onItemClickListener.deleteListItem(position, url)
                }

            }
        }
    }

        override fun getItemCount(): Int {
//        return quickOrderList?.size ?: quickOrderListUrl?.size ?: quickOrderTextItems?.size ?: 1
            return if (quickOrderList.isNotEmpty()) {
                quickOrderList.size + 1
            } else {
                quickOrderListUrl.size
            }
        }
    }

interface QuickOrderClickListener {
    //quick order image item
    fun selectedListImage(position: Int, imageUri: Any, thumbnail: ShapeableImageView)
    fun deleteListItem(position: Int, imageUri: Any)
    fun addImage()

    //quick order text item
    fun addTextItem(productName: String, variantName: String, quantity: Int)
    fun removeTextItem(productName: String, variantName: String)
    fun updateTextItem(productName: String, variantName: String, quantity: Int)

    //quick order audio recorder
    fun startRecording()
    fun stopRecording()
    fun pauseRecording()
}

class QuickOrderTextAdapter(
    private val context: Context,
    var quickOrderTextItems: List<QuickOrderTextItem>,
    val onItemClickListener: QuickOrderClickListener
): RecyclerView.Adapter<QuickOrderTextAdapter.QuickOrderTextItemViewHolder>() {

    inner class QuickOrderTextItemViewHolder(val binding: RvQuickOrderTextBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setQuickOrderData(newList: List<QuickOrderTextItem>) {
        val diffUtil = QuickOrderTextItemDiffUtil(quickOrderTextItems, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        quickOrderTextItems = newList as ArrayList<QuickOrderTextItem>
        diffResult.dispatchUpdatesTo(this)
    }

    class QuickOrderTextItemDiffUtil(
        private val oldList: List<QuickOrderTextItem>,
        private val newList: List<QuickOrderTextItem>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].productName == newList[newItemPosition].productName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].productName != newList[newItemPosition].productName -> false
                oldList[oldItemPosition].variantName != newList[newItemPosition].variantName -> false
                oldList[oldItemPosition].quantity != newList[newItemPosition].quantity -> false
                else -> true
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickOrderTextItemViewHolder {
        val view =
            RvQuickOrderTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickOrderTextItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickOrderTextItemViewHolder, position: Int) {
        holder.binding.apply {
            if (quickOrderTextItems.isEmpty()) {
                etproductName.setText("")
                etVariantName.setText("")
                etQuantity.setText("1")
            } else {
                val textItem = quickOrderTextItems[position]
                etproductName.setText(textItem.productName)
                etVariantName.setText(textItem.variantName)
                etQuantity.setText(textItem.quantity)
                etproductName.isEnabled = false
                etVariantName.isEnabled = false
                etQuantity.isEnabled = false
            }
        }
    }

    override fun getItemCount(): Int {
        return if (quickOrderTextItems.isEmpty()) {
            1
        } else {
            quickOrderTextItems.size
        }
    }
}