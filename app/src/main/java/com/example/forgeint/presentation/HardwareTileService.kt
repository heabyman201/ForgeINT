package com.example.forgeint.presentation

import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.example.weargemini.data.SettingsManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.wear.tiles.LayoutElementBuilders.Row

// ... (Imports remain the same)

class HardwareTileService : TileService() {
    private val RESOURCES_VERSION = "1"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val future = SettableFuture.create<Tile>()
        serviceScope.launch {
            try {
                val deviceParams = requestParams.deviceConfiguration
                val settingsManager = SettingsManager(this@HardwareTileService)
                
                // 1. Resolve Host/Port
                val host = settingsManager.hostIp.first()
                val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
                val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok") || cleanHost.contains("loclx")
                val hostWithoutPort = cleanHost.substringBefore(":")

                val baseUrl = if (isTunnel) {
                    "https://$cleanHost/"
                } else {
                    "http://$hostWithoutPort:8080/"
                }

                // 2. Fetch Data
                val api = HardwareApi.create(baseUrl)
                val repo = HardwareRepository(api, baseUrl)
                val stats = repo.fetchG14Status() // Returns valid stats or stats with errorMessage

                // 3. Logic
                val cpuTemp = parseValue(stats.cpuTemp)
                val gpuTemp = parseValue(stats.dgpuTemp)
                val maxTemp = maxOf(cpuTemp, gpuTemp)

                val (statusText, statusColor) = when {
                    stats.errorMessage != null -> "ERROR" to 0xFFD32F2F
                    maxTemp > 90 -> "CRITICAL" to 0xFFD32F2F
                    maxTemp > 75 -> "CAUTION" to 0xFFF57C00
                    maxTemp > 0 -> "OPTIMAL" to 0xFF388E3C
                    else -> "OFFLINE" to 0xFF888888
                }

                val tile = buildTile(stats, statusText, statusColor, deviceParams)
                future.set(tile)
            } catch (e: Exception) {
                // Return offline tile on exception
                val offlineStats = G14Stats() // All "N/A"
                val tile = buildTile(offlineStats, "OFFLINE", 0xFF888888, requestParams.deviceConfiguration)
                future.set(tile)
            }
        }
        return future
    }

    private fun buildTile(
        stats: G14Stats, 
        statusText: String, 
        statusColor: Long,
        deviceParams: DeviceParametersBuilders.DeviceParameters
    ): Tile {
        val timelineEntry = TimelineEntry.Builder()
            .setLayout(
                Layout.Builder()
                    .setRoot(
                        layout(this@HardwareTileService, deviceParams, stats, statusText, statusColor)
                    )
                    .build()
            )
            .build()

        val timeline = Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        return Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(timeline)
            .setFreshnessIntervalMillis(60000)
            .build()
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun layout(
        context: Context, 
        deviceParams: DeviceParametersBuilders.DeviceParameters,
        stats: G14Stats,
        statusText: String,
        statusColor: Long
    ): LayoutElementBuilders.LayoutElement {
        return Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                Text.Builder()
                    .setText("Server Health")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setColor(argb(0xFFAAAAAA.toInt())).setSize(sp(10f)).build())
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(2f)).build())
            .addContent(
                Text.Builder()
                    .setText(statusText)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(statusColor.toInt()))
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .setSize(sp(14f))
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(4f)).build())
            .addContent(
                Row.Builder()
                    .addContent(
                        Text.Builder()
                            .setText("GPU: ${stats.dgpuTemp}  |  ")
                            .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setColor(argb(0xFFFFFFFF.toInt())).setSize(sp(12f)).build())
                            .build()
                    )
                    .addContent(
                        Text.Builder()
                            .setText("CPU: ${stats.cpuTemp}")
                            .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setColor(argb(0xFFFFFFFF.toInt())).setSize(sp(12f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(8f)).build())
            .addContent(
                openMonitorButton(context)
            )
            .build()
    }

    private fun openMonitorButton(context: Context): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp(100f))
            .setHeight(dp(32f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("open_monitor")
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(context.packageName)
                                            .setClassName("com.example.forgeint.presentation.MainActivity")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF1E222C.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(16f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder()
                    .setText("Monitor")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setColor(argb(0xFFDCB17E.toInt())).setSize(sp(12f)).build())
                    .build()
            )
            .build()
    }
}
