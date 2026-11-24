package com.aviatorx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest
import kotlin.math.roundToInt

private val Bg = Color(0xFF080D18)
private val Panel = Color(0xFF111B2B)
private val Accent = Color(0xFFFFC247)
private val Muted = Color(0xFF9AA9BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AviatorApp() }
    }
}

@Composable
fun AviatorApp() {
    var tab by remember { mutableStateOf(0) }
    var csvText by remember { mutableStateOf("") }
    var hashInput by remember { mutableStateOf("") }
    var hashExpected by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Import historical rounds to begin analysis.") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { csvText = it; notice = "File imported. Review the parsed round data." }
             .onFailure { notice = "Could not read that file." }
        }
    }
    val rounds = remember(csvText) { parseRounds(csvText) }
    val digest = remember(hashInput) { sha256(hashInput) }
    val hashMatches = hashExpected.trim().lowercase() == digest && hashExpected.isNotBlank()

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AVIATOR X", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("FAIRNESS • DATA • RESEARCH", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Text("v0.1.0", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Overview", "Analyzer", "SHA-256").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(14.dp))
            when (tab) {
                0 -> {
                    StatCard("RESEARCH STATUS", "DATA-DRIVEN", "No live prediction is claimed. Secure future seeds cannot be recovered from their hashes.")
                    Spacer(Modifier.height(12.dp))
                    StatCard("ROUNDS LOADED", rounds.size.toString(), "Import a CSV containing one crash multiplier per row, or a column named multiplier / crash.")
                    Spacer(Modifier.height(12.dp))
                    StatCard("IMPORTANT", "NO GUARANTEED SIGNALS", "Past multipliers do not reliably reveal the next independent crash result.")
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("OPEN ROUND ANALYZER") }
                }
                1 -> {
                    Text("HISTORICAL ROUND ANALYZER", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Import CSV data. Values are analyzed as historical observations only.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { picker.launch(arrayOf("text/*", "application/vnd.ms-excel", "text/csv")) }, modifier = Modifier.fillMaxWidth()) { Text("IMPORT CSV FILE") }
                    Spacer(Modifier.height(8.dp))
                    Text(notice, color = Accent, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    StatCard("VALID MULTIPLIERS", rounds.size.toString(), "Rows parsed from the imported file.")
                    StatCard("AVERAGE", if (rounds.isEmpty()) "—" else "%.3fx".format(rounds.average()), "Arithmetic mean; not a forecast.")
                    StatCard("MEDIAN", if (rounds.isEmpty()) "—" else "%.3fx".format(median(rounds)), "Middle historical observation.")
                    StatCard("BELOW 2.00x", if (rounds.isEmpty()) "—" else "${(rounds.count { it < 2.0 } * 100.0 / rounds.size).roundToInt()}%", "Share of imported rounds below 2.00x.")
                    StatCard("2.00x OR ABOVE", if (rounds.isEmpty()) "—" else "${(rounds.count { it >= 2.0 } * 100.0 / rounds.size).roundToInt()}%", "Share of imported rounds at or above 2.00x.")
                    Spacer(Modifier.height(8.dp))
                    if (rounds.isNotEmpty()) {
                        Text("LATEST IMPORTED VALUES", color = Muted, fontSize = 11.sp)
                        rounds.takeLast(8).reversed().forEach { Text("• %.2fx".format(it), color = Color.White, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }
                else -> {
                    Text("SHA-256 COMMITMENT CHECK", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Enter a revealed seed (or test string) and its published SHA-256 hash. This verifies a match; it does not decrypt a hidden seed.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("SEED / INPUT", color = Accent, fontSize = 11.sp)
                    OutlinedTextField(value = hashInput, onValueChange = { hashInput = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter revealed seed") }, minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Text("PUBLISHED SHA-256 HASH", color = Accent, fontSize = 11.sp)
                    OutlinedTextField(value = hashExpected, onValueChange = { hashExpected = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Paste 64-character hash") }, minLines = 2)
                    Spacer(Modifier.height(12.dp))
                    StatCard("CALCULATED SHA-256", if (hashInput.isBlank()) "—" else digest, "Computed locally on this device.")
                    if (hashExpected.isNotBlank()) {
                        Text(if (hashMatches) "✓ HASH MATCH" else "✕ NO MATCH", color = if (hashMatches) Color(0xFF69D6A4) else Color(0xFFFF7777), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Never enter gambling account passwords, OTPs, cookies or session tokens in this app.", color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Independent research tool • No betting advice", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, detail: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

fun parseRounds(raw: String): List<Double> {
    if (raw.isBlank()) return emptyList()
    val lines = raw.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()
    val first = lines.first().split(',', ';', '\t')
    val headerIndex = first.indexOfFirst { it.trim().lowercase() in listOf("multiplier", "crash", "result", "coefficient") }
    val values = if (headerIndex >= 0) lines.drop(1).mapNotNull { it.split(',', ';', '\t').getOrNull(headerIndex) }
                 else lines.flatMap { it.split(',', ';', '\t') }
    return values.mapNotNull { token ->
        token.trim().replace("x", "", ignoreCase = true).toDoubleOrNull()?.takeIf { it >= 1.0 && it.isFinite() }
    }
}

fun median(values: List<Double>): Double {
    val s = values.sorted()
    return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
}
