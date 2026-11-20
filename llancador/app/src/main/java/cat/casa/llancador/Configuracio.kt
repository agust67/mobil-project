package cat.casa.llancador

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/** Un contacte al qual el nen pot trucar. El grup serveix per al mode nit. */
data class Contacte(val nom: String, val telefon: String, val grup: String = "familia")

/**
 * Horari del mode nit. Si l'inici és més tard que la fi (21:00 → 07:00),
 * el mode nit passa per mitjanit.
 */
data class HorariNit(val inici: LocalTime, val fi: LocalTime, val grupsPermesos: Set<String>)

/**
 * Configuració del llançador.
 *
 * De moment es llegeix del fitxer `assets/configuracio.json` que va dins de l'app.
 * Més endavant vindrà del servidor del homelab (i es podrà canviar des de HA).
 */
data class Configuracio(
    val contactes: List<Contacte>,
    /** Apps que surten a l'inici, per ordre: telefon, llanterna, rellotge, calculadora, camera. */
    val apps: List<String>,
    /** Mode nit, o null si no n'hi ha. */
    val modeNit: HorariNit?
) {

    companion object {
        private const val FITXER = "configuracio.json"

        /** Apps per defecte si la configuració no en diu res. */
        private val APPS_PER_DEFECTE = listOf("telefon", "llanterna")

        /** Llegeix la configuració. Si el fitxer falla, torna una configuració buida. */
        fun llegir(context: Context): Configuracio = try {
            val text = context.assets.open(FITXER).bufferedReader().use { it.readText() }
            desdeJson(text)
        } catch (e: Exception) {
            Configuracio(contactes = emptyList(), apps = APPS_PER_DEFECTE, modeNit = null)
        }

        /** Converteix el text JSON en una configuració. */
        fun desdeJson(text: String): Configuracio {
            val arrel = JSONObject(text)

            val contactes = buildList {
                val llista = arrel.optJSONArray("contactes")
                if (llista != null) {
                    for (i in 0 until llista.length()) {
                        val element = llista.getJSONObject(i)
                        add(
                            Contacte(
                                nom = element.getString("nom"),
                                telefon = element.getString("telefon"),
                                grup = element.optString("grup", "familia")
                            )
                        )
                    }
                }
            }

            val apps = arrel.optJSONArray("apps")?.let { textos(it) } ?: APPS_PER_DEFECTE

            val modeNit = arrel.optJSONObject("mode_nit")?.let { nit ->
                HorariNit(
                    inici = LocalTime.parse(nit.getString("inici")),
                    fi = LocalTime.parse(nit.getString("fi")),
                    grupsPermesos = nit.optJSONArray("grups_permesos")?.let { textos(it).toSet() }
                        ?: setOf("familia")
                )
            }

            return Configuracio(contactes, apps, modeNit)
        }

        private fun textos(llista: JSONArray): List<String> =
            (0 until llista.length()).map { llista.getString(it) }
    }
}
