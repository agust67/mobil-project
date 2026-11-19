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
 * Aquí també hi ha les normes que el Llançador aplica al mòbil com a
 * Device Owner, i les funcions per donar-nos permisos sense preguntar al nen.
 */
class AdministradorDispositiu : DeviceAdminReceiver() {

    /** Quan l'app passa a ser administradora (per exemple amb `dpm set-device-owner`). */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        aplicarNormes(context)
    }

    companion object {
        /** Identificador d'aquest receptor, que demanen les crides de DevicePolicyManager. */
        fun component(context: Context): ComponentName =
            ComponentName(context.applicationContext, AdministradorDispositiu::class.java)

        private fun gestor(context: Context): DevicePolicyManager =
            context.getSystemService(DevicePolicyManager::class.java)

        /** Cert si aquesta app és el Device Owner del mòbil. */
        fun somPropietaris(context: Context): Boolean =
            gestor(context).isDeviceOwnerApp(context.packageName)

        /**
         * Si som Device Owner, aplica les normes del mòbil:
         * - el Llançador és sempre la pantalla d'inici (no es pot canviar des d'Ajustos);
         * - el panell de dalt (notificacions i ajustos ràpids) no es pot obrir.
         *
         * Es pot cridar tantes vegades com calgui. Torna cert si s'han aplicat.
         */
        fun aplicarNormes(context: Context): Boolean {
            if (!somPropietaris(context)) return false
            val gestor = gestor(context)
            val admin = component(context)

            val filtreInici = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            gestor.addPersistentPreferredActivity(
                admin,
                filtreInici,
                ComponentName(context.applicationContext, MainActivity::class.java)
            )

            gestor.setStatusBarDisabled(admin, true)
            return true
        }

        /** Desfà les normes (només per a proves, abans de deixar de ser propietaris). */
        fun treureNormes(context: Context) {
            if (!somPropietaris(context)) return
            val gestor = gestor(context)
            val admin = component(context)
            gestor.clearPackagePersistentPreferredActivities(admin, context.packageName)
            gestor.setStatusBarDisabled(admin, false)
        }

        /**
         * Si som Device Owner, ens concedim el permís indicat nosaltres mateixos.
         * Torna cert si ho hem aconseguit.
         */
        fun concedirPermis(context: Context, permis: String): Boolean {
            if (!somPropietaris(context)) return false
            return gestor(context).setPermissionGrantState(
                component(context),
                context.packageName,
                permis,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        }
    }
}
