package com.swiftsafe.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Transaction(
    val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val recipient: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val status: String = "",
    val createdAt: Long = 0L
)

class TransactionAdapter(private val transactions: List<Transaction>,
                         private val currentUserId: String) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        val tvRecipient: TextView = view.findViewById(R.id.tvRecipient)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val isSender = transaction.senderId == currentUserId

        // Set icon based on transaction type
        holder.tvIcon.text = when {
            transaction.status == "cancelled" -> "↩️"
            isSender -> "💸"
            else -> "📥"
        }

        // Set recipient/sender info
        holder.tvRecipient.text = if (isSender) {
            "To: ${transaction.recipient}"
        } else {
            "From: ${transaction.senderId}"
        }

        // Format date
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(transaction.createdAt))

        // Set status
        holder.tvStatus.text = transaction.status.capitalize()
        holder.tvStatus.setTextColor(
            when (transaction.status) {
                "completed" -> 0xFF4CAF50.toInt()
                "cancelled" -> 0xFFFF0000.toInt()
                else -> 0xFFFF9800.toInt()
            }
        )

        // Set amount with color
        if (isSender && transaction.status != "cancelled") {
            holder.tvAmount.text = "- KES %.2f".format(transaction.amount)
            holder.tvAmount.setTextColor(0xFFFF0000.toInt())
        } else if (!isSender) {
            holder.tvAmount.text = "+ KES %.2f".format(transaction.amount)
            holder.tvAmount.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.tvAmount.text = "KES %.2f".format(transaction.amount)
            holder.tvAmount.setTextColor(0xFF666666.toInt())
        }
    }

    override fun getItemCount() = transactions.size
}