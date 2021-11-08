package com.voidapp.magizhiniorganics.magizhiniorganics.Firestoreimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport com.google.firebase.auth.FirebaseAuthimport com.google.firebase.database.*import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CrashLogimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CustomerProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messagesimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatViewModelimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationViewModelimport kotlinx.coroutines.CoroutineScopeimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.launchimport kotlinx.coroutines.tasks.awaitimport kotlin.collections.HashMapimport android.os.Buildimport android.os.Build.VERSION_CODESimport android.util.Logclass Firebase (    private val repository: DatabaseRepository) {    private val firebase by lazy {        FirebaseDatabase.getInstance()    }    private val mFirebaseAuth = FirebaseAuth.getInstance()    // Checks if the current entered phone number is already present in DB before sending the OTP    fun getPhoneNumber(): String? =        mFirebaseAuth.currentUser!!.phoneNumber    fun getCurrentUserId(): String? =        mFirebaseAuth.currentUser?.uid    fun getAllSupportProfiles(chatViewModel: ChatViewModel) = CoroutineScope(Dispatchers.IO).launch {        val supportProfiles: ArrayList<SupportProfile> = arrayListOf()        val supportProfilesReference = firebase.getReference("customerSupport")        supportProfilesReference.addListenerForSingleValueEvent(object : ValueEventListener{            override fun onDataChange(snapshot: DataSnapshot) {                snapshot.children.forEach {                    val supportProfile = it.getValue(SupportProfile::class.java)!!                    supportProfiles.add(supportProfile)                }                chatViewModel.updateAllSupportProfiles(supportProfiles)            }            override fun onCancelled(error: DatabaseError) {}        })    }    fun getSupportProfileUpdates(conversationViewModel: ConversationViewModel ,id: String) {        val supportProfileReference = firebase.getReference("customerSupport/$id")        supportProfileReference.addValueEventListener(object : ValueEventListener{            override fun onDataChange(snapshot: DataSnapshot) {                val profile = snapshot.getValue(SupportProfile::class.java)                conversationViewModel.updateSupportProfileStatus(profile)            }            override fun onCancelled(error: DatabaseError) {}        })    }    suspend fun uploadUserProfile(profile: CustomerProfile): Boolean {        return try {            val firebaseReference =                firebase.getReference("customerProfiles").child(profile.uid)            firebaseReference.setValue(profile).await()            true        } catch (e: Exception) {            e.message?.let { logCrash("upload profile", it) }            false        }    }    private suspend fun logCrash(location: String, message: String) {        CrashLog(            getCurrentUserId()!!,            "${ Build.MANUFACTURER } ${ Build.MODEL } ${Build.VERSION.RELEASE} ${ VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name }",            System.currentTimeMillis(),            location,            message        ).let {            try {                firebase.getReference("crashLog")                    .child(getCurrentUserId()!!)                    .setValue(it).await()            } catch (e: Exception) {                Log.e("Magizhini", "logCrash: $it ", )            }        }    }    fun updateTypingStatus(id: String, status: Boolean) = CoroutineScope(Dispatchers.IO).launch {        val hashMap = mutableMapOf<String, Any>()        hashMap["typing"] = status        val firebaseReference =            firebase.getReference("customerProfiles").child(id)        firebaseReference.updateChildren(hashMap)    }    fun updateProfileStatus(id: String ,status: Boolean, timestamp: Long) {        val hashMap = mutableMapOf<String, Any>()        hashMap["online"] = status        hashMap["timestamp"] = timestamp        val firebaseReference =            firebase.getReference("customerProfiles").child(id)        firebaseReference.updateChildren(hashMap)    }    fun getConversation(conversationViewModel: ConversationViewModel, fromId: String, toId: String) {        val firebaseReference =            firebase.getReference("/messages/$fromId/$toId")        firebaseReference.addChildEventListener(object : ChildEventListener{            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {                val messages = snapshot.getValue(Messages::class.java)!!                conversationViewModel.displayChatMessages(messages)            }            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}            override fun onChildRemoved(snapshot: DataSnapshot) {}            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}            override fun onCancelled(error: DatabaseError) {}        })    }    fun sendMessage(message: Messages) = CoroutineScope(Dispatchers.IO).launch {        val messageSenderReference = firebase.getReference("/messages/${message.fromId}/${message.toId}").push()        val messageReceiverReference = firebase.getReference("/messages/${message.toId}/${message.fromId}").push()        val recentMessageSenderReference = firebase.getReference("/recentMessages/${message.fromId}/${message.toId}")        val recentMessageReceiverReference = firebase.getReference("/recentMessages/${message.toId}/${message.fromId}")        message.id = messageSenderReference.key.toString()        messageSenderReference.setValue(message)        messageReceiverReference.setValue(message)        recentMessageSenderReference.setValue(message)        recentMessageReceiverReference.setValue(message)    }    fun listenForRecentMessages(chatViewModel: ChatViewModel, id: String) {        val hashMap = HashMap<String, Messages>()        val recentMessageReference = firebase.getReference("/recentMessages/$id")        recentMessageReference.addChildEventListener(object : ChildEventListener{            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {                val chatMessage = snapshot.getValue(Messages::class.java)                chatMessage?.let { it -> hashMap[snapshot.key!!] = it }                chatViewModel.updateRecentMessages(hashMap)            }            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {                val chatMessage = snapshot.getValue(Messages::class.java)                chatMessage?.let { it -> hashMap[snapshot.key!!] = it }                chatViewModel.updateRecentMessages(hashMap)            }            override fun onChildRemoved(snapshot: DataSnapshot) {}            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}            override fun onCancelled(error: DatabaseError) {}        })    }}