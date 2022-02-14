package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import kotlin.math.max

//data class to entity class converter

fun UserProfile.toUserProfileEntity() = UserProfileEntity(
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

fun UserProfile.toCustomerProfile() = CustomerProfile(
    id = id,
    profileName = name,
    phoneNumber = phNumber,
    thumbnailName = id,
    thumbnailUrl = profilePicUrl
)

fun ProductCategory.toProductCategoryEntity() = ProductCategoryEntity(
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

fun Product.toProductEntity() = ProductEntity(
    id = id,
    name = name,
    category = category,
    thumbnailUrl = thumbnailUrl,
    thumbnailName = thumbnailName,
    rating = rating,
    description = description,
    descType = descType,
    status = status,
    discountAvailable = discountAvailable,
    defaultVariant = defaultVariant,
    productType = productType,
    variants = variants,
    labels = labels,
    activated = activated
)

fun UserNotification.toUserNotificationEntity() = UserNotificationEntity(
    id = id,
    userID = userID,
    timestamp = timestamp,
    title = title,
    message = message,
    imageUrl = imageUrl,
    clickType = clickType,
    clickContent = clickContent
)

fun Coupon.toCouponEntity() = CouponEntity(
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
    description = description
)

fun Testimonials.toTestimonialEntity() = TestimonialsEntity(
    id = id,
    title = title,
    message = message,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    order = order
)

fun Order.toOrderEntity() = OrderEntity(
    orderId = orderId,
    customerId = customerId,
    transactionID = transactionID,
    cart = cart,
    purchaseDate = purchaseDate,
    isPaymentDone = isPaymentDone,
    paymentMethod = paymentMethod,
    deliveryPreference = deliveryPreference,
    deliveryNote = deliveryNote,
    appliedCoupon = appliedCoupon,
    address = address,
    price = price,
    orderStatus = orderStatus,
    monthYear = monthYear,
    phoneNumber = phoneNumber
)

fun Cart.toCartEntity() = CartEntity(
    id = id,
    variant = variant,
    productId = productId,
    productName = productName,
    thumbnailUrl = thumbnailUrl,
    quantity = quantity,
    maxOrderQuantity = maxOrderQuantity,
    price = price,
    originalPrice = originalPrice
)

fun PinCodes.toPinCodesEntity() = PinCodesEntity(
    id = id,
    areaCode = areaCode,
    deliveryCharge = deliveryCharge
)

fun Subscription.toSubscriptionEntity() = SubscriptionEntity(
    id = id,
    productID = productID,
    productName = productName,
    variantName = variantName,
    phoneNumber = phoneNumber,
    customerID = customerID,
    address = address,
    monthYear = monthYear,
    startDate = startDate,
    endDate = endDate,
    basePay = basePay,
    paymentMode = paymentMode,
    estimateAmount = estimateAmount,
    subType = subType,
    status = status,
    customDates = customDates,
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

fun Banner.toSpecialBanners() = SpecialBanners(
    id = id,
    url = url,
    order = order,
    type = type,
    description = description
)

fun SpecialBanners.toBannerEntity() = BannerEntity(
    id = id,
    url = url,
    order = order,
    type = type,
    description = description
)
// Entity class to model data class converters

fun UserProfileEntity.toUserProfile() = UserProfile(
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

fun ProductCategoryEntity.toProductCategory() = ProductCategory(
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

fun ProductEntity.toProduct() = Product(
    id = id,
    name = name,
    category = category,
    thumbnailUrl = thumbnailUrl,
    thumbnailName = thumbnailName,
    rating = rating,
    description = description,
    descType = descType,
    status = status,
    discountAvailable = discountAvailable,
    defaultVariant = defaultVariant,
    productType = productType,
    variants = variants,
    labels = labels,
    activated = activated
)

fun CouponEntity.toCoupon() = Coupon(
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
    description = description
)

fun OrderEntity.toOrder() = Order(
    orderId = orderId,
    customerId = customerId,
    transactionID = transactionID,
    cart = cart,
    purchaseDate = purchaseDate,
    isPaymentDone = isPaymentDone,
    paymentMethod = paymentMethod,
    deliveryPreference = deliveryPreference,
    deliveryNote = deliveryNote,
    appliedCoupon = appliedCoupon,
    address = address,
    price = price,
    orderStatus = orderStatus,
    monthYear = monthYear,
    phoneNumber = phoneNumber
)


fun SubscriptionEntity.toSubscription() = Subscription(
    id = id,
    productID = productID,
    productName = productName,
    variantName = variantName,
    phoneNumber = phoneNumber,
    customerID = customerID,
    address = address,
    monthYear = monthYear,
    startDate = startDate,
    endDate = endDate,
    basePay = basePay,
    paymentMode = paymentMode,
    estimateAmount = estimateAmount,
    subType = subType,
    status = status,
    customDates = customDates,
    deliveredDates = deliveredDates,
    cancelledDates = cancelledDates,
    notDeliveredDates = notDeliveredDates
)
