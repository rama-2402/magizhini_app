package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hsalf.smilerating.SmileRating
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlin.collections.ArrayList

class ReviewAdapter(
    val context: Context,
    var reviews: ArrayList<Review>,
    val viewModel: ProductViewModel
): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        val profileName: TextView = itemView.findViewById(R.id.tvProfileName)
        val timeStamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val ratings: SmileRating = itemView.findViewById(R.id.srRating)
        val reviewContent: TextView = itemView.findViewById(R.id.tvReview)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReviewViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_review_items, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        with(holder) {
            GlideLoader().loadUserPicture(context, review.userProfilePicUrl, profilePic)
            profileName.text = review.userName
            timeStamp.text = Time().getCustomDate(dateLong = review.timeStamp)
//            Timer("SettingUp", false).schedule(1000) {
//            ratings.setRating(review.rating, false)
//            ratings.disallowSelection(true)
//            ratings.setFaceColor(SmileyRating.Type.GREAT, Color.BLACK)
//            ratings.setFaceBackgroundColor(SmileyRating.Type.GREAT, Color.YELLOW)
//            }
//                ratings.setFaceColor(SmileyRating.Type.GREAT, Color.BLUE)

            ratings.setSelectedSmile(review.rating-1, false)

            reviewContent.text = getReviewContent(review.review)
        }
    }

    private fun getReviewContent(review: String): String {
        return if (review.isEmpty()) {
            ""
        } else {
            review
        }
    }

    override fun getItemCount(): Int {
        return reviews.size
    }
}