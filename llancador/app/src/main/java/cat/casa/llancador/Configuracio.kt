package cat.casa.llancador

import android.content.Context
import org.json.JSONObject

/** Un contacte al qual el nen pot trucar. */
data class Contacte(val nom: String, val telefon: String)

/**
 * Configuració del llançador.
 *
 * De moment es llegeix del fitxer `assets/configuracio.json` que va dins de l'app.
 * Més endavant vindrà del servidor del homelab.
 */
data class Configuracio(val contactes: List<Contacte>) {

    companion object {
        private const val FITXER = "configuracio.json"

        /** Llegeix la configuració. Si el fitxer falla, torna una configuració buida. */
        fun llegir(context: Context): Configuracio = try {
            val text = context.assets.open(FITXER).bufferedReader().use { it.readText() }
            desdeJson(text)
        } catch (e: Exception) {
            Configuracio(contactes = emptyList())
        }

        /** Converteix el text JSON en una configuració. */
        fun desdeJson(text: String): Configuracio {
            val arrel = JSONObject(text)
            val llista = arrel.optJSONArray("contactes")
            val contactes = buildList {
                if (llista != null) {
                    for (i in 0 until llista.length()) {
                        val element = llista.getJSONObject(i)
                        add(Contacte(nom = element.getString("nom"), telefon = element.getString("telefon")))
                    }
                }
            }
            return Configuracio(contactes)
        }
    }
}
