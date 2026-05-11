package com.swiftsafe.app

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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

        val recipient = intent.getStringExtra("recipient") ?: ""
        val amount = intent.getDoubleExtra("amount", 0.0)
        val note = intent.getStringExtra("note") ?: ""

        val tvRecipient = findViewById<TextView>(R.id.tvRecipient)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnUndo = findViewById<MaterialButton>(R.id.btnUndo)

        tvRecipient.text = "To: $recipient"
        tvAmount.text = "KES %.2f".format(amount)

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

        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvCountdown.text = secondsLeft.toString()
                if (secondsLeft <= 10) {
                    tvCountdown.setBackgroundResource(R.drawable.countdown_circle_red)
                    tvStatus.text = "⚠️ Hurry! Only $secondsLeft seconds left to undo!"
                }
            }

            override fun onFinish() {
                if (!isUndone) {
                    completeTransaction(userId, recipient, amount, note)
                }
            }
        }.start()

        btnUndo.setOnClickListener {
            if (!isUndone) {
                isUndone = true
                countDownTimer?.cancel()
                undoTransaction()
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun completeTransaction(
        userId: String,
        recipient: String,
        amount: Double,
        note: String
    ) {
        if (!isInternetAvailable()) {
            Toast.makeText(
                this,
                "⚠️ No internet. Transaction cancelled for safety.",
                Toast.LENGTH_LONG
            ).show()
            undoTransaction()
            return
        }

        db.collection("users")
            .whereEqualTo("email", recipient)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "Recipient not found!",
                        Toast.LENGTH_LONG
                    ).show()
                    undoTransaction()
                    return@addOnSuccessListener
                }

                val recipientDoc = documents.first()
                val recipientId = recipientDoc.id

                db.collection("wallets").document(userId)
                    .get()
                    .addOnSuccessListener { senderWallet ->
                        val senderBalance = senderWallet.getDouble("balance") ?: 0.0
                        val newSenderBalance = senderBalance - amount

                        db.collection("wallets").document(userId)
                            .update("balance", newSenderBalance)

                        db.collection("wallets").document(recipientId)
                            .get()
                            .addOnSuccessListener { recipientWallet ->
                                val recipientBalance =
                                    recipientWallet.getDouble("balance") ?: 0.0
                                db.collection("wallets").document(recipientId)
                                    .update("balance", recipientBalance + amount)
                            }

                        val transaction = hashMapOf(
                            "senderId" to userId,
                            "recipientId" to recipientId,
                            "recipient" to recipient,
                            "amount" to amount,
                            "note" to note,
                            "status" to "completed",
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("transactions").add(transaction)

                        realtimeDb.reference
                            .child("pending_transactions")
                            .child(transactionId)
                            .removeValue()

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("✅ Transaction Complete!")
                            .setMessage(
                                "You successfully sent KES %.2f to %s"
                                    .format(amount, recipient)
                            )
                            .setPositiveButton("Done") { _, _ ->
                                startActivity(
                                    Intent(this, HomeActivity::class.java)
                                )
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "⚠️ Transaction failed. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                undoTransaction()
            }
    }

    private fun undoTransaction() {
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