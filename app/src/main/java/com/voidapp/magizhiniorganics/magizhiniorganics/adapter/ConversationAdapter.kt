package com.voidapp.magizhiniorganics.magizhiniorganics.adapterimport android.view.LayoutInflaterimport android.view.Viewimport android.view.ViewGroupimport android.widget.TextViewimport androidx.recyclerview.widget.RecyclerViewimport com.google.android.material.imageview.ShapeableImageViewimport com.voidapp.magizhiniorganics.magizhiniorganics.Rimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messagesimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivityimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationViewModelimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.*class ConversationAdapter(    private val activity: ConversationActivity,    var messages: ArrayList<Messages>,    var currentUserID: String,    private val onItemClickListener: ConversationItemClickListener): RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {    companion object {        private const val CUSTOMER_ID: Int = 1        private const val SUPPORT_ID: Int = 2    }    inner class ConversationViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {        val message: TextView = itemView.findViewById(R.id.tvMessageContent)        val timeStamp: TextView = itemView.findViewById(R.id.tvChatTimestamp)        val imageMessage: ShapeableImageView = itemView.findViewById(R.id.ivImageMessage)    }    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {        return if (viewType == CUSTOMER_ID) {            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_right_chathead, parent, false)            ConversationViewHolder(view)        } else {            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_left_chathead, parent, false)            ConversationViewHolder(view)        }    }    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {        val chatItem = messages[position]        with(holder) {            if (chatItem.type == Constants.TEXT) {                imageMessage.visibility = View.GONE                message.text = chatItem.message            } else if (chatItem.type == Constants.IMAGE) {                imageMessage.visibility = View.VISIBLE                message.visibility = View.GONE                GlideLoader().loadUserPicture(imageMessage.context, chatItem.message, imageMessage)            }            message.setOnClickListener {                showTimeAgo(holder, chatItem.timeStamp)            }            imageMessage.setOnClickListener {                onItemClickListener.openImage(chatItem.message)            }        }    }    private fun showTimeAgo(holder: ConversationViewHolder, time: Long) {        with(holder) {            if (timeStamp.visibility == View.VISIBLE) {                timeStamp.visibility = View.GONE            } else {                timeStamp.visibility = View.VISIBLE                timeStamp.text = TimeUtil().getTimeAgo(time)            }        }    }    override fun getItemCount(): Int {        return messages.size    }    override fun getItemViewType(position: Int): Int {        return if (messages[position].fromId == currentUserID) {            CUSTOMER_ID        } else {            SUPPORT_ID        }    }    interface ConversationItemClickListener {        fun openImage(url: String)    }}