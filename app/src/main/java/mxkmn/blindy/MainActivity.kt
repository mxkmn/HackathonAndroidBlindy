package mxkmn.blindy

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
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

    private var targetPosition: Float3? = null
    private var modelNode: ArModelNode? = null
    private var userPosition: Float3? = Float3(0.0f,0.0f,0.0f)
    lateinit var sceneView: ArSceneView
    lateinit var statusText: TextView
    lateinit var distanceText: TextView
    lateinit var placeModelButton: ExtendedFloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = findViewById(R.id.statusText)
        distanceText=findViewById(R.id.distance)
        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            onArTrackingFailureChanged = { reason ->
                binding.statusText.text = reason?.getDescription(context)
                binding.statusText.isGone = reason == null
            }
        }

        placeModelButton = findViewById<ExtendedFloatingActionButton>(R.id.placeModelButton).apply {
            setOnClickListener {
                modelNode?.anchor()
                targetPosition = modelNode?.anchor?.pose?.position
                binding.placeModelButton.isVisible = false
                sceneView.planeRenderer.isVisible = false
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
                @SuppressLint("SetTextI18n")
                distanceText.text= "distance " + userPosition?.let { mePos ->
                    targetPosition?.let { objPos ->
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
        val cameraPosition = sceneView.cameraNode.worldPosition.toFloatArray().let { arr ->
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
            sceneView.removeChild(it)
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
                sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = { node, _ ->
                binding.placeModelButton.isGone = node.isAnchored
            }
            onHitResult = { node, _ ->
                binding.placeModelButton.isGone = !node.isTracking
            }
        }
        userPosition = modelNode?.position

        sceneView.addChild(modelNode!!)

        // Select the model node by default (the model node is also selected on tap)
        sceneView.selectedNode = modelNode
    }

    private fun calculateDistance(a:Float3, b:Float3) =
        sqrt((a.x-b.x).pow(2) + (a.y-b.y).pow(2) + (a.z-b.z).pow(2))
}