package com.babecam.carenote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babecam.carenote.data.Prefs

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val cs = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.model) }
    var show by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(cs.background).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
            Text("설정", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Surface(color = cs.surface, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gemini API 키", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "1. 아래 버튼으로 Google AI Studio에 들어가 구글 계정으로 로그인합니다.\n" +
                        "2. 'Get API key' → 'Create API key'를 눌러 키를 만듭니다.\n" +
                        "3. 만든 키를 복사해서 아래 칸에 붙여넣고 저장합니다.",
                    fontSize = 14.sp, lineHeight = 22.sp,
                )
                OutlinedButton(onClick = { uri.openUri("https://aistudio.google.com/apikey") }) {
                    Text("Google AI Studio 열기")
                }
                OutlinedTextField(
                    value = key, onValueChange = { key = it; saved = false },
                    label = { Text("API 키") }, singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "키 보기")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model, onValueChange = { model = it; saved = false },
                    label = { Text("모델 이름") }, singleLine = true,
                    supportingText = { Text("보통은 그대로 두세요. 기본값: ${Prefs.DEFAULT_MODEL}") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { vm.saveSettings(key, model); saved = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                ) { Text(if (saved) "저장했어요" else "저장", fontWeight = FontWeight.SemiBold) }
                Text("키는 이 휴대폰 안에만 저장되고 다른 곳으로 보내지지 않아요(구글 AI 요청에만 사용).", fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }

        Surface(color = LocalStatus.current.warnSoft, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("개인정보 주의", fontWeight = FontWeight.Bold, color = LocalStatus.current.warn)
                Text(
                    "Gemini 무료 요금제는 입력한 내용이 구글의 서비스 개선(학습)에 쓰일 수 있어요. " +
                        "어르신 실명·주민번호·주소·연락처는 절대 넣지 말고 '김○○'처럼 이니셜만 쓰세요. " +
                        "실제 센터에서 쓰려면 결제(유료) 계정으로 바꾸는 것을 권해요.",
                    fontSize = 14.sp, lineHeight = 22.sp, color = LocalStatus.current.warn,
                )
            }
        }
    }
}
