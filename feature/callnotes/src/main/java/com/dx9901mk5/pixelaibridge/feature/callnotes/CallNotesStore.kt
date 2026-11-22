package com.dx9901mk5.pixelaibridge.feature.callnotes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CallNoteRecord(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val createdAtMs: Long
)

class CallNotesStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                summary TEXT NOT NULL,
                created_at_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    suspend fun insert(note: CallNoteRecord): CallNoteRecord = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", note.title)
            put("summary", note.summary)
            put("created_at_ms", note.createdAtMs)
        }
        val rowId = writableDatabase.insert(TABLE_NOTES, null, values)
        note.copy(id = rowId)
    }

    suspend fun getRecent(limit: Int = 30): List<CallNoteRecord> = withContext(Dispatchers.IO) {
        val list = ArrayList<CallNoteRecord>()
        readableDatabase.rawQuery(
            "SELECT id, title, summary, created_at_ms FROM $TABLE_NOTES ORDER BY created_at_ms DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CallNoteRecord(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        summary = cursor.getString(2),
                        createdAtMs = cursor.getLong(3)
                    )
                )
            }
        }
        list
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_NOTES", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    companion object {
        private const val DB_NAME = "pixelai_callnotes.db"
        private const val DB_VERSION = 1
        private const val TABLE_NOTES = "call_notes"
    }
}
