package com.babecam.carenote.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AiException(message: String) : Exception(message)

/** Google Gemini API (generateContent) 호출 */
object Gemini {
    fun generate(apiKey: String, model: String, prompt: String): String {
        if (apiKey.isBlank()) throw AiException("설정에서 Gemini API 키를 먼저 넣어주세요.")
        val m = URLEncoder.encode(model.ifBlank { Prefs.DEFAULT_MODEL }, "UTF-8")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent")

        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .put("generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.2))
            .toString()

        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 90_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", apiKey.trim())
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw AiException(errorMessage(code, text))
            return extractText(text)
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiException("인터넷 연결을 확인해 주세요.")
        } finally {
            conn.disconnect()
        }
    }

    private fun extractText(raw: String): String {
        val o = JSONObject(raw)
        val cands = o.optJSONArray("candidates")
        if (cands == null || cands.length() == 0) {
            val block = o.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw AiException(if (block.isNotEmpty()) "AI가 이 내용을 정리하지 않았어요. 메모를 조금 바꿔서 다시 해보세요." else "AI 답변이 비어 있어요. 다시 눌러주세요.")
        }
        val parts = cands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            if (p.optBoolean("thought", false)) continue
            sb.append(p.optString("text"))
        }
        if (sb.isBlank()) throw AiException("AI 답변이 비어 있어요. 다시 눌러주세요.")
        return sb.toString()
    }

    private fun errorMessage(code: Int, body: String): String {
        val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message").orEmpty() }.getOrDefault("")
        return when {
            msg.contains("API key not valid", true) || msg.contains("API_KEY_INVALID", true) ->
                "API 키가 올바르지 않아요. 설정에서 키를 다시 확인해 주세요."
            code == 404 -> "모델 이름을 찾을 수 없어요. 설정에서 모델 이름을 확인해 주세요."
            code == 429 -> "무료 사용량을 넘었어요. 잠시 뒤(또는 내일) 다시 해보세요."
            code == 403 -> "이 키로는 사용할 수 없어요. 키 권한을 확인해 주세요."
            code >= 500 -> "AI 서버가 바빠요. 잠시 뒤 다시 눌러주세요."
            else -> "AI 요청이 실패했어요 (오류 $code)."
        }
    }
}
