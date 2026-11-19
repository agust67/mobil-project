package cat.casa.llancador

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/**
 * Receptor d'administrador del dispositiu.
 *
 * Android exigeix aquesta classe per poder convertir l'app en Device Owner.
 * Aquí també hi ha les funcions que fan servir les pantalles per saber
 * si som Device Owner i per donar-nos permisos sense preguntar al nen.
 */
class AdministradorDispositiu : DeviceAdminReceiver() {

    companion object {
        /** Identificador d'aquest receptor, que demanen les crides de DevicePolicyManager. */
        fun component(context: Context): ComponentName =
            ComponentName(context.applicationContext, AdministradorDispositiu::class.java)

        /** Cert si aquesta app és el Device Owner del mòbil. */
        fun somPropietaris(context: Context): Boolean =
            context.getSystemService(DevicePolicyManager::class.java)
                .isDeviceOwnerApp(context.packageName)

        /**
         * Si som Device Owner, ens concedim el permís indicat nosaltres mateixos.
         * Torna cert si ho hem aconseguit.
         */
        fun concedirPermis(context: Context, permis: String): Boolean {
            if (!somPropietaris(context)) return false
            return context.getSystemService(DevicePolicyManager::class.java).setPermissionGrantState(
                component(context),
                context.packageName,
                permis,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        }
    }
}
