package cat.casa.llancador

import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
 * Fons de color, hora i data a dalt, i a baix les apps permeses per la
 * configuració (en files de quatre) i l'enllaç a «Com funciona el meu mòbil».
 * Al mode nit només queden les apps de nit. El 112 és a la pantalla Telèfon.
 *
 * Com a Device Owner, en mostrar-se aplica les normes del mòbil i, si cal,
 * demana els papers de filtre de trucades entrants i sortints. Quan els té,
 * entra en mode quiosc.
 *
 * En versions de desenvolupament, a baix surt l'estat i botons de proves.
 */
class MainActivity : AppCompatActivity() {

    /** Una app de la pantalla d'inici. */
    private class App(
        val etiqueta: Int,
        val icona: Int,
        val fons: Int,
        /** Si també surt al mode nit. */
        val deNit: Boolean,
        val obrir: () -> Unit
    )

    private lateinit var graellaApps: LinearLayout
    private lateinit var textModeNit: TextView
    private lateinit var textEstat: TextView
    private lateinit var grupProves: View
    private lateinit var botoTreure: Button
    private lateinit var botoModeNit: Button
    private lateinit var llanterna: Llanterna

    /** Icona de la llanterna a la graella (per pintar-la groga quan està encesa). */
    private var iconaLlanterna: ImageView? = null

    /** Totes les apps que sap obrir el Llançador. La configuració tria quines surten. */
    private val apps: Map<String, App> by lazy {
        mapOf(
            "telefon" to App(R.string.app_telefon, R.drawable.ic_telefon, R.drawable.fons_icona_telefon, true) {
                startActivity(Intent(this, ContactesActivity::class.java))
            },
            "llanterna" to App(R.string.app_llanterna, R.drawable.ic_llanterna, R.drawable.fons_icona_llanterna, true) {
                if (!llanterna.canviar()) {
                    Toast.makeText(this, R.string.sense_llanterna, Toast.LENGTH_SHORT).show()
                }
            },
            "rellotge" to App(R.string.app_rellotge, R.drawable.ic_rellotge, R.drawable.fons_icona_rellotge, true) {
                obrirApp(Intent(AlarmClock.ACTION_SHOW_ALARMS))
            },
            "calculadora" to App(R.string.app_calculadora, R.drawable.ic_calculadora, R.drawable.fons_icona_calculadora, false) {
                obrirApp(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))
            },
            "camera" to App(R.string.app_camera, R.drawable.ic_camera, R.drawable.fons_icona_camera, false) {
                obrirApp(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
        )
    }

    /** Papers que demanem una vegada per sessió, per ordre, si encara no els tenim. */
    private val rolsDemanats = mutableSetOf<String>()
    private val demanarRol =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mostrarEstat()
            demanarRolsSiCal()
        }

    /** Cada mig minut comprovem si ha començat o acabat el mode nit. */
    private val rellotge = Handler(Looper.getMainLooper())
    private val revisarModeNit = object : Runnable {
        override fun run() {
            mostrarApps()
            rellotge.postDelayed(this, 30_000)
        }
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

        graellaApps = findViewById(R.id.graella_apps)
        textModeNit = findViewById(R.id.text_mode_nit)
        textEstat = findViewById(R.id.text_estat)
        grupProves = findViewById(R.id.grup_proves)
        botoTreure = findViewById(R.id.boto_treure_propietari)
        botoModeNit = findViewById(R.id.boto_mode_nit)

        llanterna = Llanterna(this) { encesa -> pintarLlanterna(encesa) }

        findViewById<View>(R.id.enllac_com_funciona).setOnClickListener {
            startActivity(Intent(this, ComFuncionaActivity::class.java))
        }
        botoTreure.setOnClickListener { deixarDeSerPropietari() }
        // Proves: cada toc passa per segons l'horari → forçar nit → forçar dia → ...
        botoModeNit.setOnClickListener {
            Normes.forcarModeNit = when (Normes.forcarModeNit) {
                null -> true
                true -> false
                false -> null
            }
            mostrarApps()
            mostrarEstat()
        }

        // A la pantalla d'inici, el botó Enrere no ha de fer res.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    override fun onStart() {
        super.onStart()
        llanterna.escoltar()
        rellotge.post(revisarModeNit)
    }

    override fun onStop() {
        llanterna.deixarDEscoltar()
        rellotge.removeCallbacks(revisarModeNit)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Cada vegada que es mostra l'inici, ens assegurem que les normes segueixen aplicades.
        AdministradorDispositiu.aplicarNormes(this)
        demanarRolsSiCal()
        mostrarApps()
        mostrarEstat()
    }

    /** Dibuixa les apps permeses en files de quatre. Al mode nit, només les de nit. */
    private fun mostrarApps() {
        val config = Configuracio.llegir(this)
        val nit = Normes.modeNitActiu(config)
        textModeNit.visibility = if (nit) View.VISIBLE else View.GONE
        config.modeNit?.let { textModeNit.text = getString(R.string.mode_nit_fins, it.fi.toString()) }

        val visibles = config.apps.mapNotNull { clau -> apps[clau]?.let { clau to it } }
            .filter { (_, app) -> !nit || app.deNit }

        graellaApps.removeAllViews()
        iconaLlanterna = null
        val inflador = LayoutInflater.from(this)
        visibles.chunked(4).forEach { grup ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f
            }
            for ((clau, app) in grup) {
                val casella = inflador.inflate(R.layout.element_app, fila, false)
                val icona = casella.findViewById<ImageView>(R.id.icona_app)
                icona.setImageResource(app.icona)
                icona.setBackgroundResource(app.fons)
                casella.findViewById<TextView>(R.id.nom_app).setText(app.etiqueta)
                casella.setOnClickListener { app.obrir() }
                if (clau == "llanterna") iconaLlanterna = icona
                fila.addView(casella, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            graellaApps.addView(fila)
        }
        pintarLlanterna(llanterna.encesa)
    }

    /** Obre una app del sistema (rellotge, calculadora, càmera). */
    private fun obrirApp(intencio: Intent) {
        try {
            startActivity(intencio)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.app_no_disponible, Toast.LENGTH_SHORT).show()
        }
    }

    /** Icona de la llanterna: groga si està encesa, grisa si està apagada. */
    private fun pintarLlanterna(encesa: Boolean) {
        iconaLlanterna?.setBackgroundResource(
            if (encesa) R.drawable.fons_icona_llanterna_encesa else R.drawable.fons_icona_llanterna
        )
    }

    /**
     * Si som Device Owner i ens falta algun paper (filtre d'entrants, control de
     * sortints), el demanem. Surt un diàleg del sistema que cal acceptar una vegada.
     */
    private fun demanarRolsSiCal() {
        if (!AdministradorDispositiu.somPropietaris(this)) return
        val rols = getSystemService(RoleManager::class.java)
        val pendent = ROLS.firstOrNull {
            it !in rolsDemanats && rols.isRoleAvailable(it) && !rols.isRoleHeld(it)
        }
        if (pendent == null) {
            entrarModeQuiosc()
            return
        }
        rolsDemanats.add(pendent)
        demanarRol.launch(rols.createRequestRoleIntent(pendent))
    }

    /**
     * Mode quiosc: quan ja tenim els papers de trucades, fixem el Llançador i les
     * apps permeses. A partir d'aquí no es pot obrir cap altra app (ni Ajustos).
     */
    private fun entrarModeQuiosc() {
        if (!AdministradorDispositiu.somPropietaris(this)) return
        if (AdministradorDispositiu.enModeQuiosc(this)) return
        if (getSystemService(DevicePolicyManager::class.java).isLockTaskPermitted(packageName)) {
            startLockTask()
        }
    }

    private fun tenimRol(rol: String): Boolean =
        getSystemService(RoleManager::class.java).isRoleHeld(rol)

    /** En versions de desenvolupament, escriu l'estat i mostra els botons de proves. */
    private fun mostrarEstat() {
        if (!AdministradorDispositiu.esVersioDesenvolupament(this)) {
            grupProves.visibility = View.GONE
            return
        }
        grupProves.visibility = View.VISIBLE
        val somPropietaris = AdministradorDispositiu.somPropietaris(this)
        textEstat.text = getString(
            R.string.estat_proves,
            getString(if (somPropietaris) R.string.estat_propietari_si else R.string.estat_propietari_no),
            getString(
                if (tenimRol(RoleManager.ROLE_CALL_SCREENING)) R.string.estat_filtre_si
                else R.string.estat_filtre_no
            ),
            getString(
                if (tenimRol(RoleManager.ROLE_CALL_REDIRECTION)) R.string.estat_sortints_si
                else R.string.estat_sortints_no
            ),
            getString(
                if (AdministradorDispositiu.enModeQuiosc(this)) R.string.estat_quiosc_si
                else R.string.estat_quiosc_no
            )
        )
        botoTreure.visibility = if (somPropietaris) View.VISIBLE else View.GONE
        botoModeNit.setText(
            when (Normes.forcarModeNit) {
                null -> R.string.boto_mode_horari
                true -> R.string.boto_mode_nit_forcat
                false -> R.string.boto_mode_dia_forcat
            }
        )
    }

    /** Només per a proves: desfà les normes i deixa de ser Device Owner sense resetejar el mòbil. */
    @Suppress("DEPRECATION")
    private fun deixarDeSerPropietari() {
        if (AdministradorDispositiu.enModeQuiosc(this)) stopLockTask()
        AdministradorDispositiu.treureNormes(this)
        getSystemService(DevicePolicyManager::class.java).clearDeviceOwnerApp(packageName)
        mostrarEstat()
    }

    private companion object {
        val ROLS = listOf(RoleManager.ROLE_CALL_SCREENING, RoleManager.ROLE_CALL_REDIRECTION)
    }
}
