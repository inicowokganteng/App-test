package com.example.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvUtils {
    fun exportToCsv(context: Context, uri: Uri, transactions: List<Transaction>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val builder = StringBuilder()
                builder.append("id,type,amount,category,note,timestamp\n")
                transactions.forEach { t ->
                    val note = t.note.replace(",", " ")
                    val category = t.category.replace(",", " ")
                    builder.append("${t.id},${t.type.name},${t.amount},${category},${note},${t.timestamp}\n")
                }
                outputStream.write(builder.toString().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importFromCsv(context: Context, uri: Uri): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String? = reader.readLine() // Skip header
                line = reader.readLine()
                while (line != null) {
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        try {
                            val type = TransactionType.valueOf(parts[1])
                            val amount = parts[2].toDouble()
                            val category = parts[3]
                            val note = parts[4]
                            val timestamp = parts[5].toLong()
                            // Force ID to 0 so room autogenerates a new one to avoid conflicts
                            transactions.add(Transaction(0, type, amount, category, note, timestamp))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return transactions
    }
}
