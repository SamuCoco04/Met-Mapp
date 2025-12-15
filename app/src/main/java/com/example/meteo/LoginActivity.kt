/*
 * ------------------------------------------------------------
 * Project: Met-Mapp
 *
 * Developers:
 *   - Samuel Coco Delfa — nº Alumn: a22507106
 *   - Carlos Galea Magro — nº Alumn: a22506794
 *   - Javier Sánchez Gonzalo — nº Alumn: a22506948
 *
 * ------------------------------------------------------------
 */

package com.example.meteo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        // si quieres, puedes rellenar los campos con la cuenta por defecto:
        // etEmail.setText("a@gmail.com")
        // etPassword.setText("1234")

        btnLogin.setOnClickListener { performLogin() }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Cuenta por defecto
        if (email == "a@gmail.com" && password == "1234") {
            openMain()
            return
        }

        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedEmail = prefs.getString("user_email", null)
        val savedPassword = prefs.getString("user_password", null)

        if (email == savedEmail && password == savedPassword) {
            openMain()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        // Al cerrar la app se pierde la sesión (no guardamos flag de login)
        finish()
    }
}
