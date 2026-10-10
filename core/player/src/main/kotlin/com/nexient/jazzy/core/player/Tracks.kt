package com.nexient.jazzy.core.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

/** One selectable audio or text track (PL-4, PL-8). */
data class TrackOption(val id: String, val label: String, val selected: Boolean, val group: Tracks.Group?, val index: Int)

object TrackHelper {
    fun options(player: Player, type: Int): List<TrackOption> {
        val out = mutableListOf<TrackOption>()
        if (type == C.TRACK_TYPE_TEXT) {
            val disabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
            out += TrackOption("off", "Off", disabled, null, -1)
        }
        player.currentTracks.groups.filter { it.type == type }.forEachIndexed { g, group ->
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val f = group.getTrackFormat(i)
                val lang = f.language?.let { java.util.Locale.forLanguageTag(it).displayLanguage }?.takeIf { it.isNotBlank() }
                val label = listOfNotNull(
                    f.label ?: lang ?: "Track ${out.size + 1}",
                    f.channelCount.takeIf { type == C.TRACK_TYPE_AUDIO && it > 0 }?.let { if (it >= 6) "5.1" else if (it == 2) "Stereo" else "${it}ch" },
                    f.sampleMimeType?.substringAfter('/')?.uppercase()?.takeIf { type == C.TRACK_TYPE_AUDIO },
                ).joinToString(" · ")
                out += TrackOption("$g:$i", label, group.isTrackSelected(i), group, i)
            }
        }
        return out
    }

    fun select(player: Player, type: Int, option: TrackOption) {
        val params = player.trackSelectionParameters.buildUpon()
        if (option.group == null) {
            params.setTrackTypeDisabled(type, true)
        } else {
            params.setTrackTypeDisabled(type, false)
                .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
        }
        player.trackSelectionParameters = params.build()
    }

    fun setPreferredLanguages(player: Player, audio: String?, subtitles: String?) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            audio?.let { setPreferredAudioLanguage(it.substringBefore('-')) }
            if (subtitles != null) setPreferredTextLanguage(subtitles.substringBefore('-')).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        }.build()
    }
}
