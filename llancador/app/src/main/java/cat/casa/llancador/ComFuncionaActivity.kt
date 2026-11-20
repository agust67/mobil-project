package cat.casa.llancador

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * «Com funciona el meu mòbil»: explica al nen, en llenguatge clar,
 * les normes del mòbil. Part del text surt de la configuració
 * (contactes, horari de nit), així sempre està al dia.
 */
class ComFuncionaActivity : AppCompatActivity() {

    private lateinit var contingut: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_com_funciona)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.com_funciona)) { vista, insets ->
            val barres = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barres.left, barres.top, barres.right, barres.bottom)
            insets
        }
        contingut = findViewById(R.id.contingut_com_funciona)

        val config = Configuracio.llegir(this)
        val noms = config.contactes.joinToString(", ") { it.nom }

        afegirApartat(R.string.cf_trucades_titol, getString(R.string.cf_trucades_text, noms))
        config.modeNit?.let { nit ->
            val nomsNit = config.contactes.filter { it.grup in nit.grupsPermesos }.joinToString(", ") { it.nom }
            afegirApartat(
                R.string.cf_nit_titol,
                getString(R.string.cf_nit_text, nit.inici.toString(), nit.fi.toString(), nomsNit)
            )
        }
        afegirApartat(R.string.cf_apps_titol, getString(R.string.cf_apps_text))
        afegirApartat(R.string.cf_emergencia_titol, getString(R.string.cf_emergencia_text))
        afegirApartat(R.string.cf_ubicacio_titol, getString(R.string.cf_ubicacio_text))
        afegirApartat(R.string.cf_normes_titol, getString(R.string.cf_normes_text))
    }

    private fun afegirApartat(titol: Int, text: String) {
        val densitat = resources.displayMetrics.density
        contingut.addView(TextView(this).apply {
            setText(titol)
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (24 * densitat).toInt(), 0, (6 * densitat).toInt())
        })
        contingut.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.rgb(0xDD, 0xDD, 0xE4))
            textSize = 17f
            setLineSpacing(0f, 1.2f)
        })
    }
}
