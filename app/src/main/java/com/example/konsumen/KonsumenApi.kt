package com.example.konsumen

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object KonsumenApi {
    // Public Tailscale Funnel URL (works on physical phones and emulator):
    private const val ENDPOINT =
        "https://desktop-jarkom.tail0244bb.ts.net/api/konsumen"
    // Fallback for local emulator-only testing without Tailscale:
    // private const val ENDPOINT = "http://10.0.2.2:10002/api/konsumen"

    fun request(
        method: String,
        id: Long? = null,
        body: JSONObject? = null
    ): JSONObject {
        val alamat = if (id == null) ENDPOINT else "$ENDPOINT?id=$id"
        val koneksi = URL(alamat).openConnection() as HttpURLConnection
        try {
            koneksi.requestMethod = method
            koneksi.connectTimeout = 8000
            koneksi.readTimeout = 8000
            koneksi.setRequestProperty("Accept", "application/json")
            if (body != null) {
                koneksi.doOutput = true
                koneksi.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )
                koneksi.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val status = koneksi.responseCode
            val sukses = status in 200..299
            val stream = if (sukses) koneksi.inputStream else koneksi.errorStream
            val teks = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }.orEmpty()
            if (!sukses) {
                val pesan = runCatching {
                    JSONObject(teks).getString("pesan")
                }.getOrDefault("Periksa alamat API dan log server.")
                throw IOException("HTTP $status. $pesan")
            }
            return JSONObject(teks)
        } finally {
            koneksi.disconnect()
        }
    }
}
