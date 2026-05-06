package com.swiftsafe.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WalletActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentBalance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val tvTotalSent = findViewById<TextView>(R.id.tvTotalSent)
        val tvTotalReceived = findViewById<TextView>(R.id.tvTotalReceived)
        val etDeposit = findViewById<TextInputEditText>(R.id.etDeposit)
        val etWithdraw = findViewById<TextInputEditText>(R.id.etWithdraw)
        val btnDeposit = findViewById<MaterialButton>(R.id.btnDeposit)
        val btnWithdraw = findViewById<MaterialButton>(R.id.btnWithdraw)

        val userId = auth.currentUser?.uid ?: return

        // Load wallet data in real time
        db.collection("wallets").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    currentBalance = snapshot.getDouble("balance") ?: 0.0
                    val totalSent = snapshot.getDouble("totalSent") ?: 0.0
                    val totalReceived = snapshot.getDouble("totalReceived") ?: 0.0

                    tvBalance.text = "KES %.2f".format(currentBalance)
                    tvTotalSent.text = "KES %.2f".format(totalSent)
                    tvTotalReceived.text = "KES %.2f".format(totalReceived)
                }
            }

        // Deposit button
        btnDeposit.setOnClickListener {
            val amountStr = etDeposit.text.toString().trim()
            if (amountStr.isEmpty()) {
                etDeposit.error = "Enter amount"
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etDeposit.error = "Enter valid amount"
                return@setOnClickListener
            }

            // Add to balance
            val newBalance = currentBalance + amount
            db.collection("wallets").document(userId)
                .update(
                    "balance", newBalance,
                    "totalReceived", (db.collection("wallets")
                        .document(userId)) .let { newBalance }
                )
                .addOnSuccessListener {
                    etDeposit.setText("")
                    Toast.makeText(
                        this,
                        "KES %.2f deposited successfully!".format(amount),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // Withdraw button
        btnWithdraw.setOnClickListener {
            val amountStr = etWithdraw.text.toString().trim()
            if (amountStr.isEmpty()) {
                etWithdraw.error = "Enter amount"
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etWithdraw.error = "Enter valid amount"
                return@setOnClickListener
            }
            if (amount > currentBalance) {
                etWithdraw.error = "Insufficient balance"
                return@setOnClickListener
            }

            // Deduct from balance
            val newBalance = currentBalance - amount
            db.collection("wallets").document(userId)
                .update("balance", newBalance)
                .addOnSuccessListener {
                    etWithdraw.setText("")
                    Toast.makeText(
                        this,
                        "KES %.2f withdrawn successfully!".format(amount),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        tvBack.setOnClickListener { finish() }
    }
}