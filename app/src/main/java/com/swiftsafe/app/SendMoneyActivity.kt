package com.swiftsafe.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SendMoneyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentBalance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_money)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get UI references
        val etRecipient = findViewById<TextInputEditText>(R.id.etRecipient)
        val etAmount = findViewById<TextInputEditText>(R.id.etAmount)
        val etNote = findViewById<TextInputEditText>(R.id.etNote)
        val tvYourBalance = findViewById<TextView>(R.id.tvYourBalance)
        val btnSendMoney = findViewById<MaterialButton>(R.id.btnSendMoney)
        val tvCancel = findViewById<TextView>(R.id.tvCancel)

        // Load current balance
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("wallets").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        currentBalance = snapshot.getDouble("balance") ?: 0.0
                        tvYourBalance.text = "Your balance: KES %.2f".format(currentBalance)
                    }
                }
        }

        // Send Money button click
        btnSendMoney.setOnClickListener {
            val recipient = etRecipient.text.toString().trim().lowercase()
            val amountStr = etAmount.text.toString().trim()
            val note = etNote.text.toString().trim()

            // Validate inputs
            if (recipient.isEmpty()) {
                etRecipient.error = "Please enter recipient"
                return@setOnClickListener
            }
            if (amountStr.isEmpty()) {
                etAmount.error = "Please enter amount"
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etAmount.error = "Please enter a valid amount"
                return@setOnClickListener
            }
            if (amount > currentBalance) {
                etAmount.error = "Insufficient balance"
                return@setOnClickListener
            }

            // Go to countdown screen
            val intent = Intent(this, CountdownActivity::class.java)
            intent.putExtra("recipient", recipient)
            intent.putExtra("amount", amount)
            intent.putExtra("note", note)
            startActivity(intent)
        }

        // Cancel button
        tvCancel.setOnClickListener {
            finish()
        }
    }
}