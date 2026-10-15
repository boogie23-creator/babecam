package com.babecam.carenote.data

import android.content.Context

/** 설정은 이 휴대폰 안에만 저장된다 (GitHub 코드에는 키가 없음). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("gemini_api_key", "") ?: ""
        set(v) = sp.edit().putString("gemini_api_key", v.trim()).apply()

    var model: String
        get() = (sp.getString("gemini_model", "") ?: "").ifBlank { DEFAULT_MODEL }
        set(v) = sp.edit().putString("gemini_model", v.trim()).apply()

    companion object {
        /** 구글이 최신 Flash 모델을 가리키도록 유지하는 별칭. 안 되면 설정에서 모델 이름을 바꿀 수 있다. */
        const val DEFAULT_MODEL = "gemini-flash-latest"
    }
}
