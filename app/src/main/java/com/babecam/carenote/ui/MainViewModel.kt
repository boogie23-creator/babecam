package com.babecam.carenote.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babecam.carenote.data.AiException
import com.babecam.carenote.data.Gemini
import com.babecam.carenote.data.Prefs
import com.babecam.carenote.data.RecordResult
import com.babecam.carenote.data.RecordSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var apiKey by mutableStateOf(prefs.apiKey)
        private set
    var model by mutableStateOf(prefs.model)
        private set

    var date by mutableStateOf(LocalDate.now())
    var who by mutableStateOf("김○○ 어르신")
    var note by mutableStateOf(RecordSpec.EXAMPLE_NOTE)

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf(RecordSpec.EXAMPLE)
        private set
    var isExample by mutableStateOf(true)
        private set
    var resultDate by mutableStateOf(LocalDate.now())
        private set
    var resultWho by mutableStateOf("김○○ 어르신")
        private set
    /** 특이사항 문장 — 사용자가 직접 고칠 수 있음 */
    var summary by mutableStateOf(RecordSpec.EXAMPLE.summary)

    private var job: Job? = null

    fun saveSettings(key: String, modelName: String) {
        prefs.apiKey = key
        prefs.model = modelName
        apiKey = prefs.apiKey
        model = prefs.model
    }

    fun appendNote(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        note = if (note.isBlank()) t else note.trimEnd() + " " + t
    }

    fun newRecord() {
        note = ""
        who = ""
        date = LocalDate.now()
        error = null
    }

    fun summarize() {
        val n = note.trim()
        if (n.length < 10) {
            error = "관찰 내용을 한두 문장 이상 적어주세요."
            return
        }
        if (apiKey.isBlank()) {
            error = "설정(오른쪽 위 톱니바퀴)에서 Gemini API 키를 먼저 넣어주세요."
            return
        }
        val d = date
        val w = who.trim()
        error = null
        loading = true
        job = viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) {
                    val raw = Gemini.generate(apiKey, model, RecordSpec.prompt(n, d.toString(), w))
                    RecordSpec.parse(raw)
                }
                result = r
                summary = r.summary
                isExample = false
                resultDate = d
                resultWho = w
            } catch (e: AiException) {
                error = e.message
            } catch (e: kotlinx.coroutines.CancellationException) {
                error = "멈췄어요."
                throw e
            } catch (e: Exception) {
                error = "정리 결과를 읽지 못했어요. 다시 눌러주세요."
            } finally {
                loading = false
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        loading = false
        error = "멈췄어요."
    }
}
