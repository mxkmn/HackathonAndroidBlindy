package mxkmn.blindy

import com.google.ar.sceneform.math.Vector3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Place(val location: GeometryLocation) {

    fun Place.getPositionVector(azimuth:Float, x: Double, y: Double): Vector3 {
        val r = -2f
        val xx = r * sin(azimuth + atan2(y, x)).toFloat()
        val yy = 1f
        val zz = r * cos(azimuth + atan2(y,x)).toFloat()
        return Vector3(xx, yy, zz)
    }

    data class GeometryLocation(
        val x: Double,
        val y: Double
    )
}