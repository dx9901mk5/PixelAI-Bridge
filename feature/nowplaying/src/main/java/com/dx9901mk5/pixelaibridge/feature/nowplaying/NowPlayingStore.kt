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
    val recognizedAtMs: Long,
    val isFavorite: Boolean = false
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
                recognized_at_ms INTEGER NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TRACKS ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
        }
    }

    suspend fun insert(track: NowPlayingTrack): NowPlayingTrack = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", track.title)
            put("artist", track.artist)
            put("confidence_note", track.confidenceNote)
            put("recognized_at_ms", track.recognizedAtMs)
            put("is_favorite", if (track.isFavorite) 1 else 0)
        }
        val rowId = writableDatabase.insert(TABLE_TRACKS, null, values)
        track.copy(id = rowId)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("is_favorite", if (favorite) 1 else 0)
        }
        writableDatabase.update(TABLE_TRACKS, values, "id = ?", arrayOf(id.toString()))
    }

    suspend fun getRecent(limit: Int = 50): List<NowPlayingTrack> = withContext(Dispatchers.IO) {
        val list = ArrayList<NowPlayingTrack>()
        readableDatabase.rawQuery(
            "SELECT id, title, artist, confidence_note, recognized_at_ms, is_favorite FROM $TABLE_TRACKS ORDER BY recognized_at_ms DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    NowPlayingTrack(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        artist = cursor.getString(2),
                        confidenceNote = cursor.getString(3),
                        recognizedAtMs = cursor.getLong(4),
                        isFavorite = cursor.getInt(5) == 1
                    )
                )
            }
        }
        list
    }

    companion object {
        private const val DB_NAME = "pixelai_nowplaying.db"
        private const val DB_VERSION = 2
        private const val TABLE_TRACKS = "tracks"
    }
}
