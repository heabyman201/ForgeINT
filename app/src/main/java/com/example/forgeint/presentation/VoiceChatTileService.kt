package com.example.forgeint.presentation

import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.tiles.ResourceBuilders.ImageResource
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.example.forgeint.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class VoiceChatTileService : TileService() {
    private val resourcesVersion = "2"
    private val micResourceId = "voice_chat_mic"

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val timelineEntry = TimelineEntry.Builder()
            .setLayout(
                Layout.Builder()
                    .setRoot(layout(this, requestParams.deviceConfiguration))
                    .build()
            )
            .build()

        val tile = Tile.Builder()
            .setResourcesVersion(resourcesVersion)
            .setTimeline(
                Timeline.Builder()
                    .addTimelineEntry(timelineEntry)
                    .build()
            )
            .setFreshnessIntervalMillis(0)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder()
                .setVersion(resourcesVersion)
                .addIdToImageMapping(
                    micResourceId,
                    ImageResource.Builder()
                        .setAndroidResourceByResId(
                            AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_tile_mic)
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun layout(
        context: Context,
        deviceParams: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(Spacer.Builder().setHeight(dp(10f)).build())
            .addContent(
                Text.Builder()
                    .setText("ForgeINT")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFF7E8A97.toInt()))
                            .setSize(sp(10f))
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(
                Text.Builder()
                    .setText("Hands-free mode")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFFFFFFFF.toInt()))
                            .setSize(sp(13f))
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(12f)).build())
            .addContent(voiceLaunchButton(context))
            .addContent(Spacer.Builder().setHeight(dp(10f)).build())
            .addContent(
                Text.Builder()
                    .setText("One tap to speak")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFFBFC7D1.toInt()))
                            .setSize(sp(11f))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun voiceLaunchButton(context: Context): LayoutElementBuilders.LayoutElement {
        return Column.Builder()
            .setWidth(dp(146f))
            .setHeight(dp(70f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("launch_voice_chat")
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
                            .setColor(argb(0xFFDCB17E.toInt()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(dp(35f))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(14f)).build())
            .addContent(
                Image.Builder()
                    .setResourceId(micResourceId)
                    .setWidth(dp(24f))
                    .setHeight(dp(24f))
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(
                Text.Builder()
                    .setText("Launch now")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(argb(0xFF4A381C.toInt()))
                            .setSize(sp(11f))
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
