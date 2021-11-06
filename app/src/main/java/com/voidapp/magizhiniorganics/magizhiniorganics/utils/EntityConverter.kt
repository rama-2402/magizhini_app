package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*

    //data class to entity class converter

    fun UserProfile.toUserProfileEntity() = UserProfileEntity (
        id = id,
        name = name,
        phNumber = phNumber,
        alternatePhNumber = alternatePhNumber,
        dob = dob,
        mailId = mailId,
        address = address,
        profilePicUrl = profilePicUrl,
        referralId = referrerNumber,
        defaultProductVariant = defaultProductVariant,
        favorites = favorites,
        purchaseHistory = purchaseHistory,
        purchasedMonths = purchasedMonths,
        subscribedMonths = subscribedMonths,
        subscriptions = subscriptions,
        member = member,
        membershipType = membershipType
    )

    fun ProductCategory.toProductCategoryEntity() = ProductCategoryEntity (
        id = id,
        name = name,
        items = items,
        thumbnailUrl = thumbnailUrl,
        thumbnailName = thumbnailName,
        isDiscounted = isDiscounted,
        discountType = discountType,
        discountAmount = discountAmount,
        products = products,
        activated = activated
    )

    fun Product.toProductEntity() = ProductEntity (
        id = id,
        name = name,
        category = category,
        thumbnailUrl = thumbnailUrl,
        thumbnailName = thumbnailName,
        rating = rating,
        description =  description,
        descType = descType,
        status = status,
        discountAvailable =  discountAvailable,
        defaultVariant = defaultVariant,
        productType = productType,
        variants = variants,
        activated = activated,
        reviews = reviews
    )

    fun Coupon.toCouponEntity() = CouponEntity (
        id = id,
        name = name,
        code = code,
        description = description,
        status = status,
        type = type,
        amount = amount,
        purchaseLimit = purchaseLimit,
        maxDiscount = maxDiscount,
        from = from,
        expiryDate = expiryDate,
        categories = categories
    )

    fun Banner.toBannerEntity() = BannerEntity(
        id = id,
        url = url,
        order = order,
        type = type,
        description = description,
        imageId = imageId
    )

    fun Order.toOrderEntity() = OrderEntity(
        orderId = orderId,
        customerId = customerId,
        transactionID = transactionID,
        cart = cart,
        purchaseDate = purchaseDate,
        isPaymentDone = isPaymentDone,
        paymentMethod = paymentMethod,
        deliveryPreference =  deliveryPreference,
        deliveryNote = deliveryNote,
        appliedCoupon = appliedCoupon,
        address = address,
        price = price,
        orderStatus = orderStatus,
        monthYear = monthYear,
        phoneNumber = phoneNumber
    )

    fun PinCodes.toPinCodesEntity() = PinCodesEntity (
        id = id,
        areaCode = areaCode,
        deliveryCharge = deliveryCharge
    )

fun Subscription.toSubscriptionEntity() = SubscriptionEntity (
    id = id,
    productID = productID,
    productName = productName,
    customerID = customerID,
    address = address,
    monthYear = monthYear,
    startDate = startDate,
    endDate = endDate,
    autoPay = autoPay,
    paymentMode = paymentMode,
    estimateAmount = estimateAmount,
    subType = subType,
    status = status,
    deliveredDates = deliveredDates,
    cancelledDates = cancelledDates,
    notDeliveredDates = notDeliveredDates
        )

    fun ProductSpecials.toBestSellers() = BestSellers(
        name = name,
        id = id
    )
    fun ProductSpecials.toSpecialsOne() = SpecialsOne(
        name = name,
        id = id
    )
    fun ProductSpecials.toSpecialsTwo() = SpecialsTwo(
        name = name,
        id = id
    )
    fun ProductSpecials.toSpecialsThree() = SpecialsThree(
        name = name,
        id = id
    )
    fun SpecialBannersData.toSpecialBanners() = SpecialBanners(
        id = id,
        url = url
    )

    // Entity class to model data class converters

    fun UserProfileEntity.toUserProfile() = UserProfile (
        id = id,
        name = name,
        phNumber = phNumber,
        alternatePhNumber = alternatePhNumber,
        dob = dob,
        mailId = mailId,
        address = address,
        profilePicUrl = profilePicUrl,
        referrerNumber = referralId,
        defaultProductVariant = defaultProductVariant,
        favorites = favorites,
        purchaseHistory = purchaseHistory,
        purchasedMonths = purchasedMonths,
        subscribedMonths = subscribedMonths,
        subscriptions = subscriptions,
        member = member,
        membershipType = membershipType
    )

    fun ProductCategoryEntity.toProductCategory() = ProductCategory (
        id = id,
        name = name,
        items = items,
        thumbnailUrl = thumbnailUrl,
        thumbnailName = thumbnailName,
        isDiscounted = isDiscounted,
        discountType = discountType,
        discountAmount = discountAmount,
        products = products,
        activated = activated
    )

    fun ProductEntity.toProduct() = Product (
        id = id,
        name = name,
        category = category,
        thumbnailUrl = thumbnailUrl,
        thumbnailName = thumbnailName,
        rating = rating,
        description =  description,
        descType = descType,
        status = status,
        discountAvailable =  discountAvailable,
        defaultVariant = defaultVariant,
        productType = productType,
        variants = variants,
        activated = activated,
        reviews = reviews
    )

    fun CouponEntity.toCoupon() = Coupon (
        id = id,
        name = name,
        code = code,
        description = description,
        status = status,
        type = type,
        amount = amount,
        purchaseLimit = purchaseLimit,
        maxDiscount = maxDiscount,
        from = from,
        expiryDate = expiryDate,
        categories = categories
    )

    fun BannerEntity.toBanner() = Banner(
        id = id,
        url = url,
        order = order,
        type = type,
        description = description,
        imageId = imageId
    )

    fun OrderEntity.toOrder() = Order(
        orderId = orderId,
        customerId = customerId,
        transactionID = transactionID,
        cart = cart,
        purchaseDate = purchaseDate,
        isPaymentDone = isPaymentDone,
        paymentMethod = paymentMethod,
        deliveryPreference =  deliveryPreference,
        deliveryNote = deliveryNote,
        appliedCoupon = appliedCoupon,
        address = address,
        price = price,
        orderStatus = orderStatus,
        monthYear = monthYear,
        phoneNumber = phoneNumber
    )


fun SubscriptionEntity.toSubscription() = Subscription (
    id = id,
    productID = productID,
    productName = productName,
    customerID = customerID,
    address = address,
    monthYear = monthYear,
    startDate = startDate,
    endDate = endDate,
    autoPay = autoPay,
    paymentMode = paymentMode,
    estimateAmount = estimateAmount,
    subType = subType,
    status = status,
    deliveredDates = deliveredDates,
    cancelledDates = cancelledDates,
    notDeliveredDates = notDeliveredDates
)
