package com.whisperyao.dsplayer.playing

import android.content.Context;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext;
import java.util.List;
import javax.inject.Inject;

class ChromeCastHelper @Inject constructor(
    @ApplicationContext context: Context
) {

    interface OnRouteSetChangedListener {
        fun onRouteSetChanged()
    }

    val router: MediaRouter =
        MediaRouter.getInstance(context)

    val selector: MediaRouteSelector =
        MediaRouteSelector.Builder()
            .addControlCategory(
                CastMediaControlIntent.categoryForCast("ED01B6D7")
            )
            .build()

    private val mediaRouterCallback = object : MediaRouter.Callback() {

        override fun onRouteAdded(
            router: MediaRouter,
            route: MediaRouter.RouteInfo
        ) {
            notifyRouteChanged()
        }

        override fun onRouteRemoved(
            router: MediaRouter,
            route: MediaRouter.RouteInfo
        ) {
            notifyRouteChanged()
        }

        override fun onRouteChanged(
            router: MediaRouter,
            route: MediaRouter.RouteInfo
        ) {
            notifyRouteChanged()
        }

        private fun notifyRouteChanged() {
            onRouteSetChangedListener?.onRouteSetChanged()
        }
    }

    var onRouteSetChangedListener: OnRouteSetChangedListener? = null

    val routes: MutableList<MediaRouter.RouteInfo> = mutableListOf()

    init {
        router.addCallback(
            selector,
            mediaRouterCallback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
        )
    }

    fun getLoadedRoutes(): List<MediaRouter.RouteInfo> = routes as List<MediaRouter.RouteInfo>

    fun loadRoutes() {
        routes.clear()

        router.routes.forEach { route ->
            if (!route.isDefault &&
                route.isEnabled &&
                route.matchesSelector(selector)
            ) {
                routes.add(route)
            }
        }
    }
}