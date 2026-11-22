package com.dx9901mk5.pixelaibridge.feature.magiccue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

sealed interface CueAction {
    val label: String

    data class CopyOtp(val code: String) : CueAction {
        override val label: String = "複製驗證碼 $code"
    }

    data class OpenUrl(val url: String) : CueAction {
        override val label: String = "開啟連結"
    }

    data class OpenMap(val query: String) : CueAction {
        override val label: String = "地圖導航：$query"
    }

    data class AddCalendar(val title: String) : CueAction {
        override val label: String = "加入行事曆"
    }
}

object MagicCueActionExtractor {

    fun extract(text: String): List<CueAction> {
        if (text.isBlank()) return emptyList()
        val actions = mutableListOf<CueAction>()

        Regex("\\b\\d{4,8}\\b").find(text)?.value?.let { code ->
            actions.add(CueAction.CopyOtp(code))
        }

        Regex("https?://\\S+").find(text)?.value?.let { url ->
            actions.add(CueAction.OpenUrl(url.trimEnd('.', ',', ')', '」')))
        }

        Regex("[\\u4e00-\\u9fa5A-Za-z0-9]{2,18}(?:路|街|大道|巷|車站|機場|餐廳|門市)").find(text)?.value?.let { place ->
            actions.add(CueAction.OpenMap(place))
        }

        if (Regex("(明天|後天|週[一二三四五六日天]|\\d{1,2}/\\d{1,2}|\\d{1,2}:\\d{2})").containsMatchIn(text)) {
            actions.add(CueAction.AddCalendar(text.lineSequence().firstOrNull()?.take(40) ?: "PixelAI 行程"))
        }

        return actions.distinctBy { it.label }
    }

    fun execute(context: Context, action: CueAction, onFeedback: (String) -> Unit) {
        when (action) {
            is CueAction.CopyOtp -> {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("OTP", action.code))
                onFeedback("已複製驗證碼 ${action.code}")
            }
            is CueAction.OpenUrl -> {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { onFeedback("無法開啟連結") }
            }
            is CueAction.OpenMap -> {
                runCatching {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(action.query)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { onFeedback("找不到地圖應用程式") }
            }
            is CueAction.AddCalendar -> {
                runCatching {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, action.title)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure { onFeedback("無法開啟行事曆") }
            }
        }
    }
}
