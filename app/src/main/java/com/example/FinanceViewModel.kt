package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Category
import com.example.data.CategoryDao
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class FinanceSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val weeklyChartData: List<Pair<String, Double>> = emptyList(),
    val monthlyChartData: List<Pair<String, Double>> = emptyList()
)

class FinanceViewModel(
    private val repository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val expenseCategories = categoryDao.getCategoriesByType(true).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val incomeCategories = categoryDao.getCategoriesByType(false).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allTransactions = repository.allTransactions

    init {
        viewModelScope.launch {
            if (categoryDao.getCategoryCount() == 0) {
                val defaults = listOf(
                    Category(name = "Makanan & Minuman", isExpense = true),
                    Category(name = "Transportasi", isExpense = true),
                    Category(name = "Hiburan", isExpense = true),
                    Category(name = "Tagihan", isExpense = true),
                    Category(name = "Belanja", isExpense = true),
                    Category(name = "Kesehatan", isExpense = true),
                    Category(name = "Gaji", isExpense = false),
                    Category(name = "Bonus", isExpense = false),
                    Category(name = "Hadiah", isExpense = false),
                    Category(name = "Lainnya", isExpense = false)
                )
                categoryDao.insertCategories(defaults)
            }
        }
    }

    fun addCategory(name: String, isExpense: Boolean) {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = name, isExpense = isExpense))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }

    fun importTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            transactions.forEach {
                repository.insert(it)
            }
        }
    }

    val uiState: StateFlow<FinanceSummary> = repository.allTransactions
        .map { transactions ->
            val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val balance = totalIncome - totalExpense
            val recent = transactions.take(10)
            
            FinanceSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                recentTransactions = recent,
                weeklyChartData = calculateWeeklyData(transactions),
                monthlyChartData = calculateMonthlyData(transactions)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinanceSummary()
        )

    fun addTransaction(type: TransactionType, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            repository.insert(Transaction(
                type = type,
                amount = amount,
                category = category,
                note = note
            ))
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun calculateWeeklyData(transactions: List<Transaction>): List<Pair<String, Double>> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeek = cal.timeInMillis

        val data = mutableMapOf<Int, Double>() // Day of week to amount
        for (i in 1..7) data[i] = 0.0

        expenses.forEach {
            if (it.timestamp >= startOfWeek) {
                val tCal = Calendar.getInstance()
                tCal.timeInMillis = it.timestamp
                val dayOfWeek = tCal.get(Calendar.DAY_OF_WEEK)
                data[dayOfWeek] = data.getOrDefault(dayOfWeek, 0.0) + it.amount
            }
        }

        val days = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab") // Map standard 1 (Sun) to 7 (Sat)
        // Adjust for first day of week. Standard US is Sun=1, Mon=2...
        return (1..7).map {
            days[it - 1] to data.getOrDefault(it, 0.0)
        }
    }

    private fun calculateMonthlyData(transactions: List<Transaction>): List<Pair<String, Double>> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)
        val startOfMonth = cal.timeInMillis

        val data = mutableMapOf<Int, Double>() // Week of month to amount
        for (i in 1..5) data[i] = 0.0

        expenses.forEach {
            if (it.timestamp >= startOfMonth) {
                val tCal = Calendar.getInstance()
                tCal.timeInMillis = it.timestamp
                val weekOfMonth = tCal.get(Calendar.WEEK_OF_MONTH)
                data[weekOfMonth] = data.getOrDefault(weekOfMonth, 0.0) + it.amount
            }
        }

        return (1..5).map {
            "M$it" to data.getOrDefault(it, 0.0)
        }
    }
}

class FinanceViewModelFactory(
    private val repository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository, categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
