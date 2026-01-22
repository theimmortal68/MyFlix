package dev.jausc.myflix.tv.dream

import android.service.dreams.DreamService
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Base class for implementing a DreamService with Jetpack Compose support.
 *
 * DreamService doesn't extend ComponentActivity, so it lacks the lifecycle
 * infrastructure Compose needs. This class bridges that gap by implementing
 * the required owner interfaces and wiring them up to the DreamService lifecycle.
 *
 * Usage:
 * ```
 * class MyDreamService : DreamServiceCompat() {
 *     override fun onDreamingStarted() {
 *         super.onDreamingStarted()
 *         setContent {
 *             MyTheme {
 *                 DreamScreen()
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class DreamServiceCompat :
    DreamService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val vmStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = vmStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        vmStore.clear()
        super.onDestroy()
    }

    /**
     * Set Compose content for this dream service.
     *
     * Creates a ComposeView and wires up the lifecycle owners so that
     * Compose can properly observe lifecycle events and create ViewModels.
     */
    protected fun setContent(content: @Composable () -> Unit) {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DreamServiceCompat)
            setViewTreeViewModelStoreOwner(this@DreamServiceCompat)
            setViewTreeSavedStateRegistryOwner(this@DreamServiceCompat)
            setContent(content)
        }
        setContentView(view)
    }
}
