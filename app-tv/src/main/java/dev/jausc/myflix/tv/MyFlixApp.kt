package dev.jausc.myflix.tv

import android.app.Application
import dev.jausc.myflix.core.common.youtube.YouTubeTrailerResolver

class MyFlixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        YouTubeTrailerResolver.initialize(this)
    }
}
