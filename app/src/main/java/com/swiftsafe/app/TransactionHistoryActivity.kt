package com.swiftsafe.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var allTransactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val emptyState = findViewById<LinearLayout>(R.id.emptyState)
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabSent = findViewById<TextView>(R.id.tabSent)
        val tabReceived = findViewById<TextView>(R.id.tabReceived)
        val tabCancelled = findViewById<TextView>(R.id.tabCancelled)

        rvTransactions.layoutManager = LinearLayoutManager(this)

        val userId = auth.currentUser?.uid ?: return

        fun resetTabs() {
            listOf(tabAll, tabSent, tabReceived, tabCancelled).forEach {
                it.setTextColor(0xFF1A73E8.toInt())
                it.setBackgroundColor(0xFFFFFFFF.toInt())
            }
        }

        fun loadTransactions(filter: String = "all") {
            db.collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    allTransactions.clear()
                    for (doc in documents) {
                        val senderId = doc.getString("senderId") ?: ""
                        val recipientId = doc.getString("recipientId") ?: ""
                        val status = doc.getString("status") ?: ""

                        if (senderId == userId || recipientId == userId) {
                            val transaction = Transaction(
                                id = doc.id,
                                senderId = senderId,
                                recipientId = recipientId,
                                recipient = doc.getString("recipient") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                note = doc.getString("note") ?: "",
                                status = status,
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                            when (filter) {
                                "all" -> allTransactions.add(transaction)
                                "sent" -> if (senderId == userId && status == "completed")
                                    allTransactions.add(transaction)
                                "received" -> if (recipientId == userId)
                                    allTransactions.add(transaction)
                                "cancelled" -> if (status == "cancelled")
                                    allTransactions.add(transaction)
                            }
                        }
                    }
                    if (allTransactions.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        rvTransactions.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        rvTransactions.visibility = View.VISIBLE
                        rvTransactions.adapter = TransactionAdapter(allTransactions, userId)
                    }
                }
        }

        loadTransactions("all")

        tabAll.setOnClickListener {
            resetTabs()
            tabAll.setTextColor(0xFFFFFFFF.toInt())
            tabAll.setBackgroundColor(0xFF1A73E8.toInt())
            loadTransactions("all")
        }
        tabSent.setOnClickListener {
            resetTabs()
            tabSent.setTextColor(0xFFFFFFFF.toInt())
            tabSent.setBackgroundColor(0xFF1A73E8.toInt())
            loadTransactions("sent")
        }
        tabReceived.setOnClickListener {
            resetTabs()
            tabReceived.setTextColor(0xFFFFFFFF.toInt())
            tabReceived.setBackgroundColor(0xFF1A73E8.toInt())
            loadTransactions("received")
        }
        tabCancelled.setOnClickListener {
            resetTabs()
            tabCancelled.setTextColor(0xFFFFFFFF.toInt())
            tabCancelled.setBackgroundColor(0xFF1A73E8.toInt())
            loadTransactions("cancelled")
        }

        tvBack.setOnClickListener { finish() }
    }
}