package cat.casa.llancador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla Telèfon: la llista de contactes permesos.
 *
 * Cada fila té un cercle amb la inicial, el nom i, a la dreta, un botó per
 * trucar i un per escriure (el xat encara no està fet). Al mode nit només
 * surten els contactes dels grups permesos de nit. Es truca directament,
 * sense passar per l'app de Telèfon, perquè no es pugui marcar cap altre número.
 */
class ContactesActivity : AppCompatActivity() {

    private lateinit var llista: LinearLayout

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_contactes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contactes)) { vista, insets ->
            val barres = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barres.left, barres.top, barres.right, barres.bottom)
            insets
        }

        llista = findViewById(R.id.llista_contactes)
    }

    override fun onResume() {
        super.onResume()
        // Al mode nit només surten els contactes permesos de nit.
        mostrarContactes(Normes.contactesDisponibles(Configuracio.llegir(this)))
    }

    /** Crea una fila per a cada contacte permès. */
    private fun mostrarContactes(contactes: List<Contacte>) {
        llista.removeAllViews()
        if (contactes.isEmpty()) {
            val avis = TextView(this)
            avis.setText(R.string.sense_contactes)
            avis.setTextColor(Color.WHITE)
            avis.textSize = 18f
            llista.addView(avis)
            return
        }
        val inflador = LayoutInflater.from(this)
        contactes.forEachIndexed { posicio, contacte ->
            val fila = inflador.inflate(R.layout.element_contacte, llista, false)

            val inicial = fila.findViewById<TextView>(R.id.inicial_contacte)
            inicial.text = contacte.nom.take(1).uppercase()
            inicial.background = cercle(COLORS_INICIALS[posicio % COLORS_INICIALS.size])

            fila.findViewById<TextView>(R.id.nom_contacte).text = contacte.nom

            val botoTrucar = fila.findViewById<ImageButton>(R.id.boto_trucar)
            botoTrucar.contentDescription = getString(R.string.boto_trucar, contacte.nom)
            botoTrucar.setOnClickListener { trucar(contacte) }

            val botoXat = fila.findViewById<ImageButton>(R.id.boto_xat)
            botoXat.contentDescription = getString(R.string.boto_xat, contacte.nom)
            botoXat.setOnClickListener {
                Toast.makeText(this, R.string.xat_no_disponible, Toast.LENGTH_SHORT).show()
            }

            llista.addView(fila)
        }
    }

    /** Un cercle del color indicat, per al fons de la inicial. */
    private fun cercle(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    /** Truca al contacte, demanant abans el permís si encara no el tenim. */
    private fun trucar(contacte: Contacte) {
        if (tePermisTrucar() ||
            AdministradorDispositiu.concedirPermis(this, Manifest.permission.CALL_PHONE)
        ) {
            ferTrucada(contacte)
        } else {
            contactePendent = contacte
            demanarPermisTrucar.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun tePermisTrucar(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /** Fa la trucada directament. Cal tenir el permís CALL_PHONE. */
    private fun ferTrucada(contacte: Contacte) {
        val intencio = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacte.telefon}"))
        try {
            startActivity(intencio)
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.sense_permis_trucar, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        /** Colors per als cercles de les inicials, per torns. */
        val COLORS_INICIALS = intArrayOf(
            Color.rgb(0xE9, 0x1E, 0x63), // magenta
            Color.rgb(0x00, 0x96, 0xC7), // blau
            Color.rgb(0xF5, 0x7C, 0x00), // taronja
            Color.rgb(0x7C, 0x3A, 0xED), // lila
            Color.rgb(0x00, 0x9E, 0x73)  // verd
        )
    }
}
