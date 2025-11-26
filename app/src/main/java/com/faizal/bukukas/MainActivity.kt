package com.faizal.bukukas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// Simple data model lokal (untuk sementara, nanti pindah ke file model / Room)
data class Transaction(
    val id: Long,
    val description: String,
    val amount: Long,
    val type: Int, // 0 = menerima (income), 1 = membayar (expense)
    val timestamp: Long
)

class MainActivity : AppCompatActivity() {

    private lateinit var topBar: MaterialToolbar
    private lateinit var rvKas: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private val transactions = ArrayList<Transaction>()
    private var nextId = 1L

    // current filter placeholder (not fully implemented date-filtering here)
    private var currentFilter = "ALL"

    // ActivityResult launcher untuk menerima data dari AddTransactionActivity
    private val addTxLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val desc = data.getStringExtra(AddTransactionActivity.RESULT_DESC) ?: "-"
            val amount = data.getLongExtra(AddTransactionActivity.RESULT_AMOUNT, 0L)
            val ts = data.getLongExtra(AddTransactionActivity.RESULT_TIMESTAMP, System.currentTimeMillis())
            val type = data.getIntExtra(AddTransactionActivity.EXTRA_TYPE, 0)

            // tambahkan ke list
            val tx = Transaction(id = nextId++, description = desc, amount = amount, type = type, timestamp = ts)
            transactions.add(0, tx) // tambahkan di depan (terbaru)
            adapter.notifyItemInserted(0)
            rvKas.scrollToPosition(0)
            updateTotals()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topBar = findViewById(R.id.topAppBar)
        rvKas = findViewById(R.id.rvKas)

        // RecyclerView setup
        adapter = TransactionsAdapter(transactions)
        rvKas.layoutManager = LinearLayoutManager(this)
        rvKas.adapter = adapter

        // Toolbar menu handling
        topBar.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    Toast.makeText(this, "Export Data... (stub)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_search -> {
                    Toast.makeText(this, "Cari Data... (stub)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_more -> {
                    Toast.makeText(this, "More options... (stub)", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        topBar.setNavigationOnClickListener {
            // kalau kamu bikin navigation drawer, buka di sini. sementara toast:
            Toast.makeText(this, "Menu Navigasi Klik!", Toast.LENGTH_SHORT).show()
        }

        // Tombol Menerima / Membayar
        val btnTerima = findViewById<View>(R.id.btnTerima)
        val btnBayar = findViewById<View>(R.id.btnBayar)

        btnTerima.setOnClickListener {
            // buka AddTransactionActivity (type = 0)
            val i = Intent(this, AddTransactionActivity::class.java)
            i.putExtra(AddTransactionActivity.EXTRA_TYPE, 0)
            addTxLauncher.launch(i)
        }

        btnBayar.setOnClickListener {
            // buka AddTransactionActivity (type = 1)
            val i = Intent(this, AddTransactionActivity::class.java)
            i.putExtra(AddTransactionActivity.EXTRA_TYPE, 1)
            addTxLauncher.launch(i)
        }

        // contoh seed data (opsional — hapus kalau mau)
        seedExampleData()
        updateTotals()
    }

    private fun seedExampleData() {
        // tambahkan contoh transaksi supaya list tidak kosong saat preview
        val now = System.currentTimeMillis()
        transactions.add(Transaction(nextId++, "Penjualan kopi", 25000, 0, now - 3600_000))
        transactions.add(Transaction(nextId++, "Beli gula", 5000, 1, now - 1800_000))
        transactions.add(Transaction(nextId++, "Deposit tabungan", 100000, 0, now - 86_400_000))
        adapter.notifyDataSetChanged()
    }

    private fun updateTotals() {
        val totalReceive = transactions.filter { it.type == 0 }.sumOf { it.amount }
        val totalPay = transactions.filter { it.type == 1 }.sumOf { it.amount }
        val balance = totalReceive - totalPay

        findViewById<TextView>(R.id.txtTotalTerima).text = "Menerima\n${formatNumber(totalReceive)}"
        findViewById<TextView>(R.id.txtTotalBayar).text = "Membayar\n${formatNumber(totalPay)}"
        findViewById<TextView>(R.id.txtSaldo).text = "Saldo\n${formatNumber(balance)}"
    }

    private fun formatNumber(value: Long): String {
        // format sederhana dengan ribuan (contoh: 100000 -> 100.000)
        return java.text.NumberFormat.getInstance(Locale("in", "ID")).format(value)
    }

    // --- RecyclerView Adapter sederhana ---
    inner class TransactionsAdapter(private val items: List<Transaction>) :
        RecyclerView.Adapter<TransactionsAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tx = items[position]
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(tx.timestamp))
            holder.tvAmount.text = if (tx.type == 0) {
                // menerima (green)
                holder.tvAmount.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                "+ ${formatNumber(tx.amount)}"
            } else {
                // membayar (red)
                holder.tvAmount.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                "- ${formatNumber(tx.amount)}"
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
