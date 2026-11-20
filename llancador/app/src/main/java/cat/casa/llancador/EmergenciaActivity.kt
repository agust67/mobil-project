package cat.casa.llancador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Finestra transparent que només mostra «Vols trucar al 112?».
 * L'obre el botó 112 de la pantalla Telèfon.
 */
class EmergenciaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle(R.string.emergencia_titol)
            .setMessage(R.string.emergencia_pregunta)
            .setPositiveButton(R.string.emergencia_trucar) { _, _ -> trucar112() }
            .setNegativeButton(R.string.emergencia_cancellar, null)
            .setOnDismissListener { finish() }
            .show()
    }

    /** Truca al 112. Si no tenim permís de trucar, obre el marcador amb el 112 posat. */
    private fun trucar112() {
        val numero = Uri.parse("tel:112")
        val potTrucar = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED ||
            AdministradorDispositiu.concedirPermis(this, Manifest.permission.CALL_PHONE)
        try {
            startActivity(Intent(if (potTrucar) Intent.ACTION_CALL else Intent.ACTION_DIAL, numero))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_DIAL, numero))
        }
    }
}
