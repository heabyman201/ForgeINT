package com.example.forgeint.presentation

import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
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
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class LastChatTile : TileService() {
    private val RESOURCES_VERSION = "1"

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val deviceParams = requestParams.deviceConfiguration
        
        val timelineEntry = TimelineEntry.Builder()
            .setLayout(
                Layout.Builder()
                    .setRoot(
                        layout(this, deviceParams)
                    )
                    .build()
            )
            .build()

        val timeline = Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(timeline)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun layout(context: Context, deviceParams: DeviceParametersBuilders.DeviceParameters): LayoutElementBuilders.LayoutElement {
        return Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                voiceButton(context)
            )
            .addContent(
                Spacer.Builder().setHeight(dp(12f)).build()
            )
            .addContent(
                typeButton(context)
            )
            .build()
    }

    private fun voiceButton(context: Context): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp(140f)) // Bigger button
            .setHeight(dp(60f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("voice_action")
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(context.packageName)
                                            .setClassName("com.example.forgeint.presentation.MainActivity")
                                            .addKeyToExtraMapping(
                                                "action_type",
                                                ActionBuilders.AndroidStringExtra.Builder()
                                                    .setValue("voice")
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFFDCB17E.toInt())) // Primary Gold
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(30f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder()
                    .setText("Voice")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFFDCB17E.toInt()))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun typeButton(context: Context): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp(100f)) // Smaller button
            .setHeight(dp(40f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("type_action")
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(context.packageName)
                                            .setClassName("com.example.forgeint.presentation.MainActivity")
                                             .addKeyToExtraMapping(
                                                "action_type",
                                                ActionBuilders.AndroidStringExtra.Builder()
                                                    .setValue("text")
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF1E222C.toInt())) // User Bubble Color (Dark Grey)
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(20f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder()
                    .setText("Type")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFFDCB17E.toInt())) // Primary Gold Text
                            .build()
                    )
                    .build()
            )
            .build()
    }
}