package cat.casa.llancador

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Receptor d'administrador del dispositiu.
 *
 * Android exigeix aquesta classe per poder convertir l'app en Device Owner.
 * Aquí també hi ha les funcions que fan servir les pantalles per saber
 * si som Device Owner i per donar-nos permisos sense preguntar al nen.
 */
class AdministradorDispositiu : DeviceAdminReceiver() {

    /** Quan l'app passa a ser administradora (per exemple amb `dpm set-device-owner`). */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        fixarComInici(context)
    }

    companion object {
        /** Identificador d'aquest receptor, que demanen les crides de DevicePolicyManager. */
        fun component(context: Context): ComponentName =
            ComponentName(context.applicationContext, AdministradorDispositiu::class.java)

        /** Cert si aquesta app és el Device Owner del mòbil. */
        fun somPropietaris(context: Context): Boolean =
            context.getSystemService(DevicePolicyManager::class.java)
                .isDeviceOwnerApp(context.packageName)

        /**
         * Si som Device Owner, fa que el Llançador sigui sempre la pantalla d'inici.
         * El nen no ho pot canviar des d'Ajustos. Torna cert si ho hem fet.
         */
        fun fixarComInici(context: Context): Boolean {
            if (!somPropietaris(context)) return false
            val filtre = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            context.getSystemService(DevicePolicyManager::class.java).addPersistentPreferredActivity(
                component(context),
                filtre,
                ComponentName(context.applicationContext, MainActivity::class.java)
            )
            return true
        }

        /** Treu la pantalla d'inici fixada (només per a proves, abans de deixar de ser propietaris). */
        fun desfixarInici(context: Context) {
            if (!somPropietaris(context)) return
            context.getSystemService(DevicePolicyManager::class.java)
                .clearPackagePersistentPreferredActivities(component(context), context.packageName)
        }

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
