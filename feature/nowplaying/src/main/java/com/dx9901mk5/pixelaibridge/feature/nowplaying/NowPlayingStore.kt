package com.dx9901mk5.pixelaibridge.feature.nowplaying

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NowPlayingTrack(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val confidenceNote: String,
    val recognizedAtMs: Long
)

class NowPlayingStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRACKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                confidence_note TEXT NOT NULL,
                recognized_at_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    suspend fun insert(track: NowPlayingTrack): NowPlayingTrack = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", track.title)
            put("artist", track.artist)
            put("confidence_note", track.confidenceNote)
            put("recognized_at_ms", track.recognizedAtMs)
        }
        val rowId = writableDatabase.insert(TABLE_TRACKS, null, values)
        track.copy(id = rowId)
    }

    suspend fun getRecent(limit: Int = 50): List<NowPlayingTrack> = withContext(Dispatchers.IO) {
        val list = ArrayList<NowPlayingTrack>()
        readableDatabase.rawQuery(
            "SELECT id, title, artist, confidence_note, recognized_at_ms FROM $TABLE_TRACKS ORDER BY recognized_at_ms DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    NowPlayingTrack(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        artist = cursor.getString(2),
                        confidenceNote = cursor.getString(3),
                        recognizedAtMs = cursor.getLong(4)
                    )
                )
            }
        }
        list
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_TRACKS", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    companion object {
        private const val DB_NAME = "pixelai_nowplaying.db"
        private const val DB_VERSION = 1
        private const val TABLE_TRACKS = "tracks"
    }
}
