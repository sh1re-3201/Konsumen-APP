package com.example.konsumen
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
class MainActivity : Activity() {
    private lateinit var panel: LinearLayout
    private lateinit var daftar: LinearLayout
    private lateinit var status: TextView
    private lateinit var simpan: Button
    private lateinit var hapus: Button
    private val kolom = listOf("nama", "email", "telepon", "alamat")
    private lateinit var input: List<EditText>
    private var idPilihan: Long? = null
    private var sibuk = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val margin = (16 * resources.displayMetrics.density).toInt()
        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val tepi = insets.getInsets(WindowInsets.Type.systemBars())
            panel.setPadding(margin + tepi.left, margin + tepi.top,
                margin + tepi.right, margin + tepi.bottom)
            insets
        }
        scroll.addView(panel)
        setContentView(scroll)
        tampilTeks("Data Konsumen", 24f)
        input = kolom.map { field ->
            EditText(this).apply {
                hint = field.replaceFirstChar { it.uppercase() }
                contentDescription = hint
                setSingleLine()
                inputType = when (field) {
                    "email" -> InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    "telepon" -> InputType.TYPE_CLASS_PHONE
                    else -> InputType.TYPE_CLASS_TEXT
                }
                panel.addView(this)
            }
        }
        aturTombolDanDaftar()
    }
    private fun tampilTeks(teks: String, ukuran: Float): TextView =
        TextView(this).apply {
            text = teks
            textSize = ukuran
            panel.addView(this)
        }
    private fun tombol(teks: String, aksi: () -> Unit): Button =
        Button(this).apply {
            text = teks
            setOnClickListener { if (!sibuk) aksi() }
            panel.addView(this)
        }
    private fun aturTombolDanDaftar() {
        simpan = tombol("Tambah konsumen") { simpanData() }
        tombol("Kosongkan formulir") { kosongkan() }
        hapus = tombol("Hapus konsumen terpilih") { konfirmasiHapus() }
        hapus.isEnabled = false
        tombol("Muat ulang daftar") { muat() }
        status = tampilTeks("", 15f)
        tampilTeks("Daftar Konsumen", 20f)
        daftar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            panel.addView(this)
        }
        muat()
    }
    private fun kosongkan() {
        idPilihan = null
        input.forEach { it.text.clear() }
        simpan.text = "Tambah konsumen"
        hapus.isEnabled = false
    }
    private fun pilih(baris: JSONObject) {
        idPilihan = baris.getLong("id")
        input.zip(kolom).forEach { (edit, nama) ->
            edit.setText(baris.getString(nama))
        }
        simpan.text = "Simpan perubahan"
        hapus.isEnabled = true
        status.text = "Mengedit konsumen #$idPilihan"
        (panel.parent as ScrollView).smoothScrollTo(0, 0)
    }
    private fun gambarDaftar(data: JSONArray) {
        daftar.removeAllViews()
        if (data.length() == 0) {
            daftar.addView(TextView(this).apply {
                text = "Belum ada konsumen."
            })
        }
        for (i in 0 until data.length()) {
            val baris = data.getJSONObject(i)
            daftar.addView(Button(this).apply {
                isAllCaps = false
                text = "#${baris.getLong("id")} · " +
                        baris.getString("nama") + "\n" +
                        baris.getString("email")
                setOnClickListener { if (!sibuk) pilih(baris) }
            })
        }
    }
    private fun muat(pesan: String = "") {
        jalankan(
            kerja = { KonsumenApi.request("GET").getJSONArray("data") }
        ) { data ->
            gambarDaftar(data)
            status.text = pesan + "${data.length()} konsumen."
        }
    }
    private fun simpanData() {
        val nilai = input.map { it.text.toString().trim() }
        val kosong = nilai.indexOfFirst { it.isBlank() }
        if (kosong >= 0) {
            input[kosong].error = "Wajib diisi"
            input[kosong].requestFocus()
            return
        }
        val body = JSONObject()
        kolom.zip(nilai).forEach { (nama, isi) -> body.put(nama, isi) }
        val id = idPilihan
        val method = if (id == null) "POST" else "PUT"
        jalankan(kerja = { KonsumenApi.request(method, id, body) }) {
            kosongkan()
            muat("Data tersimpan. ")
        }
    }
    private fun konfirmasiHapus() {
        val id = idPilihan ?: return
        AlertDialog.Builder(this)
            .setTitle("Hapus konsumen #$id?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                jalankan(kerja = { KonsumenApi.request("DELETE", id) }) {
                    kosongkan()
                    muat("Data dihapus. ")
                }
            }
            .show()
    }
    private fun <T> jalankan(kerja: () -> T, sukses: (T) -> Unit) {
        if (sibuk) return
        sibuk = true
        input.forEach { it.isEnabled = false }
        status.text = "Menghubungi server…"
        thread {
            val hasil = runCatching(kerja)
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                sibuk = false
                input.forEach { it.isEnabled = true }
                hasil.fold(sukses) {
                    status.text = it.message ?: "Permintaan gagal."
                }
            }
        }
    }
}