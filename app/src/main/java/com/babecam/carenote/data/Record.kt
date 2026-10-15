package com.babecam.carenote.data

import org.json.JSONArray
import org.json.JSONObject

data class AreaEntry(
    val area: String,
    val status: String,
    val content: String,
    val action: String,
)

data class RecordResult(
    val reportNeeded: Boolean,
    val reportReason: String,
    val areas: List<AreaEntry>,
    val summary: String,
    val missing: List<String>,
)

/** 상태변화기록지 형식(영역·상태 구분)과 AI에게 줄 지시문 */
object RecordSpec {
    /** 상태변화기록지 4개 영역과 각 영역에 담을 내용 */
    val AREA_GUIDE = linkedMapOf(
        "신체상태" to "일상생활 동작, 보행·이동(보조기구), 배설, 수면, 피부·위생, 낙상",
        "영양상태" to "식사량·식사형태, 수분 섭취, 씹기·삼킴, 체중 변화",
        "질병상태" to "통증 호소, 질병 관련 증상, 혈압·혈당 등 측정값, 투약, 병원 진료·입원, 인지(기억력·판단) 변화",
        "개별욕구" to "어르신·보호자가 원하거나 요청한 것, 기분·정서, 여가·사회활동, 보호자 연락 내용",
    )
    val AREAS = AREA_GUIDE.keys.toList()
    const val CHANGED = "변화 있음"
    const val SAME = "평소와 같음"
    const val NONE = "언급 없음"
    private val STATUSES = setOf(CHANGED, SAME, NONE)

    fun prompt(note: String, date: String, who: String): String = """
당신은 한국 노인장기요양 재가(방문요양) 현장의 기록 도우미입니다. 요양보호사가 말로 남긴 관찰 메모를 '상태변화기록지' 형식으로 정리하세요.

[규칙]
- 메모에 있는 사실만 씁니다. 없는 내용을 지어내거나 추측하지 않습니다.
- 병명 추정이나 의학적 판단을 쓰지 않습니다. 관찰한 행동·양·말씀만 객관적으로 씁니다.
- 양은 숫자로 바꿀 수 있으면 바꿉니다(반 공기 → 1/2공기, 세 번 → 3회).
- 문체는 기록지 문체("~드심", "~하심", "~함")로 통일합니다.
- 실명·연락처가 메모에 있으면 결과에서 빼고 "보호자(딸)"처럼 관계로만 씁니다.
- areas에는 정확히 다음 4개 영역을 이 순서대로 모두 넣습니다. 각 영역에 담을 내용:
${AREA_GUIDE.entries.joinToString("\n") { "  · " + it.key + ": " + it.value }}
- 한 영역에 여러 내용이 있으면 content에 이어서 씁니다.
- status는 "$CHANGED"(평소·이전과 다르거나 주의가 필요한 내용), "$SAME", "$NONE" 중 하나입니다. 메모에 없는 영역은 "$NONE", content는 빈 문자열.
- action은 메모에 나온 조치만 적습니다(권유, 도포, 보호자 연락 등). 없으면 빈 문자열.
- reportNeeded는 통증 호소, 보행·식사·배설·수면의 뚜렷한 변화, 낙상, 피부 이상, 질병 증상·투약 변화, 정서 급변이 있으면 true입니다.
- summary는 스마트장기요양 앱 특이사항 칸에 붙여넣을 3~6문장 서술문입니다.
- missing은 다음 방문 때 추가로 확인하면 좋을 점 2~4개(메모에 없는 것)를 짧은 구절로 씁니다.

[기록 정보]
날짜: ${date.ifBlank { "미기재" }}
어르신: ${who.ifBlank { "미기재" }}

[관찰 메모]
$note

다음 형태의 JSON 하나만 답하세요(다른 글 없이):
{"reportNeeded":true,"reportReason":"한두 문장","areas":[{"area":"신체상태","status":"$CHANGED","content":"...","action":"..."}],"summary":"...","missing":["..."]}
""".trimIndent()

    /** AI 답변(JSON 문자열)을 결과로 변환. 코드펜스나 앞뒤 문장이 섞여도 첫 { ~ 마지막 } 만 읽는다. */
    fun parse(raw: String): RecordResult {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw IllegalStateException("정리 결과를 읽지 못했어요. 다시 눌러주세요.")
        val o = JSONObject(raw.substring(start, end + 1))

        val byArea = HashMap<String, AreaEntry>()
        val arr = o.optJSONArray("areas") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val a = arr.optJSONObject(i) ?: continue
            val name = a.optString("area").trim()
            val st = a.optString("status").trim().let { if (it in STATUSES) it else NONE }
            byArea[name] = AreaEntry(name, st, a.optString("content").trim(), a.optString("action").trim())
        }
        val areas = AREAS.map { byArea[it] ?: AreaEntry(it, NONE, "", "") }

        val missing = ArrayList<String>()
        o.optJSONArray("missing")?.let { m ->
            for (i in 0 until m.length()) m.optString(i).trim().takeIf { it.isNotEmpty() }?.let(missing::add)
        }
        return RecordResult(
            reportNeeded = o.optBoolean("reportNeeded", false),
            reportReason = o.optString("reportReason").trim(),
            areas = areas,
            summary = o.optString("summary").trim(),
            missing = missing,
        )
    }

    /** 앱을 처음 열었을 때 보여주는 예시 (실제 어르신 정보 아님) */
    const val EXAMPLE_NOTE =
        "아침에 죽 반 공기 정도 드시고 반찬은 거의 안 드심. 물은 권해드려서 두 컵 드심. 어젯밤 화장실을 세 번 가셔서 잠을 설쳤다고 하심. " +
            "오른쪽 무릎이 아프다고 하셔서 오늘은 보행기 쓰심, 지난주엔 지팡이로 다니셨음. 엉덩이 피부 빨간 곳 없음. " +
            "기분은 좋으셨고 옛날 노래 같이 부르심. 딸이 다음 주에 병원 모시고 간다고 함."

    val EXAMPLE = RecordResult(
        reportNeeded = true,
        reportReason = "보행 보조기구가 지팡이에서 보행기로 바뀌었고 무릎 통증을 호소하심. 야간 배뇨 3회로 수면 방해.",
        areas = listOf(
            AreaEntry("신체상태", CHANGED, "오른쪽 무릎 통증으로 보행기 사용(지난주에는 지팡이 사용). 전날 밤 화장실 3회 이용하여 잠을 설쳤다고 하심. 엉덩이 부위 피부 발적 없음.", ""),
            AreaEntry("영양상태", CHANGED, "아침 죽 1/2공기 섭취, 반찬은 거의 드시지 않음. 수분은 권유하여 2컵 섭취.", "수분 섭취 권유"),
            AreaEntry("질병상태", CHANGED, "오른쪽 무릎 통증 호소. 보호자(딸)가 다음 주 병원 동행 예정이라고 함.", ""),
            AreaEntry("개별욕구", SAME, "기분 좋으셨고 옛날 노래를 함께 부르심.", "정서지원(노래 부르기)"),
        ),
        summary = "아침 식사로 죽 1/2공기를 드시고 반찬은 거의 드시지 않음. 수분 섭취를 권유하여 물 2컵 드심. " +
            "전날 밤 화장실을 3회 이용하여 잠을 설쳤다고 하심. 오른쪽 무릎 통증을 호소하시며 지난주 지팡이에서 이번 주 보행기로 보행 보조기구가 바뀜. " +
            "엉덩이 부위 피부 발적 없음. 옛날 노래를 함께 부르며 밝은 모습 보이심. 보호자(딸)가 다음 주 병원 동행 예정이라고 함.",
        missing = listOf(
            "무릎 통증이 언제부터인지, 붓기나 열감이 있는지",
            "야간 배뇨가 최근 늘었는지, 낮 동안 수분 섭취량",
            "식사량 감소가 며칠째인지",
            "통증·보행 변화를 사회복지사와 보호자에게 알렸는지",
        ),
    )
}
