package com.swiftsafe.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FinancialIntelligenceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_intelligence)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvScoreLabel = findViewById<TextView>(R.id.tvScoreLabel)
        val tvTotalTransactions = findViewById<TextView>(R.id.tvTotalTransactions)
        val tvTotalSent = findViewById<TextView>(R.id.tvTotalSent)
        val tvTotalReceived = findViewById<TextView>(R.id.tvTotalReceived)
        val tvCancelled = findViewById<TextView>(R.id.tvCancelled)
        val tvTip1 = findViewById<TextView>(R.id.tvTip1)
        val tvTip2 = findViewById<TextView>(R.id.tvTip2)
        val tvTip3 = findViewById<TextView>(R.id.tvTip3)
        val tvWarning = findViewById<TextView>(R.id.tvWarning)

        val userId = auth.currentUser?.uid ?: return

        // Load wallet data
        db.collection("wallets").document(userId)
            .get()
            .addOnSuccessListener { wallet ->
                val balance = wallet.getDouble("balance") ?: 0.0
                val totalSent = wallet.getDouble("totalSent") ?: 0.0
                val totalReceived = wallet.getDouble("totalReceived") ?: 0.0

                tvTotalSent.text = "KES %.2f".format(totalSent)
                tvTotalReceived.text = "KES %.2f".format(totalReceived)

                // Load transactions
                db.collection("transactions")
                    .get()
                    .addOnSuccessListener { documents ->
                        var sentCount = 0
                        var receivedCount = 0
                        var cancelledCount = 0
                        var totalSentAmount = 0.0

                        for (doc in documents) {
                            val senderId = doc.getString("senderId") ?: ""
                            val recipientId = doc.getString("recipientId") ?: ""
                            val status = doc.getString("status") ?: ""
                            val amount = doc.getDouble("amount") ?: 0.0

                            when {
                                senderId == userId && status == "completed" -> {
                                    sentCount++
                                    totalSentAmount += amount
                                }
                                recipientId == userId -> receivedCount++
                                status == "cancelled" -> cancelledCount++
                            }
                        }

                        val totalTransactions = sentCount + receivedCount
                        tvTotalTransactions.text = totalTransactions.toString()
                        tvCancelled.text = cancelledCount.toString()

                        // Calculate Financial Health Score
                        var score = 50 // Base score

                        // Balance score (up to +30)
                        score += when {
                            balance > 5000 -> 30
                            balance > 1000 -> 20
                            balance > 500 -> 10
                            else -> 0
                        }

                        // Undo usage score (up to +20)
                        // Using undo = being careful = good!
                        if (cancelledCount > 0) score += 10

                        // Activity score (up to +10)
                        if (totalTransactions > 5) score += 10

                        // Cap score at 100
                        score = score.coerceAtMost(100)

                        tvScore.text = score.toString()
                        tvScoreLabel.text = when {
                            score >= 80 -> "🌟 Excellent!"
                            score >= 60 -> "👍 Good"
                            score >= 40 -> "⚠️ Fair"
                            else -> "❗ Needs Attention"
                        }

                        // Smart Tips based on data
                        tvTip1.text = when {
                            balance < 500 ->
                                "• 💰 Your balance is low. Try to maintain at least KES 500 as emergency buffer."
                            totalSentAmount > balance * 2 ->
                                "• 📉 You've sent more than double your current balance. Monitor spending carefully."
                            else ->
                                "• ✅ Great job maintaining your balance! Keep it up."
                        }

                        tvTip2.text = when {
                            cancelledCount > 3 ->
                                "• ↩️ You've undone $cancelledCount transactions. Double-check recipients before sending!"
                            cancelledCount > 0 ->
                                "• 👍 Smart! You used the undo feature to prevent mistakes."
                            else ->
                                "• 💡 Remember — you have 30 seconds to undo any transaction!"
                        }

                        tvTip3.text = when {
                            totalTransactions == 0 ->
                                "• 🚀 Start using SwiftSafe to track your financial activity!"
                            totalTransactions < 5 ->
                                "• 📊 Keep using SwiftSafe for better financial insights over time."
                            else ->
                                "• 🌍 You're an active SwiftSafe user! Your financial data is being tracked."
                        }

                        // Financial Warning
                        tvWarning.text = when {
                            balance == 0.0 ->
                                "Your wallet is empty. Deposit money to start transacting safely."
                            balance < 200 ->
                                "⚠️ Critical: Balance below KES 200. Top up soon to avoid transaction failures."
                            totalSentAmount > 0 && totalSentAmount > balance * 3 ->
                                "⚠️ You've been spending heavily. Consider saving before sending more."
                            else ->
                                "✅ Your finances look healthy. Keep maintaining good habits!"
                        }
                    }
            }

        tvBack.setOnClickListener { finish() }
    }
}