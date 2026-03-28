package com.example.dokodemotv.epg

import android.util.Log
import android.util.Xml
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EpgParser {
    companion object {
        private const val TAG = "EpgParser"
        // XMLTV time format: 20260320140000 +0900
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    data class ParseResult(
        val channels: List<EpgChannel>,
        val programs: List<EpgProgram>
    )

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): ParseResult {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            return readTv(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTv(parser: XmlPullParser): ParseResult {
        val channels = mutableListOf<EpgChannel>()
        val programs = mutableListOf<EpgProgram>()

        parser.require(XmlPullParser.START_TAG, null, "tv")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "channel" -> channels.add(readChannel(parser))
                "programme" -> programs.add(readProgramme(parser))
                else -> skip(parser)
            }
        }
        return ParseResult(channels, programs)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readChannel(parser: XmlPullParser): EpgChannel {
        parser.require(XmlPullParser.START_TAG, null, "channel")
        val id = parser.getAttributeValue(null, "id") ?: ""
        var displayName = ""
        var iconUrl: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "display-name" -> displayName = readText(parser)
                "icon" -> {
                    iconUrl = parser.getAttributeValue(null, "src")
                    skip(parser)
                }
                else -> skip(parser)
            }
        }
        return EpgChannel(id, displayName, iconUrl)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProgramme(parser: XmlPullParser): EpgProgram {
        parser.require(XmlPullParser.START_TAG, null, "programme")
        val channelId = parser.getAttributeValue(null, "channel") ?: ""
        val startStr = parser.getAttributeValue(null, "start") ?: ""
        val stopStr = parser.getAttributeValue(null, "stop") ?: ""

        var title = ""
        var description: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readText(parser)
                "desc" -> description = readText(parser)
                else -> skip(parser)
            }
        }

        val startTime = parseTime(startStr)
        val endTime = parseTime(stopStr)

        return EpgProgram(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun parseTime(timeStr: String): Long {
        return try {
            DATE_FORMAT.parse(timeStr)?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeStr", e)
            0L
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
