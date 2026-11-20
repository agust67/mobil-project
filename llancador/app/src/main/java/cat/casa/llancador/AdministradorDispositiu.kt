package cat.casa.llancador

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.UserManager

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

        /** Cert quan l'app s'ha instal·lat des d'Android Studio (versió de depuració). */
        fun esVersioDesenvolupament(context: Context): Boolean =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        /**
         * Si som Device Owner, aplica les normes del mòbil:
         * - el Llançador és sempre la pantalla d'inici (no es pot canviar);
         * - el panell de dalt queda obert (perquè es vegin les trucades), però no s'hi pot
         *   canviar la wifi, les dades mòbils, el Bluetooth ni el mode avió;
         * - l'app d'Ajustos queda bloquejada (la roda del panell mostra un avís);
         * - l'hora és automàtica i no es pot canviar (perquè el mode nit no es pugui saltar);
         * - no es pot fer reset de fàbrica des d'Ajustos, ni afegir usuaris, ni arrencar en
         *   mode segur, ni fer servir memòries USB;
         * - a la versió definitiva, tampoc es poden instal·lar ni treure apps, ni activar
         *   les opcions de desenvolupador (a la de proves no, perquè Android Studio pugui instal·lar).
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

            for (restriccio in restriccions(context)) gestor.addUserRestriction(admin, restriccio)
            gestor.setAutoTimeEnabled(admin, true)
            // Ajustos: primer provem d'amagar-los; si el sistema no ho permet, els suspenem
            // (en obrir-los surt un avís en lloc de l'app).
            gestor.setShortSupportMessage(admin, context.getString(R.string.avis_bloquejat))
            for (paquet in APPS_BLOQUEJADES) gestor.setApplicationHidden(admin, paquet, true)
            appsSenseBloquejar = gestor.setPackagesSuspended(admin, APPS_BLOQUEJADES.toTypedArray(), true)
                .toList()
            return true
        }

        /** Desfà les normes (només per a proves, abans de deixar de ser propietaris). */
        fun treureNormes(context: Context) {
            if (!somPropietaris(context)) return
            val gestor = gestor(context)
            val admin = component(context)
            gestor.clearPackagePersistentPreferredActivities(admin, context.packageName)
            for (restriccio in RESTRICCIONS + RESTRICCIONS_DEFINITIVA) {
                gestor.clearUserRestriction(admin, restriccio)
            }
            gestor.setPackagesSuspended(admin, APPS_BLOQUEJADES.toTypedArray(), false)
            for (paquet in APPS_BLOQUEJADES) gestor.setApplicationHidden(admin, paquet, false)
            gestor.setShortSupportMessage(admin, null)
        }

        /** Apps que no s'han pogut bloquejar l'última vegada (per a la línia de proves). */
        var appsSenseBloquejar: List<String> = emptyList()
            private set

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

        private fun restriccions(context: Context): List<String> =
            if (esVersioDesenvolupament(context)) RESTRICCIONS
            else RESTRICCIONS + RESTRICCIONS_DEFINITIVA

        /** Coses que el nen no pot canviar, ni des del panell de dalt ni des d'Ajustos. */
        private val RESTRICCIONS = listOf(
            UserManager.DISALLOW_CONFIG_WIFI,             // configurar xarxes wifi
            UserManager.DISALLOW_CHANGE_WIFI_STATE,       // encendre o apagar la wifi
            UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,  // dades mòbils i xarxa
            UserManager.DISALLOW_CONFIG_BLUETOOTH,        // Bluetooth
            UserManager.DISALLOW_AIRPLANE_MODE,           // mode avió
            UserManager.DISALLOW_CONFIG_DATE_TIME,        // canviar l'hora
            UserManager.DISALLOW_FACTORY_RESET,           // reset de fàbrica des d'Ajustos
            UserManager.DISALLOW_ADD_USER,                // afegir usuaris
            UserManager.DISALLOW_SAFE_BOOT,               // arrencar en mode segur
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,    // memòries USB o targetes
            UserManager.DISALLOW_USB_FILE_TRANSFER,       // passar fitxers per USB
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY // instal·lar APK descarregats
        )

        /** Només a la versió definitiva: a la de proves impedirien instal·lar des d'Android Studio. */
        private val RESTRICCIONS_DEFINITIVA = listOf(
            UserManager.DISALLOW_INSTALL_APPS,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_DEBUGGING_FEATURES
        )

        /** Apps del sistema que el nen no ha de poder obrir. */
        private val APPS_BLOQUEJADES = listOf(
            "com.android.settings"  // Ajustos
        )
    }
}
