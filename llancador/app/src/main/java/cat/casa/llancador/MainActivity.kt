package cat.casa.llancador

import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla d'inici del mòbil.
 *
 * Fons de color, hora i data a dalt, i una graella d'icones amb les apps
 * permeses: Telèfon (llista de contactes) i Llanterna.
 *
 * Com a Device Owner, en mostrar-se aplica les normes del mòbil (inici fixat,
 * panell de dalt limitat) i, si cal, demana el paper de filtre de trucades.
 *
 * En versions de desenvolupament, a baix surt l'estat i un botó per deixar
 * de ser Device Owner (per provar a l'emulador sense resetejar-lo).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textEstat: TextView
    private lateinit var botoTreure: Button
    private lateinit var iconaLlanterna: ImageView
    private lateinit var llanterna: Llanterna

    /** Per no tornar a demanar el filtre de trucades si ja s'ha dit que no en aquesta sessió. */
    private var filtreDemanat = false

    private val demanarFiltre =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mostrarEstat()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barres del sistema transparents i amb icones clares, sobre el fons de color.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barres = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barres.left, barres.top, barres.right, barres.bottom)
            insets
        }

        textEstat = findViewById(R.id.text_estat)
        botoTreure = findViewById(R.id.boto_treure_propietari)
        botoTreure.setOnClickListener { deixarDeSerPropietari() }

        findViewById<View>(R.id.app_telefon).setOnClickListener {
            startActivity(Intent(this, ContactesActivity::class.java))
        }

        iconaLlanterna = findViewById(R.id.icona_llanterna)
        llanterna = Llanterna(this) { encesa -> pintarLlanterna(encesa) }
        findViewById<View>(R.id.app_llanterna).setOnClickListener {
            if (!llanterna.canviar()) {
                Toast.makeText(this, R.string.sense_llanterna, Toast.LENGTH_SHORT).show()
            }
        }

        // A la pantalla d'inici, el botó Enrere no ha de fer res.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    override fun onStart() {
        super.onStart()
        llanterna.escoltar()
        pintarLlanterna(llanterna.encesa)
    }

    override fun onStop() {
        llanterna.deixarDEscoltar()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Cada vegada que es mostra l'inici, ens assegurem que les normes segueixen aplicades.
        AdministradorDispositiu.aplicarNormes(this)
        demanarFiltreSiCal()
        mostrarEstat()
    }

    /** Icona de la llanterna: groga si està encesa, grisa si està apagada. */
    private fun pintarLlanterna(encesa: Boolean) {
        iconaLlanterna.setBackgroundResource(
            if (encesa) R.drawable.fons_icona_llanterna_encesa else R.drawable.fons_icona_llanterna
        )
    }

    /** Cert si el Llançador ja és el filtre de trucades del mòbil. */
    private fun somFiltreDeTrucades(): Boolean =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    /**
     * Si som Device Owner i encara no som el filtre de trucades, el demanem.
     * Surt un diàleg del sistema que s'ha d'acceptar una vegada, en configurar el mòbil.
     */
    private fun demanarFiltreSiCal() {
        if (filtreDemanat || !AdministradorDispositiu.somPropietaris(this)) return
        val rols = getSystemService(RoleManager::class.java)
        if (!rols.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) || somFiltreDeTrucades()) return
        filtreDemanat = true
        demanarFiltre.launch(rols.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    /** En versions de desenvolupament, escriu l'estat del Device Owner i del filtre. */
    private fun mostrarEstat() {
        if (!esVersioDesenvolupament()) {
            textEstat.visibility = View.GONE
            botoTreure.visibility = View.GONE
            return
        }
        val somPropietaris = AdministradorDispositiu.somPropietaris(this)
        textEstat.visibility = View.VISIBLE
        textEstat.text = getString(
            R.string.estat_proves,
            getString(if (somPropietaris) R.string.estat_propietari_si else R.string.estat_propietari_no),
            getString(if (somFiltreDeTrucades()) R.string.estat_filtre_si else R.string.estat_filtre_no)
        )
        botoTreure.visibility = if (somPropietaris) View.VISIBLE else View.GONE
    }

    /** Només per a proves: desfà les normes i deixa de ser Device Owner sense resetejar el mòbil. */
    @Suppress("DEPRECATION")
    private fun deixarDeSerPropietari() {
        AdministradorDispositiu.treureNormes(this)
        getSystemService(DevicePolicyManager::class.java).clearDeviceOwnerApp(packageName)
        mostrarEstat()
    }

    /** Cert quan l'app s'ha instal·lat des d'Android Studio (versió de depuració). */
    private fun esVersioDesenvolupament(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
