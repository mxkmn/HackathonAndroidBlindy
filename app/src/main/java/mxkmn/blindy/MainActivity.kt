package mxkmn.blindy

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.utils.setFullScreen
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val beaconManager by lazy { BeaconManager(this, this, lifecycleScope) }

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
                placeModelButton.isVisible = false
                sceneView.planeRenderer.isVisible = false
            }
        }

        setupModelNode()

        lifecycleScope.launch {
            beaconManager.coordinate.collect {
                statusText.text = it.toString()
                statusText.isGone = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        beaconManager.start()
    }

    override fun onPause() {
        super.onPause()

        beaconManager.pause()
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
                glbFileLocation = "models/spiderbot.glb",
                autoAnimate = true,
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