package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg


class TestimonialsAdapter(
    var testimonials: MutableList<TestimonialsEntity>,
    private val onItemClickListener: TestimonialItemClickListener
) : RecyclerView.Adapter<TestimonialsAdapter.TestimonialsViewHolder>() {

    private var displaySideCounter = 2

    companion object {
        private const val ODD_ORDER: Int = 1
        private const val EVEN_ORDER: Int = 2
    }

    inner class TestimonialsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.tvMessage)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val thumbnail: ShapeableImageView = itemView.findViewById(R.id.ivThumbnail)
        val playBtn: ShapeableImageView = itemView.findViewById(R.id.ivPlayBtn)
        val body: ConstraintLayout = itemView.findViewById(R.id.cvBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestimonialsViewHolder {
        return if (viewType == EVEN_ORDER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_testimonial_left_item, parent, false)
            TestimonialsViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_testimonial_right_item, parent, false)
            TestimonialsViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: TestimonialsViewHolder, position: Int) {
        val testimonial = testimonials[position]
        holder.apply {
            thumbnail.loadImg(testimonial.thumbnailUrl) {}
            message.text = testimonial.message
            name.text = "- ${testimonial.title}"

            if (testimonial.videoUrl.isNullOrEmpty()) {
                playBtn.visibility = View.GONE
            } else {
                thumbnail.setOnClickListener {
                    onItemClickListener.openVideo(testimonial.videoUrl, thumbnail)
                }
                playBtn.setOnClickListener {
                    onItemClickListener.openVideo(testimonial.videoUrl, thumbnail)
                }
                body.setOnClickListener {
                    onItemClickListener.openVideo(testimonial.videoUrl,thumbnail)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return testimonials.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (displaySideCounter % 2 == 0) {
            displaySideCounter += 1
            EVEN_ORDER
        } else {
            displaySideCounter += 1
            ODD_ORDER
        }
    }

    fun setTestimonialData(newList: List<TestimonialsEntity>) {
        val diffUtil = TestimonialsDiffUtil(testimonials, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        testimonials = newList as ArrayList<TestimonialsEntity>
        diffResult.dispatchUpdatesTo(this)
    }

    class TestimonialsDiffUtil(
        private val oldList: List<TestimonialsEntity>,
        private val newList: List<TestimonialsEntity>
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

    interface TestimonialItemClickListener {
        fun openVideo(url: String, thumbnail: ShapeableImageView)
    }
}