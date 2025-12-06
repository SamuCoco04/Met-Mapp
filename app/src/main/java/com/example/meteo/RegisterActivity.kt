package com.example.meteo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnCreateAccount.setOnClickListener { createAccount() }

        tvGoToLogin.setOnClickListener {
            finish() // volvemos al login
        }
    }

    private fun createAccount() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()

        Toast.makeText(this, "Cuenta creada. Ahora inicia sesión.", Toast.LENGTH_SHORT).show()
        finish()
    }
}
