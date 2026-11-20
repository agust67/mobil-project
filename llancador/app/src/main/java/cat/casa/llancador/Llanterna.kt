package cat.casa.llancador

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * Encén i apaga la llanterna (el flaix de la càmera del darrere).
 *
 * No cal cap permís. Avisa amb [quanCanvia] cada vegada que la llanterna
 * s'encén o s'apaga, també si ho fa una altra app o el sistema.
 */
class Llanterna(context: Context, private val quanCanvia: (encesa: Boolean) -> Unit) {

    private val cameres = context.getSystemService(CameraManager::class.java)

    /** Identificador de la càmera amb flaix, o null si el mòbil no en té (com l'emulador). */
    private val idCamera: String? = buscarCameraAmbFlaix()

    var encesa = false
        private set

    val disponible: Boolean get() = idCamera != null

    private val avisos = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == idCamera) {
                encesa = enabled
                quanCanvia(enabled)
            }
        }
    }

    /** Comença a escoltar els canvis. Cridar-la a onStart. */
    fun escoltar() {
        if (disponible) cameres.registerTorchCallback(avisos, Handler(Looper.getMainLooper()))
    }

    /** Deixa d'escoltar els canvis. Cridar-la a onStop. */
    fun deixarDEscoltar() {
        if (disponible) cameres.unregisterTorchCallback(avisos)
    }

    /** Si està apagada l'encén, i si està encesa l'apaga. Torna fals si no s'ha pogut. */
    fun canviar(): Boolean {
        val id = idCamera ?: return false
        return try {
            cameres.setTorchMode(id, !encesa)
            true
        } catch (e: CameraAccessException) {
            false
        }
    }

    private fun buscarCameraAmbFlaix(): String? = try {
        cameres.cameraIdList.firstOrNull { id ->
            val dades = cameres.getCameraCharacteristics(id)
            dades.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                dades.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: CameraAccessException) {
        null
    }
}
