package cat.casa.llancador

import android.app.admin.DevicePolicyManager
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla principal del llançador.
 *
 * En aquesta primera versió només mostra si l'app és Device Owner, i en
 * versions de desenvolupament ofereix un botó per deixar de ser-ho
 * (així es pot provar a l'emulador sense haver-lo de resetejar).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gestorPolitiques: DevicePolicyManager
    private lateinit var textEstat: TextView
    private lateinit var botoTreure: Button

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
        textEstat = findViewById(R.id.text_estat)
        botoTreure = findViewById(R.id.boto_treure_propietari)

        botoTreure.setOnClickListener { deixarDeSerPropietari() }
    }

    override fun onResume() {
        super.onResume()
        mostrarEstat()
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
