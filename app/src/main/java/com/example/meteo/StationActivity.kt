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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class StationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATION_ID = "station_id"
    }

    private enum class Mode {
        STATS, CHARTS
    }

    // ----------------- Última lectura -----------------
    private lateinit var tvCardTemp: TextView
    private lateinit var tvCardHum: TextView
    private lateinit var tvCardPart: TextView
    private lateinit var tvCardTime: TextView

    // ----------------- Controles modo -----------------
    private lateinit var spinnerMode: Spinner
    private lateinit var spinnerVariable: Spinner

    // ----------------- Layouts de secciones -----------
    private lateinit var layoutStats: View
    private lateinit var layoutCharts: View

    // ----------------- Stats (tarjetas bonitas) -------
    private lateinit var tvStatsTitle: TextView
    private lateinit var tvStatsEmpty: TextView
    private lateinit var layoutStatsCards: LinearLayout

    // ----------------- Gráficas -----------------------
    private lateinit var cbTemp: CheckBox
    private lateinit var cbHum: CheckBox
    private lateinit var cbPart: CheckBox
    private lateinit var lineChart: LineChart
    private lateinit var tvChartEmpty: TextView

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var stationId: String = "STATION_00"
    private var currentMode: Mode = Mode.STATS

    private var measurements: List<MeteoRepository.Measurement> = emptyList()

    // --------------------------------------------------
    // onCreate
    // --------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station)

        stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: "STATION_00"
        supportActionBar?.title = stationId

        bindViews()
        setupSpinners()
        setupChart()
        loadData()
    }

    // --------------------------------------------------
    // View binding
    // --------------------------------------------------
    private fun bindViews() {
        tvCardTemp = findViewById(R.id.tvCardTemp)
        tvCardHum = findViewById(R.id.tvCardHum)
        tvCardPart = findViewById(R.id.tvCardParticles)
        tvCardTime = findViewById(R.id.tvCardTimestamp)

        spinnerMode = findViewById(R.id.spinnerMode)
        spinnerVariable = findViewById(R.id.spinnerVariable)

        layoutStats = findViewById(R.id.layoutStats)
        layoutCharts = findViewById(R.id.layoutCharts)

        tvStatsTitle = findViewById(R.id.tvStatsTitle)
        tvStatsEmpty = findViewById(R.id.tvStatsEmpty)
        layoutStatsCards = findViewById(R.id.layoutStatsCards)

        cbTemp = findViewById(R.id.cbTemp)
        cbHum = findViewById(R.id.cbHumidity)
        cbPart = findViewById(R.id.cbParticles)
        lineChart = findViewById(R.id.lineChart)
        tvChartEmpty = findViewById(R.id.tvChartEmpty)
    }

    // --------------------------------------------------
    // Spinners
    // --------------------------------------------------
    private fun setupSpinners() {
        // ----- Modo -----
        val modes = listOf("Ver estadísticas", "Ver gráficas")
        val modeAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            modes
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
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMode.adapter = modeAdapter
        spinnerMode.setSelection(0)

        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentMode = if (position == 0) Mode.STATS else Mode.CHARTS
                updateMode()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ----- Variable (solo para estadísticas) -----
        val variables = listOf(
            "Temperatura (°C)",
            "Humedad (%)",
            "Partículas"
        )
        val varAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            variables
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
        varAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVariable.adapter = varAdapter
        spinnerVariable.setSelection(0)

        spinnerVariable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (currentMode == Mode.STATS) {
                    updateStatsFromSelection()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Checkboxes de gráficas
        cbTemp.setOnCheckedChangeListener { _, _ ->
            if (currentMode == Mode.CHARTS) updateChart()
        }
        cbHum.setOnCheckedChangeListener { _, _ ->
            if (currentMode == Mode.CHARTS) updateChart()
        }
        cbPart.setOnCheckedChangeListener { _, _ ->
            if (currentMode == Mode.CHARTS) updateChart()
        }
    }

    // --------------------------------------------------
    // Carga de datos
    // --------------------------------------------------
    private fun loadData() {
        // cache
        measurements = MeteoRepository.getCachedMeasurements(stationId) ?: emptyList()
        updateCard()
        updateMode()

        // refresco desde Firestore
        MeteoRepository.refreshMeasurements(
            stationId,
            onSuccess = {
                runOnUiThread {
                    measurements =
                        MeteoRepository.getCachedMeasurements(stationId) ?: emptyList()
                    updateCard()
                    updateMode()
                }
            },
            onError = { ex ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error al cargar lecturas: ${ex.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // --------------------------------------------------
    // Actualizar secciones
    // --------------------------------------------------
    private fun updateMode() {
        if (currentMode == Mode.STATS) {
            layoutStats.visibility = View.VISIBLE
            layoutCharts.visibility = View.GONE
            spinnerVariable.visibility = View.VISIBLE
            updateStatsFromSelection()
        } else {
            layoutStats.visibility = View.GONE
            layoutCharts.visibility = View.VISIBLE
            spinnerVariable.visibility = View.GONE
            updateChart()
        }
    }

    private fun updateCard() {
        val latest = MeteoRepository.getLatestMeasurement(stationId)
        if (latest == null) {
            tvCardTemp.text = "-- °C"
            tvCardHum.text = "-- %"
            tvCardPart.text = "--"
            tvCardTime.text = "--"
            return
        }

        tvCardTemp.text = String.format(Locale.getDefault(), "%.1f °C", latest.temperature)
        tvCardHum.text = String.format(Locale.getDefault(), "%.1f %%", latest.humidity)
        tvCardPart.text = String.format(Locale.getDefault(), "%.1f", latest.particles)
        tvCardTime.text = dateFormatter.format(Date(latest.timestampMillis))
    }

    // --------------------------------------------------
    // Estadísticas con tarjetas
    // --------------------------------------------------
    private fun updateStatsFromSelection() {
        layoutStatsCards.removeAllViews()

        if (measurements.isEmpty()) {
            tvStatsEmpty.visibility = View.VISIBLE
            return
        } else {
            tvStatsEmpty.visibility = View.GONE
        }

        val (label, extractor, unit) = when (spinnerVariable.selectedItemPosition) {
            0 -> Triple("temperatura", { m: MeteoRepository.Measurement -> m.temperature }, "°C")
            1 -> Triple("humedad", { m: MeteoRepository.Measurement -> m.humidity }, "%")
            else -> Triple("partículas", { m: MeteoRepository.Measurement -> m.particles }, "")
        }

        tvStatsTitle.text = "Estadísticas de $label"

        val values = measurements.map(extractor)
        if (values.isEmpty()) {
            tvStatsEmpty.visibility = View.VISIBLE
            return
        }

        val min = values.minOrNull()!!
        val max = values.maxOrNull()!!
        val mean = values.average()
        val sorted = values.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
        val mode =
            values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: values.first()
        val stdDev = sqrt(values.map { (it - mean).pow(2) }.average())

        val format = if (spinnerVariable.selectedItemPosition == 2) "%.0f" else "%.2f"

        val stats = listOf(
            "Mínimo" to String.format(format, min),
            "Máximo" to String.format(format, max),
            "Media" to String.format(format, mean),
            "Mediana" to String.format(format, median),
            "Moda" to String.format(format, mode),
            "Desviación típica" to String.format(format, stdDev)
        )

        for ((title, value) in stats) {
            val view = layoutInflater.inflate(R.layout.item_state_card, layoutStatsCards, false)
            view.findViewById<TextView>(R.id.tvStatTitle).text = title
            view.findViewById<TextView>(R.id.tvStatValue).text =
                if (unit.isNotEmpty()) "$value $unit" else value
            layoutStatsCards.addView(view)
        }
    }

    // --------------------------------------------------
    // Configuración de la gráfica
    // --------------------------------------------------
    private fun setupChart() {
        lineChart.setBackgroundColor(Color.BLACK)
        lineChart.description.isEnabled = false
        lineChart.setNoDataText("No hay datos para mostrar")
        lineChart.setNoDataTextColor(Color.WHITE)

        // Interacción
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(false)
        xAxis.labelCount = 4
        xAxis.setAvoidFirstLastClipping(true)

        xAxis.valueFormatter = object : ValueFormatter() {
            private val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return df.format(Date(value.toLong()))
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(true)

        lineChart.axisRight.isEnabled = false

        val legend: Legend = lineChart.legend
        legend.isWordWrapEnabled = true
        legend.textColor = Color.WHITE

        lineChart.marker = TimeValueMarker(this, R.layout.marker_time_value)
    }

    // --------------------------------------------------
    // Actualizar gráfica (z-score para correlación)
    // --------------------------------------------------
    private fun updateChart() {
        if (measurements.isEmpty()) {
            lineChart.clear()
            tvChartEmpty.visibility = View.VISIBLE
            return
        }

        val selected = mutableListOf<Triple<String, (MeteoRepository.Measurement) -> Double, Int>>()

        if (cbTemp.isChecked) {
            selected += Triple("Temperatura (°C)", { it.temperature }, Color.parseColor("#FFA726"))
        }
        if (cbHum.isChecked) {
            selected += Triple("Humedad (%)", { it.humidity }, Color.parseColor("#42A5F5"))
        }
        if (cbPart.isChecked) {
            selected += Triple("Partículas", { it.particles }, Color.parseColor("#AB47BC"))
        }

        if (selected.isEmpty()) {
            lineChart.clear()
            tvChartEmpty.visibility = View.VISIBLE
            return
        } else {
            tvChartEmpty.visibility = View.GONE
        }

        val dataSets = mutableListOf<ILineDataSet>()

        if (selected.size > 1) {
            // VARIAS SERIES → normalizamos (z-score) para ver correlación
            val axisLeft = lineChart.axisLeft
            var globalMin = Float.POSITIVE_INFINITY
            var globalMax = Float.NEGATIVE_INFINITY

            selected.forEach { (label, extractor, color) ->
                val values = measurements.map(extractor)
                val mean = values.average()
                val variance = values.map { (it - mean).pow(2) }.average()
                val std = sqrt(variance).takeIf { it > 0 } ?: 1.0

                val entries = measurements.map { m ->
                    val real = extractor(m)
                    val z = ((real - mean) / std).toFloat()
                    if (z < globalMin) globalMin = z
                    if (z > globalMax) globalMax = z
                    Entry(m.timestampMillis.toFloat(), z, real)
                }

                val set = LineDataSet(entries, label).apply {
                    axisDependency = YAxis.AxisDependency.LEFT
                    this.color = color
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    setDrawHighlightIndicators(true)
                    highLightColor = color
                }
                dataSets += set
            }

            val padding = 0.5f
            axisLeft.axisMinimum = globalMin - padding
            axisLeft.axisMaximum = globalMax + padding

        } else {
            // UNA SERIE → valores reales
            val (label, extractor, color) = selected[0]

            val values = measurements.map(extractor)
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            val range = (max - min).takeIf { it > 0 } ?: 1.0
            val padding = range * 0.1

            val entries = measurements.map { m ->
                val real = extractor(m)
                Entry(m.timestampMillis.toFloat(), real.toFloat(), real)
            }

            val set = LineDataSet(entries, label).apply {
                axisDependency = YAxis.AxisDependency.LEFT
                this.color = color
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawHighlightIndicators(true)
                highLightColor = color
            }
            dataSets += set

            val axisLeft = lineChart.axisLeft
            axisLeft.axisMinimum = (min - padding).toFloat()
            axisLeft.axisMaximum = (max + padding).toFloat()
        }

        lineChart.data = LineData(dataSets)

        // ----- Ventana visible y scroll horizontal -----
        val minX = measurements.minOf { it.timestampMillis }.toFloat()
        val maxX = measurements.maxOf { it.timestampMillis }.toFloat()
        val totalRange = maxX - minX

        if (totalRange > 0f) {
            // mostramos aprox. el último tercio y se puede deslizar
            val visibleRange = totalRange / 3f
            lineChart.setVisibleXRangeMaximum(visibleRange)
            lineChart.moveViewToX(maxX)
        }

        lineChart.invalidate()
    }
}
