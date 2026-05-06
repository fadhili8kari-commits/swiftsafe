package com.swiftsafe.app

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import android.widget.Toast

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isBalanceVisible = true
    private var currentBalance = "KES 0.00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get UI references
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val tvInsight = findViewById<TextView>(R.id.tvInsight)
        val tvLogout = findViewById<TextView>(R.id.tvLogout)
        val tvToggleBalance = findViewById<TextView>(R.id.tvToggleBalance)
        val btnSend = findViewById<LinearLayout>(R.id.btnSend)
        val btnHistory = findViewById<LinearLayout>(R.id.btnHistory)
        val btnWallet = findViewById<LinearLayout>(R.id.btnWallet)
        val btnRequest = findViewById<LinearLayout>(R.id.btnRequest)

        // Set greeting based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else -> "Good evening,"
        }

        // Load user data from Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name") ?: "User"
                        tvUserName.text = name.split(" ")[0]
                    }
                }

            // Load wallet balance
            db.collection("wallets").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val balance = snapshot.getDouble("balance") ?: 0.0
                        currentBalance = "KES %.2f".format(balance)
                        if (isBalanceVisible) {
                            tvBalance.text = currentBalance
                        }
                        tvInsight.text = when {
                            balance == 0.0 -> "👋 Welcome! Deposit money to get started."
                            balance < 100 -> "⚠️ Your balance is running low. Consider topping up!"
                            balance < 500 -> "💡 You have a moderate balance. Keep it up!"
                            else -> "🌟 Great balance! You're in a good financial position."
                        }
                    }
                }
        }

        // Quick action buttons
        btnSend.setOnClickListener {
            startActivity(Intent(this, SendMoneyActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        btnWallet.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        btnRequest.setOnClickListener {
            startActivity(Intent(this, FinancialIntelligenceActivity::class.java))
        }

        // Logout button
        tvLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Toggle balance visibility
        tvToggleBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            if (isBalanceVisible) {
                tvBalance.text = currentBalance
                tvToggleBalance.text = "👁️"
            } else {
                tvBalance.text = "KES ****"
                tvToggleBalance.text = "👁️‍🗨️"
            }
        }
    }

    // Back button protection
    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit SwiftSafe")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}