package mxkmn.blindy

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.utils.setFullScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    lateinit var sceneView: ArSceneView
    lateinit var statusText: TextView
    lateinit var placeModelButton: ExtendedFloatingActionButton

    var modelNode: ArModelNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        statusText = findViewById(R.id.statusText)
        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            onArTrackingFailureChanged = { reason ->
                statusText.text = reason?.getDescription(context)
                statusText.isGone = reason == null
            }
        }

        placeModelButton = findViewById<ExtendedFloatingActionButton>(R.id.placeModelButton).apply {
            setOnClickListener {
                modelNode?.anchor()
                placeModelButton.isVisible = true
                sceneView.planeRenderer.isVisible = true
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
    }

    // поворот модели относительно камеры
    fun rotateModel(){
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

    fun setupModelNode() {
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
                placeModelButton.isGone = node.isAnchored
            }
            onHitResult = { node, _ ->
                placeModelButton.isGone = !node.isTracking
            }
        }
        sceneView.addChild(modelNode!!)
        // Select the model node by default (the model node is also selected on tap)
        sceneView.selectedNode = modelNode
    }
}