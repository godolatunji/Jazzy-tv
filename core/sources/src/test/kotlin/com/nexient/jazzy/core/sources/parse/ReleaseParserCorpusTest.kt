package com.nexient.jazzy.core.sources.parse

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.HdrFormat
import com.nexient.jazzy.core.model.HdrFormat.DOLBY_VISION
import com.nexient.jazzy.core.model.HdrFormat.HDR10
import com.nexient.jazzy.core.model.HdrFormat.HDR10_PLUS
import com.nexient.jazzy.core.model.ReleaseInfo
import com.nexient.jazzy.core.model.ReleaseSource
import com.nexient.jazzy.core.model.ReleaseSource.BLURAY
import com.nexient.jazzy.core.model.ReleaseSource.CAM
import com.nexient.jazzy.core.model.ReleaseSource.DVD
import com.nexient.jazzy.core.model.ReleaseSource.HDCAM
import com.nexient.jazzy.core.model.ReleaseSource.HDTV
import com.nexient.jazzy.core.model.ReleaseSource.REMUX
import com.nexient.jazzy.core.model.ReleaseSource.SCREENER
import com.nexient.jazzy.core.model.ReleaseSource.TELECINE
import com.nexient.jazzy.core.model.ReleaseSource.TELESYNC
import com.nexient.jazzy.core.model.ReleaseSource.WEBRIP
import com.nexient.jazzy.core.model.ReleaseSource.WEB_DL
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.Resolution.FHD_1080
import com.nexient.jazzy.core.model.Resolution.HD_720
import com.nexient.jazzy.core.model.Resolution.SD
import com.nexient.jazzy.core.model.Resolution.UHD_2160
import com.nexient.jazzy.core.model.VideoCodec
import com.nexient.jazzy.core.model.VideoCodec.AV1
import com.nexient.jazzy.core.model.VideoCodec.H264
import com.nexient.jazzy.core.model.VideoCodec.H265
import com.nexient.jazzy.core.model.VideoCodec.MPEG4
import org.junit.Assert.fail
import org.junit.Test

/**
 * Release-name corpus. Every name is invented (fictional titles or Blender open movies) in the
 * styles real indexes use; none is copied from a real listing. Each case states the full
 * expected [ReleaseInfo], so an unexpected extra field fails too.
 */
class ReleaseParserCorpusTest {

    private class Case(val name: String, val expected: ReleaseInfo, val title: String?)

    private fun c(
        name: String,
        res: Resolution? = null,
        src: ReleaseSource? = null,
        codec: VideoCodec? = null,
        hdr: List<HdrFormat> = emptyList(),
        audio: String? = null,
        ch: String? = null,
        year: Int? = null,
        s: Int? = null,
        e: Int? = null,
        pack: Boolean = false,
        complete: Boolean = false,
        title: String? = null,
    ) = Case(name, ReleaseInfo(res, src, codec, hdr, audio, ch, year, s, e, pack, complete), title)

    private val corpus = listOf(
        // Movies: scene / P2P style
        c("The.Quiet.Harbor.2019.1080p.BluRay.x264-NOVA", FHD_1080, BLURAY, H264, year = 2019, title = "The Quiet Harbor"),
        c("Neon.Tide.2021.2160p.UHD.BluRay.REMUX.HDR10.HEVC.TrueHD.7.1.Atmos-FGRP", UHD_2160, REMUX, H265, listOf(HDR10), "TrueHD Atmos", "7.1", 2021),
        c("Paper.Lanterns.2020.720p.WEBRip.x264.AAC-YIFY", HD_720, WEBRIP, H264, audio = "AAC", year = 2020),
        c("Glass.Orchard.2022.1080p.AMZN.WEB-DL.DDP5.1.H.264-FLUX", FHD_1080, WEB_DL, H264, audio = "DDP", ch = "5.1", year = 2022),
        c("Silver.Meridian.2023.2160p.WEB-DL.DDP5.1.Atmos.DV.HDR10.H.265-GRP", UHD_2160, WEB_DL, H265, listOf(HDR10, DOLBY_VISION), "DDP Atmos", "5.1", 2023),
        // Low-quality sources
        c("Iron.Valley.2018.HDCAM.x264-CAMGRP", src = HDCAM, codec = H264, year = 2018, title = "Iron Valley"),
        c("Iron.Valley.2018.CAM.XviD-CMRG", SD, CAM, MPEG4, year = 2018),
        c("Iron Valley 2018 HDTS 720p x264", HD_720, TELESYNC, H264, year = 2018),
        c("Iron.Valley.2018.TS.XviD", SD, TELESYNC, MPEG4, year = 2018),
        c("Iron.Valley.2018.TC.1080p.x264", FHD_1080, TELECINE, H264, year = 2018),
        c("Iron.Valley.2018.DVDSCR.XviD-SCRN", SD, SCREENER, MPEG4, year = 2018),
        c("Iron.Valley.2018.SCREENER.720p", HD_720, SCREENER, year = 2018),
        c("Iron.Valley.2018.HDTC.x264", src = TELECINE, codec = H264, year = 2018),
        c("Iron.Valley.2018.TELESYNC.x264", src = TELESYNC, codec = H264, year = 2018),
        c("Iron.Valley.2018.DVDRip.XviD-DVD", SD, DVD, MPEG4, year = 2018),
        c("Old.Lighthouse.1994.480p.DVDRip.x264.AC3", SD, DVD, H264, audio = "AC3", year = 1994),
        c("Crimson.Atlas.2017.1080p.BRRip.x264.AAC5.1", FHD_1080, BLURAY, H264, audio = "AAC", ch = "5.1", year = 2017),
        c("Crimson.Atlas.2017.720p.BDRip.x265.10bit", HD_720, BLURAY, H265, year = 2017),
        // YTS style
        c("Crimson Atlas (2017) [1080p] [BluRay] [5.1] [YTS.MX]", FHD_1080, BLURAY, ch = "5.1", year = 2017, title = "Crimson Atlas"),
        c("Crimson Atlas (2017) [2160p] [4K] [WEB] [5.1] [YTS.MX]", UHD_2160, WEB_DL, ch = "5.1", year = 2017),
        c("Crimson Atlas (2017) [720p] [BluRay] [YTS]", HD_720, BLURAY, year = 2017),
        c("Big Buck Bunny (2008) [1080p] [WEBRip] [x265] [5.1] [YTS]", FHD_1080, WEBRIP, H265, ch = "5.1", year = 2008, title = "Big Buck Bunny"),
        // WEB variants and audio
        c("Velvet.Engine.2024.1080p.WEB.h264-ETHEL", FHD_1080, WEB_DL, H264, year = 2024),
        c("Velvet.Engine.2024.2160p.WEB.H265-ETHEL", UHD_2160, WEB_DL, H265, year = 2024),
        c("Velvet.Engine.2024.1080p.NF.WEB-DL.DDP5.1.Atmos.x264", FHD_1080, WEB_DL, H264, audio = "DDP Atmos", ch = "5.1", year = 2024),
        c("Velvet.Engine.2024.1080p.HDRip.x264.AAC2.0", FHD_1080, WEBRIP, H264, audio = "AAC", ch = "2.0", year = 2024),
        c("Velvet.Engine.2024.720p.HDTV.x264-KILLERS", HD_720, HDTV, H264, year = 2024),
        c("Velvet Engine 2024 1080p WEB-DL AV1 Opus 5.1", FHD_1080, WEB_DL, AV1, audio = "Opus", ch = "5.1", year = 2024),
        c("Velvet.Engine.2024.2160p.BluRay.x265.10bit.HDR10Plus.DTS-HD.MA.5.1", UHD_2160, BLURAY, H265, listOf(HDR10_PLUS), "DTS-HD MA", "5.1", 2024),
        c("Velvet.Engine.2024.2160p.BluRay.HDR10+.HEVC.DTS-X.7.1", UHD_2160, BLURAY, H265, listOf(HDR10_PLUS), "DTS:X", "7.1", 2024),
        c("Velvet.Engine.2024.2160p.UHD.BluRay.Remux.DoVi.HDR10.TrueHD.Atmos", UHD_2160, REMUX, hdr = listOf(HDR10, DOLBY_VISION), audio = "TrueHD Atmos", year = 2024),
        c("Velvet.Engine.2024.2160p.WEB-DL.Dolby.Vision.HEVC.DDP.5.1", UHD_2160, WEB_DL, H265, listOf(DOLBY_VISION), "DDP", "5.1", 2024),
        c("Velvet.Engine.2024.1080p.WEB-DL.DD+5.1.H264", FHD_1080, WEB_DL, H264, audio = "DDP", ch = "5.1", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.x264.DTS-HD.MA.7.1", FHD_1080, BLURAY, H264, audio = "DTS-HD MA", ch = "7.1", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.x264.DTS", FHD_1080, BLURAY, H264, audio = "DTS", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.x264.EAC3.5.1", FHD_1080, BLURAY, H264, audio = "DDP", ch = "5.1", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.FLAC.2.0.x264", FHD_1080, BLURAY, H264, audio = "FLAC", ch = "2.0", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.LPCM.2.0.AVC", FHD_1080, BLURAY, H264, audio = "LPCM", ch = "2.0", year = 2024),
        c("Velvet.Engine.2024.1080p.BluRay.6CH.x265.HEVC-PSA", FHD_1080, BLURAY, H265, ch = "5.1", year = 2024),
        c("Velvet.Engine.2024.720p.WEBRip.2CH.x265", HD_720, WEBRIP, H265, ch = "2.0", year = 2024),
        c("Velvet.Engine.2024.1080p.WEBRip.x264.MP3", FHD_1080, WEBRIP, H264, audio = "MP3", year = 2024),
        // Titles with punctuation, numbers and years
        c("Spider.Man.No.Way.Home.2021.1080p.WEBRip.x264", FHD_1080, WEBRIP, H264, year = 2021, title = "Spider Man No Way Home"),
        c("Blade.Runner.2049.2017.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 2017, title = "Blade Runner 2049"),
        c("Blade.Runner.2049.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 2049, title = "Blade Runner"),
        c("1917.2019.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 2019, title = "1917"),
        c("2012.2009.720p.BluRay.x264", HD_720, BLURAY, H264, year = 2009, title = "2012"),
        c("Wonder.Woman.1984.2020.2160p.WEB-DL.DDP5.1.HDR.HEVC", UHD_2160, WEB_DL, H265, listOf(HDR10), "DDP", "5.1", 2020, title = "Wonder Woman 1984"),
        c("Cam.2018.1080p.NF.WEB-DL.DD5.1.x264", FHD_1080, WEB_DL, H264, audio = "AC3", ch = "5.1", year = 2018, title = "Cam"),
        c("The.Cam.Story.2021.720p.WEBRip.x264", HD_720, WEBRIP, H264, year = 2021, title = "The Cam Story"),
        c("The.French.Dispatch.2021.HDCAM.x264", src = HDCAM, codec = H264, year = 2021, title = "The French Dispatch"),
        c("Amélie.2001.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 2001, title = "Amélie"),
        c("Fast.and.Furious.6.2013.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 2013, title = "Fast and Furious 6"),
        c("Ocean's.Eleven.2001.720p.BluRay.x264", HD_720, BLURAY, H264, year = 2001, title = "Ocean's Eleven"),
        c("Movie Title Extended Cut 2016 1080p BluRay x264", FHD_1080, BLURAY, H264, year = 2016, title = "Movie Title Extended Cut"),
        c("Robot.2.0.2018.1080p.WEB-DL.x264.AAC", FHD_1080, WEB_DL, H264, audio = "AAC", year = 2018, title = "Robot 2 0"),
        c("District.9.2009.720p.BluRay.x264", HD_720, BLURAY, H264, year = 2009, title = "District 9"),
        c("Apollo.13.1995.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 1995, title = "Apollo 13"),
        c("Se7en.1995.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 1995, title = "Se7en"),
        c("Star.Wars.Episode.4.A.New.Hope.1977.1080p.BluRay.x264", FHD_1080, BLURAY, H264, year = 1977, title = "Star Wars Episode 4 A New Hope"),
        // Prefixes, tags and resolutions
        c("[YTS.MX] Lantern Keeper (2015) [1080p] [WEBRip]", FHD_1080, WEBRIP, year = 2015, title = "Lantern Keeper"),
        c("www.Torrenting.com - Lantern.Keeper.2015.720p.BluRay.x264", HD_720, BLURAY, H264, year = 2015, title = "Lantern Keeper"),
        c("[ www.UIndex.org ] Lantern Keeper 2015 1080p", FHD_1080, year = 2015, title = "Lantern Keeper"),
        c("Lantern.Keeper.2015.MULTi.1080p.BluRay.x264-LOST", FHD_1080, BLURAY, H264, year = 2015),
        c("Lantern.Keeper.2015.TRUEFRENCH.720p.WEB-DL.x264", HD_720, WEB_DL, H264, year = 2015),
        c("Lantern Keeper 2015 1920x1080 WEB-DL", FHD_1080, WEB_DL, year = 2015),
        c("Lantern Keeper 2015 3840x2160 HDR WEB", UHD_2160, WEB_DL, hdr = listOf(HDR10), year = 2015),
        c("Lantern Keeper 2015 4K HDR", UHD_2160, hdr = listOf(HDR10), year = 2015),
        c("Lantern.Keeper.2015.576p.DVDRip", SD, DVD, year = 2015),
        c("Lantern.Keeper.2015.DVD9", SD, DVD, year = 2015),
        c("Lantern Keeper 2015 1080i HDTV", FHD_1080, HDTV, year = 2015),
        c("Lantern.Keeper.2015.PROPER.1080p.WEB.H264", FHD_1080, WEB_DL, H264, year = 2015),
        c("Lantern.Keeper.2015.REPACK.2160p.WEB-DL.DV.HEVC", UHD_2160, WEB_DL, H265, listOf(DOLBY_VISION), year = 2015),
        c("Lantern.Keeper.2015.1080p.WEB-DL.HDR10.DV.x265", FHD_1080, WEB_DL, H265, listOf(HDR10, DOLBY_VISION), year = 2015),
        c("Lantern Keeper", title = "Lantern Keeper"),
        c("Lantern Keeper 2015", year = 2015, title = "Lantern Keeper"),
        c("Lantern.Keeper.(2015).1080p", FHD_1080, year = 2015, title = "Lantern Keeper"),
        // Episodes
        c("Harbor.Lights.S01E02.1080p.WEB.H264-CAKES", FHD_1080, WEB_DL, H264, s = 1, e = 2, title = "Harbor Lights"),
        c("Harbor.Lights.S01E02.720p.HDTV.x264-SVA", HD_720, HDTV, H264, s = 1, e = 2),
        c("Harbor Lights S02E10 2160p WEB-DL DDP5.1 HDR HEVC", UHD_2160, WEB_DL, H265, listOf(HDR10), "DDP", "5.1", s = 2, e = 10),
        c("Harbor.Lights.s03e05.1080p.web.h264", FHD_1080, WEB_DL, H264, s = 3, e = 5),
        c("Harbor.Lights.S01E01E02.1080p.WEB.H264", FHD_1080, WEB_DL, H264, s = 1, e = 1),
        c("Harbor.Lights.S01E01-E03.720p.WEB-DL", HD_720, WEB_DL, s = 1, e = 1),
        c("Harbor.Lights.S01E01-03.720p", HD_720, s = 1, e = 1),
        c("Harbor.Lights.1x02.HDTV.XviD", SD, HDTV, MPEG4, s = 1, e = 2, title = "Harbor Lights"),
        c("Harbor Lights 2x05 720p", HD_720, s = 2, e = 5),
        c("Harbor.Lights.S01E02.REPACK.1080p.WEB.h264", FHD_1080, WEB_DL, H264, s = 1, e = 2),
        c("Harbor.Lights.S01E02.480p.x264-mSD", SD, codec = H264, s = 1, e = 2),
        c("[SubGroup] Harbor Lights - S01E05 (1080p) [ABCDEF12]", FHD_1080, s = 1, e = 5, title = "Harbor Lights"),
        c("Harbor.Lights.S01.E05.720p.WEB", HD_720, WEB_DL, s = 1, e = 5),
        c("Harbor Lights S01 E05 720p", HD_720, s = 1, e = 5),
        c("Harbor.Lights.2005.S01E01.1080p.WEB.H264", FHD_1080, WEB_DL, H264, year = 2005, s = 1, e = 1, title = "Harbor Lights"),
        c("Harbor.Lights.US.S04E07.720p.HDTV.x264", HD_720, HDTV, H264, s = 4, e = 7, title = "Harbor Lights US"),
        c("Harbor Lights S05E12 HDTV x264", src = HDTV, codec = H264, s = 5, e = 12),
        c("Harbor.Lights.S10E101.1080p.WEB.x265", FHD_1080, WEB_DL, H265, s = 10, e = 101),
        c("Harbor.Lights.S02E03.Episode.Title.Here.1080p.AMZN.WEB-DL.DDP2.0.H.264", FHD_1080, WEB_DL, H264, audio = "DDP", ch = "2.0", s = 2, e = 3),
        c("Harbor.Lights.S01E02.Pilot.Part.2.1080p.WEB-DL.x264", FHD_1080, WEB_DL, H264, s = 1, e = 2),
        c("Law.and.Order.SVU.S22E05.720p.HDTV.x264", HD_720, HDTV, H264, s = 22, e = 5, title = "Law and Order SVU"),
        // Season packs and complete series
        c("Harbor.Lights.S01.1080p.BluRay.x264-ROVERS", FHD_1080, BLURAY, H264, s = 1, pack = true, title = "Harbor Lights"),
        c("Harbor.Lights.S01.COMPLETE.720p.WEB-DL", HD_720, WEB_DL, s = 1, pack = true),
        c("Harbor.Lights.Season.1.1080p.WEBRip", FHD_1080, WEBRIP, s = 1, pack = true, title = "Harbor Lights"),
        c("Harbor Lights Season 2 Complete 720p", HD_720, s = 2, pack = true),
        c("Harbor.Lights.S01-S03.1080p.BluRay.x264", FHD_1080, BLURAY, H264, s = 1, pack = true),
        c("Harbor.Lights.S01-03.720p.WEB-DL", HD_720, WEB_DL, s = 1, pack = true),
        c("Harbor Lights Seasons 1-4 1080p", FHD_1080, s = 1, pack = true),
        c("Harbor Lights Season 1 to 3 720p", HD_720, s = 1, pack = true),
        c("Harbor Lights Complete Series 1080p BluRay", FHD_1080, BLURAY, complete = true, title = "Harbor Lights"),
        c("Harbor.Lights.The.Complete.Series.720p.WEB-DL", HD_720, WEB_DL, complete = true, title = "Harbor Lights"),
        c("Harbor Lights Complete Collection S01-S05 1080p", FHD_1080, s = 1, pack = true, complete = true),
        c("Harbor.Lights.Complete.720p.HDTV", HD_720, HDTV, complete = true),
        c("Harbor.Lights.Saison.2.FRENCH.720p.HDTV", HD_720, HDTV, s = 2, pack = true),
        c("Harbor.Lights.Integrale.MULTi.1080p", FHD_1080, complete = true),
        c("Harbor Lights Series 3 720p HDTV", HD_720, HDTV, s = 3, pack = true),
        c("Harbor.Lights.2019.S01.Complete.1080p.WEB", FHD_1080, WEB_DL, year = 2019, s = 1, pack = true),
        c("Harbor Lights (2019) Season 1 S01 1080p", FHD_1080, year = 2019, s = 1, pack = true),
        // More sources, codecs and audio
        c("Ember.Road.2022.1080p.WEB-DL.x264.AAC-EVO", FHD_1080, WEB_DL, H264, audio = "AAC", year = 2022),
        c("Ember.Road.2022.1080p.BluRay.AVC.REMUX.DTS-HD.MA.5.1", FHD_1080, REMUX, H264, audio = "DTS-HD MA", ch = "5.1", year = 2022),
        c("Ember.Road.2022.BDRemux.1080p", FHD_1080, REMUX, year = 2022),
        c("Ember.Road.2022.UHD.BluRay.2160p.TrueHD.Atmos.7.1.HEVC.REMUX-FraMeSToR", UHD_2160, REMUX, H265, audio = "TrueHD Atmos", ch = "7.1", year = 2022),
        c("Ember.Road.2022.720p.WEB-Rip.x264", HD_720, WEBRIP, H264, year = 2022),
        c("Ember.Road.2022.1080p.WEB.DL.x264", FHD_1080, WEB_DL, H264, year = 2022),
        c("Ember.Road.2022.DVDR.NTSC", SD, DVD, year = 2022),
        c("Ember.Road.2022.PDTV.XviD", SD, HDTV, MPEG4, year = 2022),
        c("Ember.Road.2022.HD-TS.x264", src = TELESYNC, codec = H264, year = 2022),
        c("Ember.Road.2022.HD-CAM.x264", src = HDCAM, codec = H264, year = 2022),
        c("Ember.Road.2022.CAMRip.x264", src = CAM, codec = H264, year = 2022),
        c("Ember.Road.2022.BDSCR.x264", src = SCREENER, codec = H264, year = 2022),
        c("Ember.Road.2022.WEBSCR.720p", HD_720, SCREENER, year = 2022),
        c("Ember.Road.2022.1080p.WEBCap.x264", FHD_1080, WEBRIP, H264, year = 2022),
        c("Ember.Road.2022.1080p.DSNP.WEB-DL.DDP5.1.Atmos.H.264", FHD_1080, WEB_DL, H264, audio = "DDP Atmos", ch = "5.1", year = 2022),
        c("Ember Road 2022 1080p BluRay x264 AC3 5.1", FHD_1080, BLURAY, H264, audio = "AC3", ch = "5.1", year = 2022),
        c("Ember Road 2022 1080p BluRay x264 DD5.1", FHD_1080, BLURAY, H264, audio = "AC3", ch = "5.1", year = 2022),
        c("Ember Road 2022 2160p HDR10+ DV WEB-DL H265", UHD_2160, WEB_DL, H265, listOf(HDR10_PLUS, DOLBY_VISION), year = 2022),
        c("Ember Road 2022 2160p SDR WEB-DL H265", UHD_2160, WEB_DL, H265, year = 2022),
        c("Ember.Road.2022.720p.BluRay.x264.DTS-ES", HD_720, BLURAY, H264, audio = "DTS", year = 2022),
        c("Ember Road 2022 1080p BluRay x265 10bit AAC 7.1", FHD_1080, BLURAY, H265, audio = "AAC", ch = "7.1", year = 2022),
        c("Ember Road 2022 1080p WEBRip x265 HEVC 6CH", FHD_1080, WEBRIP, H265, ch = "5.1", year = 2022),
        c("Ember Road 2022 WEB-DL", src = WEB_DL, year = 2022),
        c("Midnight.Atlas.2016.LIMITED.1080p.BluRay.x264-AMIABLE", FHD_1080, BLURAY, H264, year = 2016),
        c("Midnight Atlas 2016 1080p BluRay x264 Dual Audio Hindi English AC3 5.1", FHD_1080, BLURAY, H264, audio = "AC3", ch = "5.1", year = 2016),
        c("Midnight.Atlas.2016.1080p.BluRay.x264.TrueHD.7.1.Atmos-SWTYBLZ", FHD_1080, BLURAY, H264, audio = "TrueHD Atmos", ch = "7.1", year = 2016),
        c("Midnight.Atlas.2016.iNTERNAL.720p.BluRay.x264-CLASSiC", HD_720, BLURAY, H264, year = 2016),
        // Site-specific shapes from the fixtures
        c("Sintel FRENCH WEBRIP 1080p 2010", FHD_1080, WEBRIP, year = 2010, title = "Sintel"),
        c("Sintel 2010 MULTi 1080p BluRay x264", FHD_1080, BLURAY, H264, year = 2010, title = "Sintel"),
        c("Tears of Steel (2012) [720p] [WEB-DL] [AAC2.0]", HD_720, WEB_DL, audio = "AAC", ch = "2.0", year = 2012, title = "Tears of Steel"),
        c("Open Movie Weekly S01E02 1080p WEB H264 [eztv]", FHD_1080, WEB_DL, H264, s = 1, e = 2, title = "Open Movie Weekly"),
    )

    @Test fun `corpus has at least 120 names`() {
        assertThat(corpus.size).isAtLeast(120)
        assertThat(corpus.map { it.name }.toSet().size).isEqualTo(corpus.size)
    }

    @Test fun `every corpus name parses as expected`() {
        val failures = corpus.mapNotNull { case ->
            val parsed = ReleaseParser.parseDetailed(case.name)
            val problems = buildList {
                if (parsed.info != case.expected) add("info: expected ${case.expected}\n            got      ${parsed.info}")
                if (case.title != null && parsed.title != case.title) add("title: expected '${case.title}' got '${parsed.title}'")
            }
            problems.takeIf { it.isNotEmpty() }?.let { "${case.name}\n    " + it.joinToString("\n    ") }
        }
        if (failures.isNotEmpty()) fail("${failures.size} of ${corpus.size} names misparsed:\n" + failures.joinToString("\n"))
    }

    @Test fun `ranges are exposed for matching`() {
        assertThat(ReleaseParser.parseDetailed("Harbor.Lights.S01-S03.1080p").seasons).isEqualTo(1..3)
        assertThat(ReleaseParser.parseDetailed("Harbor.Lights.S01E01-E03.720p").episodes).isEqualTo(1..3)
        assertThat(ReleaseParser.parseDetailed("Harbor Lights Seasons 1-4 1080p").seasons).isEqualTo(1..4)
        assertThat(ReleaseParser.parseDetailed("Harbor.Lights.S02.720p").episodes).isNull()
    }

    @Test fun `quality label combines resolution and source`() {
        assertThat(ReleaseParser.parse("Neon.Tide.2021.1080p.WEB-DL.x264").qualityLabel).isEqualTo("1080p WEB-DL")
        assertThat(ReleaseParser.parse("Neon Tide").qualityLabel).isEqualTo("Unknown")
        assertThat(ReleaseParser.parse("Neon.Tide.2021.HDCAM").source!!.isLowQuality).isTrue()
    }
}
