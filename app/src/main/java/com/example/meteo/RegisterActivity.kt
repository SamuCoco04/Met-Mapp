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
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmailRegister)
        etPassword = findViewById(R.id.etPasswordRegister)
        etConfirmPassword = findViewById(R.id.etPasswordConfirm)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        btnCreateAccount.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar datos en preferencias (simulación de registro)
            prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putString("password", password)
                .apply()

            Toast.makeText(this, "Cuenta creada. Ahora inicia sesión.", Toast.LENGTH_SHORT).show()
            finish() // volvemos al LoginActivity
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}
