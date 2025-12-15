package com.example.meteo
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


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerStations: Spinner
    private lateinit var btnEnter: Button
    private lateinit var tvStatus: TextView

    private var stationIds: List<String> = emptyList()
    private var selectedStationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerStations = findViewById(R.id.spinnerStations)
        btnEnter = findViewById(R.id.btnEnterStation)
        tvStatus = findViewById(R.id.tvStatusMain)

        supportActionBar?.title = "Seleccionar estación"

        tvStatus.text = "Cargando estaciones…"

        btnEnter.setOnClickListener {
            val stationId = selectedStationId
            if (stationId.isNullOrEmpty()) {
                Toast.makeText(this, "Selecciona una estación", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, StationActivity::class.java)
                intent.putExtra(StationActivity.EXTRA_STATION_ID, stationId)
                startActivity(intent)
            }
        }

        loadStations()
    }

    private fun loadStations() {
        MeteoRepository.getStationIds(
            onSuccess = { ids ->
                runOnUiThread {
                    stationIds = ids
                    if (ids.isEmpty()) {
                        tvStatus.text = "No hay estaciones disponibles"
                        setupSpinner(emptyList())
                    } else {
                        tvStatus.text = ""
                        setupSpinner(ids)
                    }
                }
            },
            onError = { ex ->
                runOnUiThread {
                    tvStatus.text = "Error al cargar estaciones"
                    Toast.makeText(
                        this,
                        "Error al obtener estaciones: ${ex.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    setupSpinner(emptyList())
                }
            }
        )
    }

    private fun setupSpinner(items: List<String>) {
        if (items.isEmpty()) {
            selectedStationId = null
            spinnerStations.adapter = null
            return
        }

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                return v
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                v.setBackgroundColor(Color.parseColor("#101818"))
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStations.adapter = adapter

        spinnerStations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedStationId = items[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedStationId = null
            }
        }

        spinnerStations.setSelection(0)
        selectedStationId = items[0]
    }
}
