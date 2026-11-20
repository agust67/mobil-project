package cat.casa.llancador

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle

/**
 * Control de trucades sortints.
 *
 * Android el consulta abans de fer qualsevol trucada, sigui des del Llançador
 * o des de l'app de Telèfon del sistema (per exemple, si s'hi arriba des del 112).
 * Si el número no és d'un contacte disponible ara, la trucada no es fa.
 * Les trucades d'emergència (112) no passen mai per aquí: sempre es poden fer.
 *
 * Perquè funcioni, el Llançador ha de tenir el paper de «redirecció de trucades»
 * (ROLE_CALL_REDIRECTION). El demana la pantalla d'inici la primera vegada.
 */
class ControlTrucadesSortints : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        if (Normes.numeroPermesAra(this, handle.schemeSpecificPart)) {
            placeCallUnmodified()
        } else {
            cancelCall()
        }
    }
}
