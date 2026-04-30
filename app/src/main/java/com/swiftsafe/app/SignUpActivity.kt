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

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to UI elements
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // Sign Up button click
        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validate inputs
            if (fullName.isEmpty()) {
                etFullName.error = "Please enter your full name"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Please enter your phone number"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Please enter a password"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Show loading
            btnSignUp.isEnabled = false
            btnSignUp.text = "Creating account..."

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Save user details to Firestore
                    val user = hashMapOf(
                        "name" to fullName,
                        "email" to email,
                        "phone" to phone,
                        "balance" to 0.0,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            // Also create wallet in Firestore
                            val wallet = hashMapOf(
                                "balance" to 0.0,
                                "totalSent" to 0.0,
                                "totalReceived" to 0.0,
                                "currency" to "KES",
                                "lastUpdated" to System.currentTimeMillis()
                            )
                            db.collection("wallets").document(userId)
                                .set(wallet)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                        }
                        .addOnFailureListener { e ->
                            btnSignUp.isEnabled = true
                            btnSignUp.text = "Create Account"
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "Create Account"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Go back to Login
        tvLogin.setOnClickListener {
            finish()
        }
    }
}