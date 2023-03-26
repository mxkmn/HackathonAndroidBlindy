package mxkmn.blindy

import android.app.Activity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.mrx.indoorservice.api.IndoorService
import com.mrx.indoorservice.domain.model.BeaconsEnvironmentInfo
import com.mrx.indoorservice.domain.model.EnvironmentInfo
import com.mrx.indoorservice.domain.model.Point
import com.mrx.indoorservice.domain.model.StateEnvironment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.service.ArmaRssiFilter

class BeaconManager(activity: Activity, lifecycleOwner: LifecycleOwner, scope: LifecycleCoroutineScope) {
    private val indoorService = IndoorService.getInstance(activity.applicationContext)

    private val _coordinate = MutableStateFlow( Point(0.0, 0.0) )
    val coordinate: StateFlow<Point<Double>>
        get() = _coordinate

    private val _azimuth = MutableStateFlow( 0 )
    val azimuth: StateFlow<Int>
        get() = _azimuth

    private val beaconsPositions = listOf(
        StateEnvironment("E6:96:DA:5C:82:59", Point(0.0, 0.0)), // одинокий кортеж
        StateEnvironment("F8:8E:31:1C:9A:21", Point(10.0, 0.0)), // токсичный уголок
        StateEnvironment("CF:CA:06:0F:D0:F9", Point(0.0, 10.0)), // у проектора
        StateEnvironment("D3:81:75:66:79:B8", Point(10.0, 10.0)), // оконный угол
//        StateEnvironment("F2:59:A3:E2:E0:AA", Point(8.2, 0.0)), // mR
//        StateEnvironment("E4:C1:3F:EF:49:D7", Point(8.2, 6.0)), // bL
    )

    init {
        indoorService.Position.setEnvironment(beaconsPositions)

        BeaconManager.setRssiFilterImplClass(ArmaRssiFilter::class.java)

        indoorService.BeaconsEnvironment.getRangingViewModel()
            .observe(lifecycleOwner) { scope.launch {
                parseBeacons(it)
            } }

        indoorService.AzimuthManager.getAzimuthViewModel()
            .observe(lifecycleOwner) { libAzimuth ->
                scope.launch {
                    libAzimuth?.let { _azimuth.emit(it.toInt()) }
                }
            }
    }

    fun start() {
        indoorService.BeaconsEnvironment.startRanging()
        indoorService.AzimuthManager.startListen()
    }

    fun pause() {
        indoorService.BeaconsEnvironment.stopRanging()
        indoorService.AzimuthManager.stopListen()
    }

    private suspend fun parseBeacons(beacons: Collection<BeaconsEnvironmentInfo>?) {
        if (beacons == null) return

        println("Ranged ${beacons.size} beacons")

        try {
            val posInfo = indoorService.Position.getPosition(beacons.map { EnvironmentInfo(it.beaconId, it.distance) })
            println("Position: (${posInfo.position.x}, ${posInfo.position.y})")

            if (posInfo.position.x != 0.0 && posInfo.position.y != 0.0 &&
                posInfo.position.x != 10.0 && posInfo.position.x != 10.0) // library bug fix
                _coordinate.emit(posInfo.position)
        } catch (e: Exception) {
            println("No position")
        }
    }
}