package cat.casa.llancador

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Filtre de trucades entrants.
 *
 * Android el consulta abans de fer sonar el mòbil. Si ara no es pot parlar amb
 * aquest número (no és de cap contacte, és ocult, o és mode nit i el contacte
 * no hi està permès), la trucada es penja sense sonar ni avisar.
 * Queda al registre de trucades, perquè des de casa es pugui veure qui ha trucat.
 *
 * Perquè funcioni, el Llançador ha de tenir el paper de «filtre de trucades»
 * (ROLE_CALL_SCREENING). El demana la pantalla d'inici la primera vegada.
 */
class FiltreTrucades : CallScreeningService() {

    override fun onScreenCall(detalls: Call.Details) {
        // Les sortints les controla ControlTrucadesSortints.
        if (detalls.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(detalls, CallScreeningService.CallResponse.Builder().build())
            return
        }

        val numero = detalls.handle?.schemeSpecificPart
        val resposta = if (Normes.numeroPermesAra(this, numero)) {
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
}
