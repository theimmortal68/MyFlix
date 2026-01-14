package dev.jausc.myflix.mobile

import android.app.Application
import dev.jausc.myflix.core.common.youtube.YouTubeTrailerResolver

class MyFlixMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        YouTubeTrailerResolver.initialize(this)
    }
}
