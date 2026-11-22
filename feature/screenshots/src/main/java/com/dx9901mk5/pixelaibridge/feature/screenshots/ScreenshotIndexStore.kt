package com.dx9901mk5.pixelaibridge.feature.screenshots

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local SQLite index for OCR text and AI summaries of screenshots.
 */
class ScreenshotIndexStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SCREENSHOTS (
                media_id INTEGER PRIMARY KEY,
                content_uri TEXT NOT NULL,
                display_name TEXT NOT NULL,
                date_added_sec INTEGER NOT NULL,
                ocr_text TEXT NOT NULL,
                category TEXT NOT NULL,
                ai_summary TEXT NOT NULL,
                indexed_at_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Initial schema version 1.
    }

    suspend fun upsert(record: ScreenshotRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("media_id", record.mediaId)
            put("content_uri", record.contentUri)
            put("display_name", record.displayName)
            put("date_added_sec", record.dateAddedSec)
            put("ocr_text", record.ocrText)
            put("category", record.category)
            put("ai_summary", record.aiSummary)
            put("indexed_at_ms", record.indexedAtMs)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SCREENSHOTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun getIndexedIds(): Set<Long> = withContext(Dispatchers.IO) {
        val ids = HashSet<Long>()
        readableDatabase.rawQuery("SELECT media_id FROM $TABLE_SCREENSHOTS", null).use { cursor ->
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0))
            }
        }
        ids
    }

    suspend fun search(query: String): List<ScreenshotRecord> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val cursor = if (trimmed.isEmpty()) {
            readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_SCREENSHOTS ORDER BY date_added_sec DESC LIMIT 100",
                null
            )
        } else {
            val like = "%$trimmed%"
            readableDatabase.rawQuery(
                """
                SELECT * FROM $TABLE_SCREENSHOTS
                WHERE ocr_text LIKE ? OR ai_summary LIKE ? OR category LIKE ? OR display_name LIKE ?
                ORDER BY date_added_sec DESC LIMIT 100
                """.trimIndent(),
                arrayOf(like, like, like, like)
            )
        }
        cursor.use { readRecords(it) }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_SCREENSHOTS", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun readRecords(cursor: Cursor): List<ScreenshotRecord> {
        val list = ArrayList<ScreenshotRecord>()
        val idxId = cursor.getColumnIndexOrThrow("media_id")
        val idxUri = cursor.getColumnIndexOrThrow("content_uri")
        val idxName = cursor.getColumnIndexOrThrow("display_name")
        val idxDate = cursor.getColumnIndexOrThrow("date_added_sec")
        val idxOcr = cursor.getColumnIndexOrThrow("ocr_text")
        val idxCat = cursor.getColumnIndexOrThrow("category")
        val idxSummary = cursor.getColumnIndexOrThrow("ai_summary")
        val idxIndexedAt = cursor.getColumnIndexOrThrow("indexed_at_ms")

        while (cursor.moveToNext()) {
            list.add(
                ScreenshotRecord(
                    mediaId = cursor.getLong(idxId),
                    contentUri = cursor.getString(idxUri),
                    displayName = cursor.getString(idxName),
                    dateAddedSec = cursor.getLong(idxDate),
                    ocrText = cursor.getString(idxOcr),
                    category = cursor.getString(idxCat),
                    aiSummary = cursor.getString(idxSummary),
                    indexedAtMs = cursor.getLong(idxIndexedAt)
                )
            )
        }
        return list
    }

    companion object {
        private const val DB_NAME = "pixelai_screenshots.db"
        private const val DB_VERSION = 1
        private const val TABLE_SCREENSHOTS = "screenshots"
    }
}
