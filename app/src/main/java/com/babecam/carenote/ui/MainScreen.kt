package com.babecam.carenote.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babecam.carenote.data.AreaEntry
import com.babecam.carenote.data.RecordSpec
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private fun fmtDate(d: LocalDate): String =
    "${d.year}. ${d.monthValue}. ${d.dayOfMonth}. (${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

@Composable
fun MainScreen(vm: MainViewModel, onSettings: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) vm.appendNote(text)
        }
    }
    fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "어르신 상태를 말씀해 주세요")
        try {
            speech.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "이 휴대폰에서는 음성 입력을 쓸 수 없어요. 키보드 마이크를 이용해 주세요.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 머리말
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("재가 방문요양 · 요양보호사용", color = cs.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("상태변화 기록 도우미", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)
                Text(
                    "오늘 본 어르신 모습을 말하면 상태변화기록지 항목별로 나누고 특이사항 문장으로 정리해 드려요.",
                    fontSize = 14.sp, color = cs.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "설정") }
        }

        if (vm.apiKey.isBlank()) {
            Surface(color = cs.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "처음 한 번, 설정에서 Gemini API 키를 넣어야 AI 정리가 작동해요.",
                        Modifier.weight(1f), fontSize = 14.sp, color = cs.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onSettings) { Text("설정") }
                }
            }
        }

        // 입력 카드
        Sheet {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val d = vm.date
                        DatePickerDialog(context, { _, y, m, day -> vm.date = LocalDate.of(y, m + 1, day) },
                            d.year, d.monthValue - 1, d.dayOfMonth).show()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(fmtDate(vm.date), fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = vm.who, onValueChange = { if (it.length <= 20) vm.who = it },
                    label = { Text("어르신(이니셜)") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = vm.note, onValueChange = { vm.note = it },
                label = { Text("오늘 관찰한 내용") },
                placeholder = { Text("식사, 화장실, 잠, 걷기, 아프다고 하신 곳, 기분, 피부, 보호자 연락 등") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            FilledTonalButton(
                onClick = { startSpeech() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text("말로 입력하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.summarize() },
                    enabled = !vm.loading,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("기록지로 정리하기", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                if (vm.loading) {
                    OutlinedButton(onClick = { vm.stop() }, Modifier.height(54.dp), shape = RoundedCornerShape(10.dp)) { Text("멈추기") }
                } else {
                    OutlinedButton(onClick = { vm.newRecord() }, Modifier.height(54.dp), shape = RoundedCornerShape(10.dp)) { Text("새로 쓰기") }
                }
            }
            if (vm.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("정리하는 중이에요… 10~30초 걸릴 수 있어요.", fontSize = 14.sp, color = cs.onSurfaceVariant)
            }
            vm.error?.let { Text(it, fontSize = 14.sp, color = cs.error) }
        }

        // 결과 카드
        val r = vm.result
        val st = LocalStatus.current
        Sheet {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("상태변화기록지", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Pill(
                    if (vm.isExample) "예시 결과" else "AI 정리 초안",
                    if (vm.isExample) st.warn else st.calm,
                    if (vm.isExample) st.warnSoft else st.calmSoft,
                )
            }
            val meta = buildList {
                add(fmtDate(vm.resultDate))
                if (vm.resultWho.isNotBlank()) add(vm.resultWho)
                if (vm.isExample) add("예시 입력으로 만든 결과예요")
            }.joinToString("  ·  ")
            Text(meta, fontSize = 13.sp, color = cs.onSurfaceVariant)

            Surface(color = if (r.reportNeeded) st.alertSoft else st.calmSoft, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (r.reportNeeded) "사회복지사·보호자에게 알리세요" else "즉시 보고할 변화는 보이지 않아요",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (r.reportNeeded) st.alert else st.calm,
                    )
                    if (r.reportReason.isNotBlank()) {
                        Text(r.reportReason, fontSize = 14.sp, color = if (r.reportNeeded) st.alert else st.calm)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                r.areas.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(color = cs.outline)
                    AreaRow(a)
                }
            }

            Text("특이사항 문장", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("고쳐 쓴 뒤 복사해서 스마트장기요양 앱에 붙여넣으세요.", fontSize = 13.sp, color = cs.onSurfaceVariant)
            OutlinedTextField(
                value = vm.summary, onValueChange = { vm.summary = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 28.sp),
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(vm.summary))
                    Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("문장 복사", fontWeight = FontWeight.SemiBold)
            }

            if (r.missing.isNotEmpty()) {
                Text("다음 방문 때 확인해 보세요", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.missing.forEach { Text("•  $it", fontSize = 14.sp) }
                }
            }
        }

        Text(
            "입력한 내용은 휴대폰에 저장되지 않아요. 어르신 실명·주민번호·주소·연락처는 넣지 마세요. " +
                "AI가 정리한 문장은 초안이니 실제로 본 내용과 다르면 고쳐 쓰고, 건강 판단은 간호사·의료진에게 확인하세요.",
            fontSize = 12.sp, color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, cs.outline, RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AreaRow(a: AreaEntry) {
    val cs = MaterialTheme.colorScheme
    val st = LocalStatus.current
    val changed = a.status == RecordSpec.CHANGED
    val (fg, bg) = when (a.status) {
        RecordSpec.CHANGED -> st.warn to st.warnSoft
        RecordSpec.SAME -> cs.primary to cs.primaryContainer
        else -> st.calm to st.calmSoft
    }
    Row(
        Modifier.fillMaxWidth().background(if (changed) cs.surfaceVariant else cs.surface)
    ) {
        Box(Modifier.width(4.dp).heightIn(min = 56.dp).background(if (changed) st.warn else Color.Transparent))
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(84.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(a.area, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Pill(a.status, fg, bg)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (a.content.isNotBlank()) Text(a.content, fontSize = 14.sp)
                else Text("말씀하신 내용이 없어요", fontSize = 13.sp, color = cs.onSurfaceVariant)
                if (a.action.isNotBlank()) Text("조치  ${a.action}", fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }
    }
}
