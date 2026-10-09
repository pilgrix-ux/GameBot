package com.pilgrix.bible

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class BiblePassage(val id: String, val reference: String, val content: String)

enum class YouVersionFailure {
    MISSING_KEY, UNAUTHORIZED, NIV_NOT_ENABLED, PASSAGE_NOT_FOUND, RATE_LIMITED,
    SERVER, NETWORK, INVALID_RESPONSE
}

class YouVersionApiException(
    val failure: YouVersionFailure,
    override val message: String
) : Exception(message)

/** YouVersion Platform passage client. NIV is Bible version 111; access still requires licensing. */
class YouVersionApi(private val appKey: String) {
    val isConfigured: Boolean get() = appKey.isNotBlank()

    suspend fun getChapter(bookName: String, chapter: Int): BiblePassage {
        if (!isConfigured) throw YouVersionApiException(
            YouVersionFailure.MISSING_KEY,
            "The Bible text connection is not configured yet. Register the Android app with YouVersion Platform, enable NIV access for the app, add YVP_APP_KEY to your Gradle user properties, then rebuild."
        )
        if (chapter < 1) throw YouVersionApiException(
            YouVersionFailure.PASSAGE_NOT_FOUND, "Choose a valid chapter."
        )
        val bookCode = bookCodes[bookName] ?: throw YouVersionApiException(
            YouVersionFailure.PASSAGE_NOT_FOUND, "This Bible book is not recognized by the text provider."
        )

        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/bibles/$NIV_BIBLE_ID/passages/$bookCode.$chapter?format=text")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 20_000
                setRequestProperty("X-YVP-App-Key", appKey)
                setRequestProperty("Accept", "application/json")
                useCaches = false
            }
            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    throw when (status) {
                        401 -> YouVersionApiException(YouVersionFailure.UNAUTHORIZED, "YouVersion did not accept this app key. Check YVP_APP_KEY in your Gradle user properties.")
                        403 -> YouVersionApiException(YouVersionFailure.NIV_NOT_ENABLED, "YouVersion received the request, but NIV text access is not enabled for this app key. Check the app's translation/licensing access in the YouVersion Platform portal.")
                        404 -> YouVersionApiException(YouVersionFailure.PASSAGE_NOT_FOUND, "YouVersion could not find this chapter. Please choose another chapter.")
                        429 -> YouVersionApiException(YouVersionFailure.RATE_LIMITED, "The Bible text service is busy. Please wait a moment and try again.")
                        in 500..599 -> YouVersionApiException(YouVersionFailure.SERVER, "The Bible text service is temporarily unavailable. Please try again.")
                        else -> YouVersionApiException(YouVersionFailure.SERVER, "The Bible text service returned an unexpected response (HTTP $status).")
                    }
                }
                val json = try { JSONObject(body) } catch (_: Exception) {
                    throw YouVersionApiException(YouVersionFailure.INVALID_RESPONSE, "The Bible text service returned an unreadable response.")
                }
                val content = json.optString("content", "").trim()
                if (content.isBlank()) throw YouVersionApiException(
                    YouVersionFailure.INVALID_RESPONSE, "No chapter text was returned. Check that NIV access is enabled for this app."
                )
                BiblePassage(
                    id = json.optString("id", "$bookCode.$chapter"),
                    reference = json.optString("reference", "$bookName $chapter"),
                    content = content
                )
            } catch (failure: YouVersionApiException) {
                throw failure
            } catch (_: IOException) {
                throw YouVersionApiException(YouVersionFailure.NETWORK, "Couldn't connect to YouVersion. Check your internet connection and try again.")
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://api.youversion.com/v1"
        const val NIV_BIBLE_ID = 111
        val bookCodes = mapOf(
            "Genesis" to "GEN", "Exodus" to "EXO", "Leviticus" to "LEV",
            "Numbers" to "NUM", "Deuteronomy" to "DEU", "Joshua" to "JOS",
            "Judges" to "JDG", "Ruth" to "RUT", "1 Samuel" to "1SA",
            "2 Samuel" to "2SA", "1 Kings" to "1KI", "2 Kings" to "2KI",
            "1 Chronicles" to "1CH", "2 Chronicles" to "2CH", "Ezra" to "EZR",
            "Nehemiah" to "NEH", "Esther" to "EST", "Job" to "JOB",
            "Psalms" to "PSA", "Proverbs" to "PRO", "Ecclesiastes" to "ECC",
            "Song of Solomon" to "SNG", "Isaiah" to "ISA", "Jeremiah" to "JER",
            "Lamentations" to "LAM", "Ezekiel" to "EZK", "Daniel" to "DAN",
            "Hosea" to "HOS", "Joel" to "JOL", "Amos" to "AMO",
            "Obadiah" to "OBA", "Jonah" to "JON", "Micah" to "MIC",
            "Nahum" to "NAM", "Habakkuk" to "HAB", "Zephaniah" to "ZEP",
            "Haggai" to "HAG", "Zechariah" to "ZEC", "Malachi" to "MAL",
            "Matthew" to "MAT", "Mark" to "MRK", "Luke" to "LUK",
            "John" to "JHN", "Acts" to "ACT", "Romans" to "ROM",
            "1 Corinthians" to "1CO", "2 Corinthians" to "2CO", "Galatians" to "GAL",
            "Ephesians" to "EPH", "Philippians" to "PHP", "Colossians" to "COL",
            "1 Thessalonians" to "1TH", "2 Thessalonians" to "2TH", "1 Timothy" to "1TI",
            "2 Timothy" to "2TI", "Titus" to "TIT", "Philemon" to "PHM",
            "Hebrews" to "HEB", "James" to "JAS", "1 Peter" to "1PE",
            "2 Peter" to "2PE", "1 John" to "1JN", "2 John" to "2JN",
            "3 John" to "3JN", "Jude" to "JUD", "Revelation" to "REV"
        )
    }
}
