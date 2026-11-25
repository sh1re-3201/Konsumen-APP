package com.example.konsumen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.konsumen.ui.theme.KonsumenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private data class Konsumen(
    val id: Long,
    val nama: String,
    val email: String,
    val telepon: String,
    val alamat: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KonsumenTheme {
                KonsumenScreen()
            }
        }
    }
}

@Composable
private fun KonsumenScreen() {
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var fieldError by remember { mutableStateOf<String?>(null) }
    var idPilihan by remember { mutableStateOf<Long?>(null) }
    var daftar by remember { mutableStateOf<List<Konsumen>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    var tampilDialogHapus by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val fokusNama = remember { FocusRequester() }
    val fokusEmail = remember { FocusRequester() }
    val fokusTelepon = remember { FocusRequester() }
    val fokusAlamat = remember { FocusRequester() }

    fun kosongkan() {
        idPilihan = null
        nama = ""
        email = ""
        telepon = ""
        alamat = ""
        fieldError = null
    }

    fun pilih(item: Konsumen) {
        idPilihan = item.id
        nama = item.nama
        email = item.email
        telepon = item.telepon
        alamat = item.alamat
        fieldError = null
        status = "Mengedit konsumen #${item.id}"
        scope.launch { scrollState.animateScrollTo(0) }
    }

    fun <T> jalankan(kerja: () -> T, sukses: (T) -> Unit) {
        if (sibuk) return
        sibuk = true
        status = "Menghubungi server…"
        scope.launch {
            val hasil = withContext(Dispatchers.IO) { runCatching(kerja) }
            sibuk = false
            hasil.fold(
                onSuccess = sukses,
                onFailure = { status = it.message ?: "Permintaan gagal." }
            )
        }
    }

    fun muat(pesan: String = "") {
        jalankan(
            kerja = { parseDaftar(KonsumenApi.request("GET").getJSONArray("data")) }
        ) { data ->
            daftar = data
            status = "$pesan${data.size} konsumen."
        }
    }

    fun simpanData() {
        val nilaiNama = nama.trim()
        val nilaiEmail = email.trim()
        val nilaiTelepon = telepon.trim()
        val nilaiAlamat = alamat.trim()

        when {
            nilaiNama.isBlank() -> {
                fieldError = "nama"
                fokusNama.requestFocus()
                return
            }
            nilaiEmail.isBlank() -> {
                fieldError = "email"
                fokusEmail.requestFocus()
                return
            }
            nilaiTelepon.isBlank() -> {
                fieldError = "telepon"
                fokusTelepon.requestFocus()
                return
            }
            nilaiAlamat.isBlank() -> {
                fieldError = "alamat"
                fokusAlamat.requestFocus()
                return
            }
        }

        fieldError = null
        val body = JSONObject().apply {
            put("nama", nilaiNama)
            put("email", nilaiEmail)
            put("telepon", nilaiTelepon)
            put("alamat", nilaiAlamat)
        }
        val id = idPilihan
        val method = if (id == null) "POST" else "PUT"
        jalankan(kerja = { KonsumenApi.request(method, id, body) }) {
            kosongkan()
            muat("Data tersimpan. ")
        }
    }

    fun hapusData(id: Long) {
        jalankan(kerja = { KonsumenApi.request("DELETE", id) }) {
            kosongkan()
            muat("Data dihapus. ")
        }
    }

    LaunchedEffect(Unit) {
        muat()
    }

    if (tampilDialogHapus && idPilihan != null) {
        val id = idPilihan!!
        AlertDialog(
            onDismissRequest = { tampilDialogHapus = false },
            title = { Text("Hapus konsumen #$id?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tampilDialogHapus = false
                        hapusData(id)
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { tampilDialogHapus = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Data Konsumen",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = nama,
                onValueChange = {
                    nama = it
                    if (fieldError == "nama") fieldError = null
                },
                label = { Text("Nama") },
                singleLine = true,
                enabled = !sibuk,
                isError = fieldError == "nama",
                supportingText = if (fieldError == "nama") {
                    { Text("Wajib diisi") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fokusNama)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (fieldError == "email") fieldError = null
                },
                label = { Text("Email") },
                singleLine = true,
                enabled = !sibuk,
                isError = fieldError == "email",
                supportingText = if (fieldError == "email") {
                    { Text("Wajib diisi") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fokusEmail)
            )

            OutlinedTextField(
                value = telepon,
                onValueChange = {
                    telepon = it
                    if (fieldError == "telepon") fieldError = null
                },
                label = { Text("Telepon") },
                singleLine = true,
                enabled = !sibuk,
                isError = fieldError == "telepon",
                supportingText = if (fieldError == "telepon") {
                    { Text("Wajib diisi") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fokusTelepon)
            )

            OutlinedTextField(
                value = alamat,
                onValueChange = {
                    alamat = it
                    if (fieldError == "alamat") fieldError = null
                },
                label = { Text("Alamat") },
                singleLine = true,
                enabled = !sibuk,
                isError = fieldError == "alamat",
                supportingText = if (fieldError == "alamat") {
                    { Text("Wajib diisi") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fokusAlamat)
            )

            Button(
                onClick = { simpanData() },
                enabled = !sibuk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (idPilihan == null) "Tambah konsumen" else "Simpan perubahan")
            }

            OutlinedButton(
                onClick = { kosongkan() },
                enabled = !sibuk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kosongkan formulir")
            }

            OutlinedButton(
                onClick = { tampilDialogHapus = true },
                enabled = !sibuk && idPilihan != null,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hapus konsumen terpilih")
            }

            OutlinedButton(
                onClick = { muat() },
                enabled = !sibuk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Muat ulang daftar")
            }

            if (sibuk) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Daftar Konsumen",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (daftar.isEmpty() && !sibuk) {
                Text(
                    text = "Belum ada konsumen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                daftar.forEach { item ->
                    OutlinedCard(
                        onClick = { if (!sibuk) pilih(item) },
                        enabled = !sibuk,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "#${item.id} · ${item.nama}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = item.email,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseDaftar(data: JSONArray): List<Konsumen> =
    List(data.length()) { index ->
        val baris = data.getJSONObject(index)
        Konsumen(
            id = baris.getLong("id"),
            nama = baris.getString("nama"),
            email = baris.getString("email"),
            telepon = baris.getString("telepon"),
            alamat = baris.getString("alamat")
        )
    }
