package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.voidapp.magizhiniorganics.magizhiniorganics.R

class ViewPagerAdapter(
    val title: List<String>,
    val bodyOne: List<String>,
    val viewPagerListener: ViewPagerListener
): RecyclerView.Adapter<ViewPagerAdapter.Pager2ViewHolder>() {

    inner class Pager2ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val bodyOne = itemView.findViewById<TextView>(R.id.tvBodyOne)
        val btnSignIn = itemView.findViewById<MaterialButton>(R.id.btnSignIn)
        val nextLottie = itemView.findViewById<LottieAnimationView>(R.id.next_screen)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewPagerAdapter.Pager2ViewHolder {
        return Pager2ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.onboarding_viewpager, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewPagerAdapter.Pager2ViewHolder, position: Int) {
        holder.title.text = title[position]
        holder.bodyOne.text = bodyOne[position]

        if (position == title.size-1) {
            holder.nextLottie.visibility = View.GONE
            holder.btnSignIn.visibility = View.VISIBLE
        } else {
            holder.nextLottie.visibility = View.VISIBLE
            holder.btnSignIn.visibility = View.GONE
        }

        holder.btnSignIn.setOnClickListener {
            viewPagerListener.signIn()
        }
    }

    override fun getItemCount(): Int {
        return title.size
    }
}