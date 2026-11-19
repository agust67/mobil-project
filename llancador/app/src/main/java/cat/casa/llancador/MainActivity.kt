package cat.casa.llancador

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla principal del llançador.
 *
 * Mostra un botó per a cada contacte permès de la configuració. En prémer-lo,
 * truca directament, sense passar per l'app de Telèfon (així no es pot marcar
 * cap altre número).
 *
 * A baix hi ha l'estat del Device Owner i, en versions de desenvolupament,
 * un botó per deixar de ser-ho (per provar a l'emulador sense resetejar-lo).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gestorPolitiques: DevicePolicyManager
    private lateinit var contenidorContactes: LinearLayout
    private lateinit var textEstat: TextView
    private lateinit var botoTreure: Button

    /** Contacte que s'esperava trucar mentre es demanava el permís. */
    private var contactePendent: Contacte? = null

    /** Demana el permís de trucar i, si el dona, fa la trucada pendent. */
    private val demanarPermisTrucar =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedit ->
            val contacte = contactePendent
            contactePendent = null
            if (concedit && contacte != null) {
                ferTrucada(contacte)
            } else if (!concedit) {
                Toast.makeText(this, R.string.sense_permis_trucar, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barres = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barres.left, barres.top, barres.right, barres.bottom)
            insets
        }

        gestorPolitiques = getSystemService(DevicePolicyManager::class.java)
        contenidorContactes = findViewById(R.id.contenidor_contactes)
        textEstat = findViewById(R.id.text_estat)
        botoTreure = findViewById(R.id.boto_treure_propietari)

        botoTreure.setOnClickListener { deixarDeSerPropietari() }

        mostrarContactes(Configuracio.llegir(this).contactes)
    }

    override fun onResume() {
        super.onResume()
        mostrarEstat()
    }

    /** Crea un botó per a cada contacte permès. */
    private fun mostrarContactes(contactes: List<Contacte>) {
        contenidorContactes.removeAllViews()
        if (contactes.isEmpty()) {
            val avis = TextView(this)
            avis.setText(R.string.sense_contactes)
            contenidorContactes.addView(avis)
            return
        }
        for (contacte in contactes) {
            val boto = Button(this)
            boto.text = getString(R.string.boto_trucar, contacte.nom)
            boto.textSize = 20f
            boto.setOnClickListener { trucar(contacte) }
            val mides = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            mides.topMargin = resources.getDimensionPixelSize(R.dimen.separacio_botons)
            contenidorContactes.addView(boto, mides)
        }
    }

    /** Truca al contacte, demanant abans el permís si encara no el tenim. */
    private fun trucar(contacte: Contacte) {
        if (tePermisTrucar() || concedirPermisComAPropietari()) {
            ferTrucada(contacte)
        } else {
            contactePendent = contacte
            demanarPermisTrucar.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun tePermisTrucar(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Si som Device Owner, ens donem el permís de trucar nosaltres mateixos,
     * sense preguntar res al nen. Torna cert si ho hem aconseguit.
     */
    private fun concedirPermisComAPropietari(): Boolean {
        if (!gestorPolitiques.isDeviceOwnerApp(packageName)) return false
        return gestorPolitiques.setPermissionGrantState(
            AdministradorDispositiu.component(this),
            packageName,
            Manifest.permission.CALL_PHONE,
            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
        )
    }

    /** Fa la trucada directament. Cal tenir el permís CALL_PHONE. */
    private fun ferTrucada(contacte: Contacte) {
        val intencio = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacte.telefon}"))
        try {
            startActivity(intencio)
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.sense_permis_trucar, Toast.LENGTH_LONG).show()
        }
    }

    /** Escriu a la pantalla si som Device Owner o no. */
    private fun mostrarEstat() {
        val somPropietari = gestorPolitiques.isDeviceOwnerApp(packageName)
        textEstat.text = getString(
            if (somPropietari) R.string.estat_propietari_si else R.string.estat_propietari_no
        )
        // El botó de treure només surt en versions de desenvolupament i si som propietaris.
        botoTreure.visibility =
            if (somPropietari && esVersioDesenvolupament()) View.VISIBLE else View.GONE
    }

    /** Només per a proves: deixa de ser Device Owner sense resetejar el mòbil. */
    @Suppress("DEPRECATION")
    private fun deixarDeSerPropietari() {
        gestorPolitiques.clearDeviceOwnerApp(packageName)
        mostrarEstat()
    }

    /** Cert quan l'app s'ha instal·lat des d'Android Studio (versió de depuració). */
    private fun esVersioDesenvolupament(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
