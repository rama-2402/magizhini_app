package com.voidapp.magizhiniorganics.magizhiniorganics.ui.howTo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.HowToVideo
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHowToBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HowToActivity : BaseActivity() {

    private lateinit var binding: ActivityHowToBinding

    private var docs: QuerySnapshot? = null

    private val urlMap: HashMap<String, String> = hashMapOf()
    private val tbMap: HashMap<String, String> = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_how_to)

        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.apply {
            clInstall.setOnClickListener {
//                getHowToVideo("Install")
                openInBrowser(urlMap["Install"].toString())
            }
            clShopping.setOnClickListener {
//                getHowToVideo("Main")
                openInBrowser(urlMap["Main"].toString())
            }
            clProduct.setOnClickListener {
//                getHowToVideo("Product")
                openInBrowser(urlMap["Product"].toString())
            }
            clCheckout.setOnClickListener {
//                getHowToVideo("CheckOut")
                openInBrowser(urlMap["CheckOut"].toString())
            }
            clWallet.setOnClickListener {
//                getHowToVideo("Wallet")
                openInBrowser(urlMap["Wallet"].toString())
            }
            clquickorder.setOnClickListener {
//                getHowToVideo("QuickOrder")
                openInBrowser(urlMap["QuickOrder"].toString())
            }
            clCWM.setOnClickListener {
                openInBrowser(urlMap["AllCwm"].toString())
//                getHowToVideo("AllCwm")
            }
            clSub.setOnClickListener {
                openInBrowser(urlMap["SubProduct"].toString())
//                getHowToVideo("SubProduct")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        showProgressDialog(true)
        getDocs()
    }

    private fun getDocs() = lifecycleScope.launch(Dispatchers.IO) {
        docs = FirebaseFirestore.getInstance().collection("HowTo").get().await()
        docs?.let { querySnapshot ->
            for (doc in querySnapshot.documents) {
                if (doc.id.contains("tb")) {
                    tbMap[doc.id] = doc.toObject(HowToVideo::class.java)!!.url
                } else {
                    urlMap[doc.id] = doc.toObject(HowToVideo::class.java)!!.url
                }
            }
            withContext(Dispatchers.Main) {
                populateThumbnails()
                hideProgressDialog()
            }
        }
    }

    private fun populateThumbnails() {
        for (item in tbMap.entries) {
            when (item.key) {
                "tbInstall" -> {
                    binding.ivInstallThumbnail.loadImg(item.value) {}
                    binding.tvInstallName.setTextAnimation("How To Create Account?")
                }
                "tbMain" -> {
                    binding.ivShoppingThumbnail.loadImg(item.value) {}
                    binding.tvShoppingName.setTextAnimation("Exploring Magizhini Organics")
                }
                "tbProduct" -> {
                    binding.ivProductThumbnail.loadImg(item.value) {}
                    binding.tvProductName.setTextAnimation("Product Details")
                }
                "tbCheckOut" -> {
                    binding.ivCheckoutThumbnail.loadImg(item.value) {}
                    binding.tvCheckoutName.setTextAnimation("How To Place Order?")
                }
                "tbQuickOrder" -> {
                    binding.ivquickorder.loadImg(item.value) {}
                    binding.tvquickorder.setTextAnimation("What is Magizhini Quick Order?")
                }
                "tbWallet" -> {
                    binding.ivWalletThumbnail.loadImg(item.value) {}
                    binding.tvWalletName.setTextAnimation("How To Use Magizhini Wallet?")
                }
                "tbAllCwm" -> {
                    binding.ivCWMThumbnail.loadImg(item.value) {}
                    binding.tvCWMName.setTextAnimation("What is Cook With Magizhini?")
                }
                "tbSubProduct" -> {
                    if (item.value.isNullOrEmpty()) {
                        binding.clSub.remove()
                    } else {
                        binding.ivSubThumbnail.loadImg(item.value) {}
                        binding.tvSubName.setTextAnimation("What is Magizhini Subscription?")
                    }
                }
                else -> {}
            }
        }
    }


//     = lifecycleScope.launch(Dispatchers.IO) {
//        try {
//            val url = FirebaseFirestore.getInstance()
//                .collection("HowTo")
//                .document(where)
//                .get().await().toObject(HowToVideo::class.java)?.url
//
//            withContext(Dispatchers.Main) {
//                if (url.isNullOrEmpty()) {
//                    showToast(
//                        this@HowToActivity,
//                        "demo video will be available soon. sorry for the inconvenience."
//                    )
//                } else {
//                    openInBrowser(url)
//                }
//            }
//        } catch (e: Exception) {
//            showToast(
//                this@HowToActivity,
//                "demo video will be available soon. sorry for the inconvenience."
//            )
//        }
//    }

//    fun getHowToVideo(where: String) = lifecycleScope.launch(Dispatchers.IO) {
//        try {
//            val url = FirebaseFirestore.getInstance()
//                .collection("HowTo")
//                .document(where)
//                .get().await().toObject(HowToVideo::class.java)?.url
//
//            withContext(Dispatchers.Main) {
//                if (url.isNullOrEmpty()) {
//                    showToast(
//                        this@HowToActivity,
//                        "demo video will be available soon. sorry for the inconvenience."
//                    )
//                } else {
//                    openInBrowser(url)
//                }
//            }
//        } catch (e: Exception) {
//            showToast(
//                this@HowToActivity,
//                "demo video will be available soon. sorry for the inconvenience."
//            )
//        }
//    }

    private fun openInBrowser(url: String) {
        if (url.isEmpty()) {
            showToast(
                this@HowToActivity,
                "demo video will be available soon. sorry for the inconvenience."
            )
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(url)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

}