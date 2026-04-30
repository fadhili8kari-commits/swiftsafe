package com.swiftsafe.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class CountdownActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase
    private var countDownTimer: CountDownTimer? = null
    private var transactionId: String = ""
    private var isUndone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance()

        // Get transaction details from SendMoneyActivity
        val recipient = intent.getStringExtra("recipient") ?: ""
        val amount = intent.getDoubleExtra("amount", 0.0)
        val note = intent.getStringExtra("note") ?: ""

        // Get UI references
        val tvRecipient = findViewById<TextView>(R.id.tvRecipient)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnUndo = findViewById<MaterialButton>(R.id.btnUndo)

        // Display transaction details
        tvRecipient.text = "To: $recipient"
        tvAmount.text = "KES %.2f".format(amount)

        // Save pending transaction to Realtime Database
        val userId = auth.currentUser?.uid ?: return
        transactionId = realtimeDb.reference.push().key ?: return

        val pendingTransaction = hashMapOf(
            "senderId" to userId,
            "recipient" to recipient,
            "amount" to amount,
            "note" to note,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        realtimeDb.reference
            .child("pending_transactions")
            .child(transactionId)
            .setValue(pendingTransaction)

        // Start 30 second countdown
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvCountdown.text = secondsLeft.toString()

                // Change color to red when less than 10 seconds
                if (secondsLeft <= 10) {
                    tvCountdown.parent.let {
                        tvCountdown.setBackgroundResource(R.drawable.countdown_circle_red)
                    }
                    tvStatus.text = "⚠️ Hurry! Only $secondsLeft seconds left to undo!"
                }
            }

            override fun onFinish() {
                if (!isUndone) {
                    // Timer finished — complete the transaction
                    completeTransaction(userId, recipient, amount, note)
                }
            }
        }.start()

        // Undo button click
        btnUndo.setOnClickListener {
            if (!isUndone) {
                isUndone = true
                countDownTimer?.cancel()
                undoTransaction()
            }
        }
    }

    private fun completeTransaction(
        userId: String,
        recipient: String,
        amount: Double,
        note: String
    ) {
        // Find recipient by email
        db.collection("users")
            .whereEqualTo("email", recipient)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Recipient not found!", Toast.LENGTH_LONG).show()
                    undoTransaction()
                    return@addOnSuccessListener
                }

                val recipientDoc = documents.first()
                val recipientId = recipientDoc.id

                // Deduct from sender
                db.collection("wallets").document(userId)
                    .get()
                    .addOnSuccessListener { senderWallet ->
                        val senderBalance = senderWallet.getDouble("balance") ?: 0.0
                        val newSenderBalance = senderBalance - amount

                        db.collection("wallets").document(userId)
                            .update("balance", newSenderBalance)

                        // Add to recipient
                        db.collection("wallets").document(recipientId)
                            .get()
                            .addOnSuccessListener { recipientWallet ->
                                val recipientBalance = recipientWallet.getDouble("balance") ?: 0.0
                                db.collection("wallets").document(recipientId)
                                    .update("balance", recipientBalance + amount)
                            }

                        // Save completed transaction to Firestore
                        val transaction = hashMapOf(
                            "senderId" to userId,
                            "recipientId" to recipientId,
                            "amount" to amount,
                            "note" to note,
                            "status" to "completed",
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("transactions").add(transaction)

                        // Remove from pending
                        realtimeDb.reference
                            .child("pending_transactions")
                            .child(transactionId)
                            .removeValue()

                        Toast.makeText(
                            this,
                            "Transaction completed! ✅",
                            Toast.LENGTH_LONG
                        ).show()

                        // Go back to Home
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
            }
    }

    private fun undoTransaction() {
        // Remove pending transaction
        realtimeDb.reference
            .child("pending_transactions")
            .child(transactionId)
            .removeValue()

        Toast.makeText(
            this,
            "Transaction cancelled! ↩️ Money not sent.",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}