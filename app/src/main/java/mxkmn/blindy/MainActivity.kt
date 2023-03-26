package mxkmn.blindy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.utils.setFullScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mxkmn.blindy.databinding.ActivityMainBinding
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val beaconManager by lazy { BeaconManager(this, this, lifecycleScope) }

    var pointPosition: Float3? = null // our target
    var modelNode: ArModelNode? = null
    var userPosition: Float3? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        binding.sceneView.apply {
            onArTrackingFailureChanged = { reason ->
                binding.statusText.text = reason?.getDescription(context)
                binding.statusText.isGone = reason == null
            }
        }

        binding.placeModelButton.apply {
            setOnClickListener {
                modelNode?.anchor()
                pointPosition = modelNode?.anchor?.pose?.position
                binding.placeModelButton.isVisible = false
                binding.sceneView.planeRenderer.isVisible = false
            }
        }

        setupModelNode()
        
        // пересчет поворота баннера
        lifecycleScope.launch {
            while (true) {
                rotateModel()
                delay(70)
            }
        }

        // получение положения с маяков
        lifecycleScope.launch {
            beaconManager.coordinate.collect {
                statusText.text = it.toString()
                statusText.isGone = false

                userPosition?.x= it.x.toFloat()
                userPosition?.y= it.y.toFloat()
                distanceText.text= "distance " + userPosition?.let { mePos ->
                    pointPosition?.let { objPos ->
                        calculateDistance(
                            mePos, objPos
                        )
                    }
                }
            }
        }
    }

    // поворот модели относительно камеры
    private fun rotateModel() {
        val cameraPosition = binding.sceneView.cameraNode.worldPosition.toFloatArray().let { arr ->
            Float3(arr[0], arr[1], arr[2])
        }
        val modelPosition = modelNode?.worldPosition?.toFloatArray().let { arr ->
            Float3(arr?.get(0) ?: 0f, arr?.get(1) ?: 0f, arr?.get(2) ?: 0f)
        }
        val direction = cameraPosition - modelPosition
        val angle = Math.toDegrees(atan2(direction.y.toDouble(), direction.z.toDouble())).toFloat()

        modelNode?.rotation = Rotation(0f, angle, 0f)
    }
    
    override fun onResume() {
        super.onResume()

        beaconManager.start()
    }

    override fun onPause() {
        super.onPause()

        beaconManager.pause()
    }

    private fun setupModelNode() {
        modelNode?.takeIf { !it.isAnchored }?.let {
            binding.sceneView.removeChild(it)
            it.destroy()
        }

        modelNode = ArModelNode(PlacementMode.INSTANT).apply {
            applyPoseRotation = false
            loadModelGlbAsync(
                context = this@MainActivity,
                glbFileLocation = "models/lustra.glb",
                autoAnimate = false,
                scaleToUnits = 1.0f,
                // Place the model origin at the bottom center
                centerOrigin = Position(y = -1.0f)
            ) {
                binding.sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = { node, _ ->
                binding.placeModelButton.isGone = node.isAnchored
            }
            onHitResult = { node, _ ->
                binding.placeModelButton.isGone = !node.isTracking
            }
            
            userPosition = position

            binding.sceneView.addChild(this)

            // Select the model node by default (the model node is also selected on tap)
            binding.sceneView.selectedNode = this
        }
    }

    private fun calculateDistance(a:Float3, b:Float3) =
        sqrt((a.x-b.x).pow(2) + (a.y-b.y).pow(2) + (a.z-b.z).pow(2))
}