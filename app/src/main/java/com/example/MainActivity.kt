package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CsvUtils
import com.example.data.TransactionRepository
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Manual DI
    val db = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "finance-database"
    ).fallbackToDestructiveMigration().build()
    val repository = TransactionRepository(db.transactionDao())
    val factory = FinanceViewModelFactory(repository, db.categoryDao())

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          FinanceApp(factory)
        }
      }
    }
  }
}

@Composable
fun FinanceApp(factory: FinanceViewModelFactory) {
  val navController = rememberNavController()
  val viewModel: FinanceViewModel = viewModel(factory = factory)
  val summary by viewModel.uiState.collectAsStateWithLifecycle()
  val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
  val context = LocalContext.current

  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
          CsvUtils.exportToCsv(context, uri, allTransactions)
      }
  }

  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
          val imported = CsvUtils.importFromCsv(context, uri)
          viewModel.importTransactions(imported)
      }
  }

  NavHost(navController = navController, startDestination = "home") {
    composable("home") {
      HomeScreen(
        summary = summary,
        onAddClick = { navController.navigate("add") },
        onDeleteTransaction = { id -> viewModel.deleteTransaction(id) },
        onExport = { exportLauncher.launch("finance_export.csv") },
        onImport = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*")) }
      )
    }
    composable("add") {
      val expenseCats by viewModel.expenseCategories.collectAsStateWithLifecycle()
      val incomeCats by viewModel.incomeCategories.collectAsStateWithLifecycle()
      AddTransactionScreen(
        expenseCategories = expenseCats,
        incomeCategories = incomeCats,
        onNavigateBack = { navController.popBackStack() },
        onSaveTransaction = { type, amount, category, note ->
          viewModel.addTransaction(type, amount, category, note)
        },
        onAddCategory = { name, isExpense ->
          viewModel.addCategory(name, isExpense)
        }
      )
    }
  }
}