package com.hereliesaz.cuedetat.ar.rendering

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.hereliesaz.cuedetat.R
import java.util.concurrent.CompletableFuture

class TextNode(
    context: Context,
    initialText: String
) : Node() {

    private var renderableFuture: CompletableFuture<ViewRenderable>

    private val onUpdateListener = Scene.OnUpdateListener {
        if (this.scene == null) return@OnUpdateListener
        val cameraPosition = this.scene!!.camera.worldPosition
        val nodePosition = this.worldPosition
        val direction = Vector3.subtract(cameraPosition, nodePosition)
        this.worldRotation = Quaternion.lookRotation(direction, Vector3.up())
    }

    override fun onActivate() {
        super.onActivate()
        scene?.addOnUpdateListener(onUpdateListener)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        scene?.removeOnUpdateListener(onUpdateListener)
    }

    init {
        renderableFuture = ViewRenderable.builder()
            .setView(context, R.layout.ar_text_label)
            .build()
            .thenApply { renderable ->
                val textView = renderable.view.findViewById<TextView>(R.id.ar_text_view)
                textView.text = initialText
                this.renderable = renderable
                renderable
            }
            .exceptionally { throwable ->
                Log.e("TextNode", "Failed to load ViewRenderable", throwable)
                null
            }
    }

    fun updateText(newText: String) {
        renderableFuture.thenAccept { renderable ->
            val textView = renderable.view.findViewById<TextView>(R.id.ar_text_view)
            textView.text = newText
        }
    }
}
