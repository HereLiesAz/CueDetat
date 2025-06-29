package com.hereliesaz.cuedetat.ar.entity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.Entity
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Renderable
import com.google.ar.core.Pose

// A generic interface for our old renderable classes
interface GLRenderable {
    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray)
}

// A custom SceneCore component to wrap our GLRenderable
class GLRenderableComponent(val glRenderable: GLRenderable) : Component(), Renderable {
    override fun render(renderContext: androidx.xr.scenecore.RenderContext) {
        // This is where the magic happens: we call the original draw method.
        glRenderable.draw(renderContext.viewMatrix, renderContext.projectionMatrix)
    }
}

@Composable
fun RenderableEntity(
    pose: Pose,
    glRenderableFactory: @Composable () -> GLRenderable
) {
    val glRenderable = glRenderableFactory()
    val renderableComponent = remember(glRenderable) { GLRenderableComponent(glRenderable) }

    Entity(
        pose = pose,
        components = listOf(renderableComponent)
    )
}