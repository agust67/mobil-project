package cat.casa.llancador

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Botó «112» del panell de dalt (ajustos ràpids).
 * En prémer-lo, plega el panell i demana confirmació abans de trucar.
 */
class Tile112 : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_112)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intencio = Intent(this, EmergenciaActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendent = PendingIntent.getActivity(this, 0, intencio, PendingIntent.FLAG_IMMUTABLE)
        startActivityAndCollapse(pendent)
    }
}
