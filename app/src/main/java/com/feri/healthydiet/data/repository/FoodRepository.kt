package com.feri.healthydiet.data.repository

import com.feri.healthydiet.data.local.FoodItemDao
import com.feri.healthydiet.data.model.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FoodRepository(
    private val foodItemDao: FoodItemDao
) {
    fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getAllFoodItems()
    }

    suspend fun getFoodItemById(id: String): FoodItem? = withContext(Dispatchers.IO) {
        return@withContext foodItemDao.getFoodItemById(id)
    }

    suspend fun searchFoodItems(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        return@withContext foodItemDao.searchFoodItems(query)
    }

    suspend fun saveFoodItem(foodItem: FoodItem) = withContext(Dispatchers.IO) {
        foodItemDao.insert(foodItem)
    }

    suspend fun updateFoodItem(foodItem: FoodItem) = withContext(Dispatchers.IO) {
        foodItemDao.update(foodItem)
    }

    suspend fun deleteFoodItem(foodItem: FoodItem) = withContext(Dispatchers.IO) {
        foodItemDao.delete(foodItem)
    }
}