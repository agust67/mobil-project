package cat.casa.llancador

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla d'inici del mòbil.
 *
 * Fons de color, hora i data a dalt, i una graella d'icones amb les apps
 * permeses. De moment només hi ha Telèfon, que obre la llista de contactes.
 *
 * Com a Device Owner, aquesta pantalla queda fixada com a inici del mòbil.
 *
 * En versions de desenvolupament, a baix surt l'estat del Device Owner i un
 * botó per deixar de ser-ho (per provar a l'emulador sense resetejar-lo).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textEstat: TextView
    private lateinit var botoTreure: Button

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

        // A la pantalla d'inici, el botó Enrere no ha de fer res.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        // Cada vegada que es mostra l'inici, ens assegurem que segueix fixat.
        AdministradorDispositiu.fixarComInici(this)
        mostrarEstat()
    }

    /** En versions de desenvolupament, escriu si som Device Owner o no. */
    private fun mostrarEstat() {
        if (!esVersioDesenvolupament()) {
            textEstat.visibility = View.GONE
            botoTreure.visibility = View.GONE
            return
        }
        val somPropietaris = AdministradorDispositiu.somPropietaris(this)
        textEstat.visibility = View.VISIBLE
        textEstat.text = getString(
            if (somPropietaris) R.string.estat_propietari_si else R.string.estat_propietari_no
        )
        botoTreure.visibility = if (somPropietaris) View.VISIBLE else View.GONE
    }

    /** Només per a proves: deixa de ser Device Owner sense resetejar el mòbil. */
    @Suppress("DEPRECATION")
    private fun deixarDeSerPropietari() {
        AdministradorDispositiu.desfixarInici(this)
        getSystemService(DevicePolicyManager::class.java).clearDeviceOwnerApp(packageName)
        mostrarEstat()
    }

    /** Cert quan l'app s'ha instal·lat des d'Android Studio (versió de depuració). */
    private fun esVersioDesenvolupament(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
