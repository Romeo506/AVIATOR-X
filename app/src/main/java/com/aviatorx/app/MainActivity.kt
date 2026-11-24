package com.aviatorx.app

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest
import kotlin.math.roundToInt

private val Bg = Color(0xFF080D18)
private val Panel = Color(0xFF111B2B)
private val Accent = Color(0xFFFFC247)
private val Muted = Color(0xFF9AA9BD)
private val Good = Color(0xFF69D6A4)
private val Bad = Color(0xFFFF7777)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AviatorApp() }
    }
}

@Composable
fun AviatorApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aviator_x_local", 0) }
    var tab by remember { mutableIntStateOf(0) }
    var savedRounds by remember { mutableStateOf(loadRounds(prefs.getString("rounds", "") ?: "")) }
    var manualInput by remember { mutableStateOf("") }
    var hashInput by remember { mutableStateOf("") }
    var hashExpected by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Import a CSV or add a round manually.") }

    SideEffect {
        prefs.edit().putString("rounds", savedRounds.joinToString(",")).apply()
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Could not open selected file")
            }.onSuccess { raw ->
                val imported = parseRounds(raw)
                if (imported.isEmpty()) notice = "No valid multipliers found. Check the CSV column name and values."
                else {
                    savedRounds = (savedRounds + imported).takeLast(10000)
                    notice = "Imported and saved ${imported.size} rounds on this phone."
                }
            }.onFailure { notice = "Import failed: ${it.message ?: "file could not be read"}" }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("multiplier\n")
                    savedRounds.forEach { writer.write("$it\n") }
                } ?: throw IllegalStateException("Could not create output file")
            }.onSuccess { notice = "Exported ${savedRounds.size} rounds to CSV." }
                .onFailure { notice = "Export failed: ${it.message ?: "could not write file"}" }
        }
    }

    val digest = remember(hashInput) { sha256(hashInput) }
    val hashMatches = hashExpected.trim().lowercase() == digest && hashExpected.trim().isNotBlank()
    val average = if (savedRounds.isEmpty()) null else savedRounds.average()
    val medianValue = if (savedRounds.isEmpty()) null else median(savedRounds)
    val lowPct = if (savedRounds.isEmpty()) 0 else (savedRounds.count { it < 2.0 } * 100.0 / savedRounds.size).roundToInt()
    val bins = listOf(
        "1.00–1.99x" to savedRounds.count { it < 2.0 },
        "2.00–4.99x" to savedRounds.count { it >= 2.0 && it < 5.0 },
        "5.00–9.99x" to savedRounds.count { it >= 5.0 && it < 10.0 },
        "10x+" to savedRounds.count { it >= 10.0 }
    )
    val maxBin = (bins.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AVIATOR X", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("ROUND DATA • FAIRNESS • RESEARCH", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Text("v0.2.0", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Home", "Analyzer", "History", "SHA-256").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            Spacer(Modifier.height(10.dp))
            when (tab) {
                0 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        item { StatCard("LOCAL ROUND RECORDS", savedRounds.size.toString(), "Saved privately on this phone. No account login or cloud upload.") }
                        item { StatCard("RESEARCH MODE", "ACTIVE", "This app describes recorded data. It does not predict the next crash point.") }
                        item { StatCard("LOW MULTIPLIER SHARE", if (savedRounds.isEmpty()) "—" else "$lowPct%", "Observed share of saved rounds below 2.00x; not a future probability guarantee.") }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                    Text("QUICK ACTIONS", color = Accent, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(10.dp))
                                    Button(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("OPEN ANALYZER") }
                                    OutlinedButton(onClick = { importPicker.launch(arrayOf("text/*", "application/vnd.ms-excel")) }, modifier = Modifier.fillMaxWidth()) { Text("IMPORT ROUND CSV") }
                                    OutlinedButton(onClick = { exportPicker.launch("aviator-x-rounds.csv") }, enabled = savedRounds.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("EXPORT MY DATA") }
                                }
                            }
                        }
                        item { Text(notice, color = Accent, fontSize = 12.sp) }
                        item { Text("Important: historical patterns cannot reliably identify an independent future round. Never risk money based on this analysis.", color = Muted, fontSize = 11.sp) }
                    }
                }
                1 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        item { Text("HISTORICAL ANALYZER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                        item { Text("All calculations use only the rounds stored on this device.", color = Muted, fontSize = 12.sp) }
                        item { StatCard("ROUNDS ANALYZED", savedRounds.size.toString(), "Maximum retained locally: 10,000 rounds.") }
                        item { StatCard("AVERAGE", average?.let { "%.3fx".format(it) } ?: "—", "Arithmetic mean of recorded outcomes.") }
                        item { StatCard("MEDIAN", medianValue?.let { "%.3fx".format(it) } ?: "—", "Middle value after sorting recorded outcomes.") }
                        item { StatCard("BELOW 2.00x", if (savedRounds.isEmpty()) "—" else "$lowPct%", "Observed frequency in your saved sample.") }
                        item { Text("OUTCOME DISTRIBUTION", color = Accent, fontWeight = FontWeight.Bold) }
                        items(bins) { (label, count) ->
                            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                    Text("$count  (${if (savedRounds.isEmpty()) 0 else (count * 100.0 / savedRounds.size).roundToInt()}%)", color = Muted, fontSize = 11.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(7.dp).background(Bg, RoundedCornerShape(8.dp))) {
                                    Box(Modifier.fillMaxWidth(count.toFloat() / maxBin).fillMaxHeight().background(Accent, RoundedCornerShape(8.dp)))
                                }
                            }
                        }
                        item {
                            val streak = currentLowStreak(savedRounds)
                            StatCard("LATEST LOW-ROUND STREAK", streak.toString(), "Consecutive recorded rounds below 2.00x at the end of your imported list. Not a signal.")
                        }
                        item { Text("Latest recorded outcomes", color = Accent, fontWeight = FontWeight.Bold) }
                        items(savedRounds.takeLast(12).reversed()) { value ->
                            Text("•  %.2fx".format(value), color = Color.White, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                2 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        item { Text("ROUND HISTORY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                        item {
                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = { manualInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Add multiplier (e.g. 2.35)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                        item {
                            Button(onClick = {
                                val value = manualInput.trim().removeSuffix("x").toDoubleOrNull()
                                if (value == null || !value.isFinite() || value < 1.0) notice = "Enter a valid multiplier of 1.00x or higher."
                                else {
                                    savedRounds = (savedRounds + value).takeLast(10000)
                                    manualInput = ""
                                    notice = "Round saved locally."
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("SAVE ROUND") }
                        }
                        item { Text(notice, color = Accent, fontSize = 12.sp) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { exportPicker.launch("aviator-x-rounds.csv") }, enabled = savedRounds.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("EXPORT CSV") }
                                OutlinedButton(onClick = {
                                    savedRounds = emptyList()
                                    notice = "Local round history cleared."
                                }, enabled = savedRounds.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("CLEAR ALL") }
                            }
                        }
                        item { Text("Newest first • ${savedRounds.size} saved", color = Muted, fontSize = 11.sp) }
                        items(savedRounds.indices.reversed().take(200).toList()) { index ->
                            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", color = Muted, modifier = Modifier.weight(1f), fontSize = 11.sp)
                                Text("%.2fx".format(savedRounds[index]), color = if (savedRounds[index] >= 2.0) Good else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        item { Text("SHA-256 COMMITMENT VERIFIER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                        item { Text("Paste a revealed seed and its published hash to check whether they match. A hash is not encryption and cannot reveal a hidden future seed.", color = Muted, fontSize = 12.sp) }
                        item {
                            OutlinedTextField(value = hashInput, onValueChange = { hashInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Revealed seed / test text") }, minLines = 2)
                        }
                        item {
                            OutlinedTextField(value = hashExpected, onValueChange = { hashExpected = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Published SHA-256 hash") }, minLines = 2)
                        }
                        item { StatCard("CALCULATED HASH", if (hashInput.isBlank()) "—" else digest, "Calculated locally on this Android device.") }
                        if (hashExpected.isNotBlank()) {
                            item { Text(if (hashMatches) "✓ HASH MATCH — commitment verified" else "✕ NO MATCH — check the seed and hash", color = if (hashMatches) Good else Bad, fontWeight = FontWeight.Bold) }
                        }
                        item {
                            OutlinedButton(onClick = {
                                hashInput = ""
                                hashExpected = ""
                            }, modifier = Modifier.fillMaxWidth()) { Text("CLEAR VERIFICATION") }
                        }
                        item { Text("Security: never enter gambling passwords, OTPs, cookies, session tokens or account credentials.", color = Muted, fontSize = 11.sp) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Independent research tool • No betting advice • Data stays on device", color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, detail: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(14.dp)) {
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
    val delimiters = charArrayOf(',', ';', '\t')
    val first = lines.first().split(*delimiters)
    val headerIndex = first.indexOfFirst {
        it.trim().trim('"').lowercase() in listOf("multiplier", "crash", "result", "coefficient")
    }
    val values = if (headerIndex >= 0) {
        lines.drop(1).mapNotNull { it.split(*delimiters).getOrNull(headerIndex) }
    } else {
        lines.flatMap { it.split(*delimiters) }
    }
    return values.mapNotNull { token ->
        token.trim().trim('"').replace("x", "", ignoreCase = true).toDoubleOrNull()
            ?.takeIf { it >= 1.0 && it.isFinite() }
    }
}

fun median(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
}

fun currentLowStreak(values: List<Double>): Int {
    var count = 0
    for (value in values.asReversed()) {
        if (value < 2.0) count++ else break
    }
    return count
}

fun loadRounds(raw: String): List<Double> = raw.split(',')
    .mapNotNull { it.trim().toDoubleOrNull()?.takeIf { value -> value >= 1.0 && value.isFinite() } }
    .takeLast(10000)
