package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish

import androidx.lifecycle.ViewModel
import androidx.work.Operation
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DishViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var dish = CWMFood()
    val cartItems = mutableListOf<CartEntity>()
    var positionToUpdate = 0

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun updateCartItem(position: Int, count: Int) {
        positionToUpdate = position
        cartItems[position].quantity = count
        updateDishPrice(position, count)
        _status.value = NetworkResult.Success("update", cartItems)
    }

    private fun updateDishPrice(position: Int, count: Int) {
        dish.totalPrice -= (dish.ingredients[position].price * dish.ingredients[position].quantity)
        dish.ingredients[position].quantity = count
        dish.totalPrice += (dish.ingredients[position].price * dish.ingredients[position].quantity)
    }

    fun deleteItemFromCart(position: Int) {
        positionToUpdate = position
        cartItems.removeAt(position)
        dish.totalPrice -= (dish.ingredients[position].price * dish.ingredients[position].quantity)
        dish.ingredients.removeAt(position)
        _status.value = NetworkResult.Success("update", cartItems)
    }

}