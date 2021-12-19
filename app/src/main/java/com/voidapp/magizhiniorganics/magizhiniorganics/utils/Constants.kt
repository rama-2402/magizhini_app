package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.text.BoringLayout
import android.view.View

object Constants {

    //appName
    const val APP_NAME: String = "MagizhiniOrganics"

    //database
    const val DATABASE: String = "UserDatabase.db"

    //permission request codes
    const val PICK_IMAGE_REQUEST_CODE: Int = 1
    const val READ_STORAGE_PERMISSION_CODE = 2
    const val STORAGE_PERMISSION_CODE = 3
    const val ACCESS_LOCATION = 4

    //toast type
    const val LONG: String = "L"
    const val SHORT: String = "S"

    //shared preference constants
    const val STRING: String = "string"
    const val BOOLEAN: String = "boolean"
    const val INT: String = "int"
    const val DATE: String = "01/01/2020"
    const val CURRENT_COUPON: String = "currentCoupon"

    //intents
    const val MAIN_PAGE_STATE: String = "mainPageState"
    const val FILTER: String = "filter"
    const val CUSTOMER_SUPPORT: String = "customerSupport"

    //Loggin status
    const val LOGIN_STATUS: String = "loginStatus"

    //firestore fields
    const val REVIEWS: String = "reviews"

    //message type
    const val TEXT: String = "text"
    const val IMAGE: String = "image"

    //time constants
    const val SECOND_MILLIS: Int = 1000
    const val MINUTE_MILLIS: Int = 60 * SECOND_MILLIS
    const val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS
    const val DAY_MILLIS: Int = 24 * HOUR_MILLIS

    //collection data
    const val ADMIN: String = "Admin"
    const val BANNER: String = "banner"
    const val BANNER_INFO: String = "banner-info"
    const val CATEGORY: String = "category"
    const val COUPON: String = "coupon"
    const val DISCOUNT: String = "discount"
    const val INVENTORY: String = "inventory"
    const val ORDER_HISTORY: String = "orderHistory"
    const val ADDRESS: String = "address"
    const val SPECIAL_BANNER = "specialBanner"
    const val PRODUCT_SPECIALS = "ProductSpecials"
    const val BEST_SELLERS = "BestSellers"
    const val SPECIALS_ONE = "one"
    const val SPECIALS_TWO = "two"
    const val SPECIALS_THREE = "three"
    const val USER_NOTIFICATIONS = "UserNotifications"
    const val TRASH = "Trash"
    const val TOKENS = "Tokens"
    const val WALLET = "Wallet"
    const val REFERRAL = "Referral"
    const val TESTIMONIALS = "Testimonials"

    //Navigation
    const val NAVIGATION: String = "navigate"
    const val HOME_PAGE: String = "home"
    const val SHOPPING_MAIN_PAGE: String = "shopping"
    const val DISCOUNT_FILTER_PAGE: String = "discountFilter"
    const val SUB_FILTER_PAGE: String = "subFilter"
    const val FAVORITES_FILTER_PAGE: String = "favoritesFilter"
    const val LIMITED_FILTER_PAGE: String = "LimitedFilter"
    const val PRODUCT_PAGE: String = "productPage"
    const val SUBSCRIPTION_PAGE: String = "subPage"
    const val CHECKOUT_PAGE: String = "checkoutPage"
    const val SUB_PRODUCT_PAGE: String = "subProductPage"
    const val PROFILE_PAGE: String = "profilePage"
    const val SUB_HISTORY_PAGE: String = "subHistoryPage"
    const val ORDER_HISTORY_PAGE: String = "orderHistoryPage"
    const val WALLET_PAGE: String = "walletPage"
    const val CUSTOMER_SUPPORT_PAGE: String = "customerSupport"

    //filters data
    const val ALL: String = "all"
    const val ALL_PRODUCTS: String = "All Products"
    const val CATEGORY_LIST: String = "categoriesList"

    //banner class
    const val BANNER_ORDER = "order"
    //banner click response types
    const val DO_NOTHING: String = "Do Nothing"
    const val SHOW_DETAILS: String = "Show details"
    const val OPEN_LINK: String = "url"
    const val DESCRIPTION = "description"
    const val NONE = "none"

    //image storage paths
    const val PROFILE_PIC_PATH: String = "users/profile-pic/"
    const val REVIEW_IMAGE_PATH: String = "reviews/"
    const val CHAT_CONVERSATION: String = "customerSupport/support/"


    //profile activity
    const val USERS: String = "users"
    const val USER_ID: String = "id"
    const val PROFILE_NAME: String = "name"
    const val PHONE_NUMBER: String = "phNumber"
    const val ALTERNATE_PH_NUMBER: String = "alternatePhNumber"
    const val DOB: String = "dob"
    const val MAIL_ID: String = "mailId"
    const val ADDRESS_LINE_ONE: String = "addressLineOne"
    const val ADDRESS_LINE_TWO: String = "addressLineTwo"
    const val LOCATION_CODE: String = "LocationCode"
    const val LOCATION_CODE_POSITION: String = "LocationCodePosition"
    const val CITY: String = "city"
    const val GPS_LATITUDE: String = "gpsLatitude"
    const val GPS_LONGITUDE: String = "gpsLongitude"
    const val GPS_ADDRESS: String = "gpsAddress"
    const val PROFILE_PIC_URI: String = "ProfilePicUrl"
    const val DEFAULT_PRODUCT_VARIANT: String = "defaultProductVariant"
    const val REFERRER_NUMBER: String = "referrerNumber"
    const val FAVORITES: String = "favorites"

    //product status
    const val PRODUCTS: String = "Products"
    const val PRODUCT_NAME: String = "productName"
    const val AVAILABLE: String = "Available"
    const val OUT_OF_STOCK: String = "Out of Stock"
    const val LIMITED: String = "Limited"
    const val NO_LIMIT: String = "No Limit"
    const val DISCOUNT_PERCENTAGE: String = "Percentage"
    const val DISCOUNT_RUPEES: String = "Rupees"

    //Coupons
    const val DATE_CODE: String = "0000/00/00"
    const val ACTIVE: String = "active"
    const val INACTIVE: String = "inactive"
    const val EXPIRED: String = "expired"
    const val STATUS: String = "status"

    //wallets
    const val SUCCESS: String = "Success"
    const val FAILED: String = "Failed"
    const val CANCELLED: String = "Cancelled"
    const val PENDING: String = "Pending"
    const val PURCHASE: String = "Purchase"
    const val SUBSCRIPTION: String = "Subscription"
    const val SUB: String = "Sub"
    const val UNSUB: String = "Unsub"
    const val ADD_MONEY: String = "Add Money"

    const val DELIVERY_CHARGE = "Delivery Charge"

    //subscriptions
    const val SINGLE_PURCHASE = "Single Purchase"
    const val MONTHLY = "Monthly"
    const val ALTERNATE_DAYS = "Alternate Days"
    const val CUSTOM_DAYS = "Custom Days"
    const val SUB_ACTIVE = "Active"
    const val SUB_CANCELLED = "Cancelled"

    //weekdays
    const val MONDAY: String = "Monday"
    const val TUESDAY: String = "Tuesday"
    const val WEDNESDAY: String = "Wednesday"
    const val THURSDAY: String = "Thursday"
    const val FRIDAY: String = "Friday"
    const val SATURDAY: String = "Saturday"
    const val SUNDAY: String = "Sunday"

    //FCM
    const val BASE_URL = "https://fcm.googleapis.com"
    const val SERVER_KEY = "AAAAicvCZoA:APA91bGUQOWLoRac1IR6wlE3deLM-d4T8PBl9JRiJe9AesvbaN4WOQJjwgcLf8yQ6M5I-uDGKLF_PDA7E0R-qQfB14P1I97NHMCGOhHxB7p4HZpoYVtbqtnLl6ocUR6Y1gWX1iAMKvKl"
    const val CONTENT_TYPE = "application/json"

    //notification
    const val BROADCAST: String = "/topics/magizhiniBroadcast"
    const val ONLINE_STATUS: String = "online"
}