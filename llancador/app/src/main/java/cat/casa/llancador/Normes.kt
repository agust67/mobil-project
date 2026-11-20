package cat.casa.llancador

import android.content.Context
import android.telephony.PhoneNumberUtils
import java.time.LocalTime

/**
 * Normes de qui pot trucar i quan. Les fan servir la pantalla d'inici,
 * la de contactes, el filtre de trucades entrants i el de sortints.
 */
object Normes {

    /** País per comparar números escrits amb o sense +34. */
    private const val PAIS = "es"

    /** Només per a proves: força el mode nit encara que no sigui l'hora. */
    var forcarModeNit = false

    /** Cert si ara mateix és l'horari del mode nit. */
    fun modeNitActiu(config: Configuracio, ara: LocalTime = LocalTime.now()): Boolean {
        if (forcarModeNit) return true
        val nit = config.modeNit ?: return false
        return if (nit.inici <= nit.fi) {
            ara >= nit.inici && ara < nit.fi
        } else {
            ara >= nit.inici || ara < nit.fi
        }
    }

    /** Contactes amb qui es pot parlar ara (al mode nit, només els grups permesos). */
    fun contactesDisponibles(config: Configuracio, ara: LocalTime = LocalTime.now()): List<Contacte> {
        val nit = config.modeNit
        if (!modeNitActiu(config, ara) || nit == null) return config.contactes
        return config.contactes.filter { it.grup in nit.grupsPermesos }
    }

    /** El contacte que té aquest número, si n'hi ha cap. */
    fun contacteDe(config: Configuracio, numero: String): Contacte? =
        config.contactes.firstOrNull { PhoneNumberUtils.areSamePhoneNumber(it.telefon, numero, PAIS) }

    /** Cert si ara mateix es pot parlar amb aquest número (entrant o sortint). */
    fun numeroPermesAra(context: Context, numero: String?): Boolean {
        if (numero.isNullOrBlank()) return false
        val config = Configuracio.llegir(context)
        val contacte = contacteDe(config, numero) ?: return false
        return contacte in contactesDisponibles(config)
    }
}
