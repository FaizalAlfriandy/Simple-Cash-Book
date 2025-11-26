package com.faizal.bukukas

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "TX_TYPE"              // optional input when launching (0 = menerima, 1 = membayar)
        const val RESULT_DESC = "RESULT_DESC"
        const val RESULT_AMOUNT = "RESULT_AMOUNT"
        const val RESULT_TIMESTAMP = "RESULT_TIMESTAMP"
        const val RESULT_TYPE = "RESULT_TYPE"
    }

    private var txType = 1 // default 1 = membayar (sesuai layout awalmu)
    private var timestamp: Long = System.currentTimeMillis()

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var btnMenerima: Button
    private lateinit var btnMembayar: Button
    private lateinit var inputNominal: TextInputEditText
    private lateinit var inputCatatan: TextInputEditText
    private lateinit var btnTanggal: Button
    private lateinit var btnJam: Button
    private lateinit var btnSimpanKeluar: Button
    private lateinit var btnSimpanLanjut: Button

    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi) // pastikan nama file xml sama

        // bind views
        topBar = findViewById(R.id.topAppBar)
        btnMenerima = findViewById(R.id.btnMenerima)
        btnMembayar = findViewById(R.id.btnMembayar)
        inputNominal = findViewById(R.id.inputNominal)
        inputCatatan = findViewById(R.id.inputCatatan)
        btnTanggal = findViewById(R.id.btnTanggal)
        btnJam = findViewById(R.id.btnJam)
        btnSimpanKeluar = findViewById(R.id.btnSimpanKeluar)
        btnSimpanLanjut = findViewById(R.id.btnSimpanLanjut)

        // ambil tipe dari intent kalau ada (0 = menerima, 1 = membayar)
        txType = intent?.getIntExtra(EXTRA_TYPE, txType) ?: txType
        applyTypeUI()

        topBar.setNavigationOnClickListener { onBackPressed() }

        // toggle behaviour
        btnMenerima.setOnClickListener {
            txType = 0
            applyTypeUI()
        }
        btnMembayar.setOnClickListener {
            txType = 1
            applyTypeUI()
        }

        // tanggal & jam: set initial text
        updateDateTimeButtons()

        btnTanggal.setOnClickListener { pickDate() }
        btnJam.setOnClickListener { pickTime() }

        // Simpan dan keluar -> kembalikan hasil ke MainActivity
        btnSimpanKeluar.setOnClickListener {
            if (validateAndSendResult()) {
                // kalau sukses, finish() akan dipanggil di validateAndSendResult
            }
        }

        // Simpan dan lanjutkan -> kirim result ke MainActivity, lalu buka ulang activity ini (tipe sama)
        btnSimpanLanjut.setOnClickListener {
            if (validateAndSendResult(sendResultOnly = true)) {
                // restart sendiri supaya user bisa langsung menambah lagi
                val restart = Intent(this, AddTransactionActivity::class.java)
                restart.putExtra(EXTRA_TYPE, txType)
                startActivity(restart)
                // finish current so previous MainActivity receives the result
                finish()
            }
        }
    }

    private fun applyTypeUI() {
        // aktif/inaktif backgrounds (drawable yang sudah kamu buat)
        if (txType == 0) {
            // menerima active, membayar inactive
            btnMenerima.setBackgroundResource(R.drawable.bg_menerima_active)
            btnMenerima.setTextColor(resources.getColor(android.R.color.white))
            btnMembayar.setBackgroundResource(R.drawable.bg_membayar_inactive)
            btnMembayar.setTextColor(resources.getColor(android.R.color.black))
            topBar.title = "Kamu Menerima"
            inputNominal.hint = "Kamu Menerima"
        } else {
            // membayar active
            btnMenerima.setBackgroundResource(R.drawable.bg_menerima_inactive)
            btnMenerima.setTextColor(resources.getColor(android.R.color.black))
            btnMembayar.setBackgroundResource(R.drawable.bg_membayar_active)
            btnMembayar.setTextColor(resources.getColor(android.R.color.white))
            topBar.title = "Kamu Membayar"
            inputNominal.hint = "Kamu Membayar"
        }
    }

    private fun updateDateTimeButtons() {
        val d = Date(timestamp)
        btnTanggal.text = dateFormat.format(d)
        btnJam.text = timeFormat.format(d)
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timestamp = cal.timeInMillis
            updateDateTimeButtons()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        TimePickerDialog(this, { _, hourOfDay, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            timestamp = cal.timeInMillis
            updateDateTimeButtons()
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    /**
     * Validasi input, kirimkan Intent result ke pemanggil.
     * @param sendResultOnly jika true, method hanya menyiapkan result dan mengembalikannya tanpa memanggil finish()
     *                         (dalam penggunaan 'Simpan dan Lanjutkan' kita akan restart activity lalu finish current)
     * @return true jika validasi sukses (result siap/telah dikirim)
     */
    private fun validateAndSendResult(sendResultOnly: Boolean = false): Boolean {
        val desc = inputCatatan.text?.toString()?.trim().orEmpty()
        val amountText = inputNominal.text?.toString()?.trim().orEmpty()

        // coba bersihkan titik atau koma pengguna
        val cleaned = amountText.replace(".", "").replace(",", "")
        val amount = cleaned.toLongOrNull()

        if (amount == null || amount <= 0L) {
            inputNominal.error = "Masukkan nominal valid"
            inputNominal.requestFocus()
            return false
        }

        // Siapkan intent result
        val data = Intent().apply {
            putExtra(RESULT_DESC, if (desc.isEmpty()) "-" else desc)
            putExtra(RESULT_AMOUNT, amount)
            putExtra(RESULT_TIMESTAMP, timestamp)
            putExtra(RESULT_TYPE, txType)
        }

        // kirim result
        setResult(Activity.RESULT_OK, data)

        // jika bukan hanya sendResultOnly, finish activity
        if (!sendResultOnly) {
            finish()
        }

        Toast.makeText(this, "Transaksi disimpan", Toast.LENGTH_SHORT).show()
        return true
    }
}
