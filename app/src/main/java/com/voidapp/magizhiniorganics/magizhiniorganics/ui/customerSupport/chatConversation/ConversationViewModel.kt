package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversationimport android.net.Uriimport android.util.Logimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntityimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messagesimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.NotificationDataimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.PushNotificationimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CHAT_CONVERSATIONimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResultimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.flow.MutableStateFlowimport kotlinx.coroutines.flow.StateFlowimport kotlinx.coroutines.launchimport kotlinx.coroutines.withContextimport java.sql.Timestampclass ConversationViewModel(    private val dbRepository: DatabaseRepository,    private val fsRepository: FirestoreRepository,    private val fbRepository: FirebaseRepository): ViewModel() {    var token: String = ""    var supportID: String = ""    var profile = UserProfileEntity()    var supportProfile: SupportProfile = SupportProfile()    private var _conversation: MutableLiveData<ArrayList<Messages>> = MutableLiveData()    val conversation: LiveData<ArrayList<Messages>> = _conversation    private var _liveSupportProfile: MutableLiveData<SupportProfile> = MutableLiveData()    val liveSupportProfile: LiveData<SupportProfile> = _liveSupportProfile    val messages: ArrayList<Messages> = arrayListOf()    private var _imageUrl: MutableLiveData<String> = MutableLiveData()    val imageUrl: LiveData<String> = _imageUrl    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(        NetworkResult.Empty)    val status: StateFlow<NetworkResult> = _status    fun setEmptyStatus() = NetworkResult.Empty    fun getProfileData() = viewModelScope.launch(Dispatchers.IO) {        profile = dbRepository.getProfileData()!!        getConversation()    }    private fun getConversation() = viewModelScope.launch(Dispatchers.IO) {        fbRepository.getConversation(this@ConversationViewModel, profile.id, supportID)    }    fun displayChatMessages(message: Messages?) {            if (message == null) {                _status.value = NetworkResult.Failed("hide", null)            } else {                messages.add(message)                Log.e("qw", "vm nonull: $messages", )                _conversation.value = messages            }    }    fun updateProfileStatus(status: Boolean, timestamp: Long = 0L) = viewModelScope.launch(Dispatchers.IO) {        fbRepository.updateProfileStatus(profile.id, status, timestamp)    }    fun supportStatusListener() = viewModelScope.launch(Dispatchers.IO) {        fbRepository.supportStatusListener(supportID, this@ConversationViewModel)    }    fun updateSupportStatus(profile: SupportProfile) {        _liveSupportProfile.value = profile    }    fun sendMessage(message: Messages) = viewModelScope.launch(Dispatchers.IO) {        Log.e("qw", "send: $message", )        fbRepository.sendMessage(message)    }    fun updateTypingStatus(status: Boolean) = viewModelScope.launch (Dispatchers.IO) {        fbRepository.updateTypingStatus(profile.id, status)    }    fun updateProfileWithPic(uri: Uri, extension: String) = viewModelScope.launch (Dispatchers.IO) {        _status.value = NetworkResult.Loading("")        val url = fsRepository.uploadImage(            "${CHAT_CONVERSATION}${profile.name}/${supportProfile.profileName}",            uri,            extension,            data = "review"        )        if (url == "failed") {            _status.value = NetworkResult.Failed(                "image",                null            )        } else {            _status.value = NetworkResult.Success(                "image",                url            )        }    }    fun getToken() =viewModelScope.launch {        token = fsRepository.getSupportToken(supportID)        if (token == "") {            _status.value = NetworkResult.Failed("token", "Failed to connect to server. Messages will not be sent")        }    }}