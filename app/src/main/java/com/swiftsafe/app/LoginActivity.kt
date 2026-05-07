package com.swiftsafe.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            goToHome()
            return
        }

        // Get references to UI elements
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Login button click
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            // Validate inputs
            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Please enter your password"
                return@setOnClickListener
            }

            // Show loading
            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            // Login with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // Login successful
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    goToHome()
                }
                .addOnFailureListener { e ->
                    // Login failed
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Go to Sign Up screen
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Forgot password click
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset email sent!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error sending reset email", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}