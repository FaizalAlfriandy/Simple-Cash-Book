package com.faizal.bukukas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class Transaction(
    val id: Long,
    val description: String,
    val amount: Long,
    val type: Int,
    val timestamp: Long
)

class MainActivity : AppCompatActivity() {

    private var topBar: MaterialToolbar? = null
    private var rvKas: RecyclerView? = null
    private lateinit var adapter: TransactionsAdapter
    private val transactions = ArrayList<Transaction>()
    private var nextId = 1L

    private val addTxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val desc = data.getStringExtra(AddTransactionActivity.RESULT_DESC) ?: "-"
                val amount = data.getLongExtra(AddTransactionActivity.RESULT_AMOUNT, 0L)
                val ts = data.getLongExtra(
                    AddTransactionActivity.RESULT_TIMESTAMP,
                    System.currentTimeMillis()
                )
                val type = data.getIntExtra(AddTransactionActivity.RESULT_TYPE, 0)

                val tx = Transaction(
                    id = nextId++,
                    description = desc,
                    amount = amount,
                    type = type,
                    timestamp = ts
                )

                adapter.addAtTop(tx)
                rvKas?.scrollToPosition(0)
                updateTotals()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Log.e("MainActivity", "setContentView failed", e)
            Toast.makeText(this, "Error loading layout!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        topBar = findViewById(R.id.topAppBar)
        rvKas = findViewById(R.id.rvKas)

        if (rvKas == null || topBar == null) {
            Toast.makeText(this, "Komponen layout tidak lengkap!", Toast.LENGTH_LONG)
                .show()
            return
        }

        adapter = TransactionsAdapter(transactions)
        rvKas!!.layoutManager = LinearLayoutManager(this)
        rvKas!!.adapter = adapter

        topBar!!.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    Toast.makeText(this, "Export...", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.action_search -> {
                    Toast.makeText(this, "Search...", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.action_more -> {
                    Toast.makeText(this, "More...", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        topBar!!.setNavigationOnClickListener {
            Toast.makeText(this, "Menu diklik", Toast.LENGTH_SHORT).show()
        }

        val btnTerima = findViewById<View?>(R.id.btnTerima)
        val btnBayar = findViewById<View?>(R.id.btnBayar)

        btnTerima?.setOnClickListener {
            val i = Intent(this, AddTransactionActivity::class.java)
            i.putExtra(AddTransactionActivity.EXTRA_TYPE, 0)
            addTxLauncher.launch(i)
        }

        btnBayar?.setOnClickListener {
            val i = Intent(this, AddTransactionActivity::class.java)
            i.putExtra(AddTransactionActivity.EXTRA_TYPE, 1)
            addTxLauncher.launch(i)
        }

        seedExampleData()
        updateTotals()
    }

    private fun seedExampleData() {
        val now = System.currentTimeMillis()
        transactions.add(Transaction(nextId++, "Penjualan kopi", 25000, 0, now - 3600000))
        transactions.add(Transaction(nextId++, "Beli gula", 5000, 1, now - 1800000))
        transactions.add(
            Transaction(
                nextId++,
                "Deposit tabungan",
                100000,
                0,
                now - 86400000
            )
        )
        adapter.updateAll(transactions)
    }

    private fun updateTotals() {
        val totalReceive = transactions.filter { it.type == 0 }.sumOf { it.amount }
        val totalPay = transactions.filter { it.type == 1 }.sumOf { it.amount }
        val balance = totalReceive - totalPay

        findViewById<TextView?>(R.id.txtTotalTerima)?.text =
            "Menerima\n${formatNumber(totalReceive)}"
        findViewById<TextView?>(R.id.txtTotalBayar)?.text =
            "Membayar\n${formatNumber(totalPay)}"
        findViewById<TextView?>(R.id.txtSaldo)?.text =
            "Saldo\n${formatNumber(balance)}"
    }

    private fun formatNumber(value: Long): String =
        java.text.NumberFormat.getInstance(Locale("in", "ID")).format(value)

    // ADAPTER + FITUR HAPUS
    inner class TransactionsAdapter(private val items: MutableList<Transaction>) :
        RecyclerView.Adapter<TransactionsAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView? = itemView.findViewById(R.id.tvDate)
            val tvDesc: TextView? = itemView.findViewById(R.id.tvDesc)
            val tvAmount: TextView? = itemView.findViewById(R.id.tvAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tx = items[position]
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())
            holder.tvDate?.text = sdf.format(Date(tx.timestamp))
            holder.tvDesc?.text = tx.description

            val formatted = formatNumber(tx.amount)
            if (tx.type == 0) {
                holder.tvAmount?.text = "+ $formatted"
                holder.tvAmount?.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        android.R.color.holo_green_dark
                    )
                )
            } else {
                holder.tvAmount?.text = "- $formatted"
                holder.tvAmount?.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        android.R.color.holo_red_dark
                    )
                )
            }

            // FITUR HAPUS DENGAN CARA LONG CLICK >-<
            holder.itemView.setOnLongClickListener {
                val pos = holder.adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener true

                val data = items[pos]

                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Hapus Laporan")
                    .setMessage("Yakin ingin menghapus transaksi:\n\"${data.description}\"?")
                    .setPositiveButton("Hapus") { _, _ ->
                        items.removeAt(pos)
                        notifyItemRemoved(pos)
                        notifyItemRangeChanged(pos, items.size)
                        this@MainActivity.updateTotals()

                        Toast.makeText(
                            holder.itemView.context,
                            "Laporan dihapus",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()

                true
            }
        }

        override fun getItemCount(): Int = items.size

        fun addAtTop(tx: Transaction) {
            items.add(0, tx)
            notifyItemInserted(0)
        }

        fun updateAll(newList: List<Transaction>) {
            items.clear()
            items.addAll(newList)
            notifyDataSetChanged()
        }
    }
}
