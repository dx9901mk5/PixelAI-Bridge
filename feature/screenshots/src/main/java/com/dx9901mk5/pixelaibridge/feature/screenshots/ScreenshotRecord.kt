package com.dx9901mk5.pixelaibridge.feature.screenshots

data class ScreenshotRecord(
    val mediaId: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedSec: Long,
    val ocrText: String,
    val category: String,
    val aiSummary: String,
    val indexedAtMs: Long
)
