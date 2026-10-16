package com.folotoy.passport

import android.content.Context
import android.content.Intent
import android.net.Uri

object ExternalMapLauncher {
    fun openAmap(context: Context, track: TrackPoint) {
        val nativeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MapLinks.amapApp(track.name, track.lat, track.lon))).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage("com.autonavi.minimap")
        }
        if (runCatching { context.startActivity(nativeIntent); true }.getOrDefault(false)) return
        if (open(context, MapLinks.amap(track.name, track.lat, track.lon))) return
        openGeo(context, track)
    }

    fun openGoogle(context: Context, track: TrackPoint) {
        val url = MapLinks.google(track.name, track.lat, track.lon, track.placeId)
        val nativeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.google.android.apps.maps")
        if (runCatching { context.startActivity(nativeIntent); true }.getOrDefault(false)) return
        if (open(context, url)) return
        openGeo(context, track)
    }

    private fun open(context: Context, url: String): Boolean =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true }.getOrDefault(false)

    private fun openGeo(context: Context, track: TrackPoint) {
        val uri = Uri.parse("geo:${track.lat},${track.lon}?q=${track.lat},${track.lon}(${Uri.encode(track.name)})")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
