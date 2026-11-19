package cat.casa.llancador

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils

/**
 * Filtre de trucades entrants.
 *
 * Android el consulta abans de fer sonar el mòbil. Si el número no és de cap
 * contacte permès (o és ocult), la trucada es penja sense sonar ni avisar.
 * Queda al registre de trucades, perquè des de casa es pugui veure qui ha trucat.
 *
 * Perquè funcioni, el Llançador ha de tenir el paper de «filtre de trucades»
 * (ROLE_CALL_SCREENING). El demana la pantalla d'inici la primera vegada.
 */
class FiltreTrucades : CallScreeningService() {

    override fun onScreenCall(detalls: Call.Details) {
        // Les trucades que fa el nen ja estan limitades pel Llançador: no les toquem.
        if (detalls.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(detalls, CallScreeningService.CallResponse.Builder().build())
            return
        }

        val numero = detalls.handle?.schemeSpecificPart
        val permesa = numero != null && Configuracio.llegir(this).contactes.any { contacte ->
            PhoneNumberUtils.areSamePhoneNumber(contacte.telefon, numero, PAIS)
        }

        val resposta = if (permesa) {
            CallScreeningService.CallResponse.Builder().build()
        } else {
            CallScreeningService.CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(false)
                .build()
        }
        respondToCall(detalls, resposta)
    }

    private companion object {
        /** País per comparar números escrits amb o sense +34. */
        const val PAIS = "es"
    }
}
