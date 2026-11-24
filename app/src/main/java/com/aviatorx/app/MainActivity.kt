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
    var savedRounds by remember { mutableStateOf(loadRounds(prefs.getString("rounds", "") ?: "")) }
    var manualInput by remember { mutableStateOf("") }
    var hashInput by remember { mutableStateOf("") }
    var hashExpected by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Import CSV history or add a multiplier.") }

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
                if (imported.isEmpty()) notice = "No valid multipliers found in this file."
                else {
                    savedRounds = (savedRounds + imported).takeLast(10000)
                    notice = "Imported ${imported.size} rounds. Saved on this phone."
                }
            }.onFailure { notice = "Import failed: ${it.message ?: "file could not be read"}" }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("multiplier\\n")
                    savedRounds.forEach { writer.write("$it\\n") }
                } ?: throw IllegalStateException("Could not create output file")
            }.onSuccess { notice = "Exported ${savedRounds.size} rounds." }
                .onFailure { notice = "Export failed: ${it.message ?: "could not write file"}" }
        }
    }

    val digest = remember(hashInput) { sha256(hashInput) }
    val hashMatches = hashExpected.trim().lowercase() == digest && hashExpected.trim().isNotBlank()
    val average = if (savedRounds.isEmpty()) null else savedRounds.average()
    val lowPct = if (savedRounds.isEmpty()) 0 else
        (savedRounds.count { it < 2.0 } * 100.0 / savedRounds.size).roundToInt()
    val bins = listOf(
        "1.00–1.99x" to savedRounds.count { it < 2.0 },
        "2.00–4.99x" to savedRounds.count { it >= 2.0 && it < 5.0 },
        "5.00–9.99x" to savedRounds.count { it >= 5.0 && it < 10.0 },
        "10x+" to savedRounds.count { it >= 10.0 }
    )
    val maxBin = (bins.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Bg),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                Column {
                    Text("AVIATOR X", color = Accent, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("ROUND ANALYSIS • SHA-256 VERIFICATION", color = Muted, fontSize = 9.sp, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("One screen. Your data stays on this phone.", color = Muted, fontSize = 11.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { importPicker.launch(arrayOf("text/*", "application/vnd.ms-excel")) }, modifier = Modifier.weight(1f)) {
                        Text("IMPORT CSV", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { exportPicker.launch("aviator-x-rounds.csv") }, enabled = savedRounds.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("EXPORT", fontSize = 11.sp)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add multiplier (example: 2.35)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = {
                    val value = manualInput.trim().removeSuffix("x").toDoubleOrNull()
                    if (value == null || !value.isFinite() || value < 1.0) notice = "Enter a valid multiplier of 1.00x or higher."
                    else {
                        savedRounds = (savedRounds + value).takeLast(10000)
                        manualInput = ""
                        notice = "Round saved."
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("ADD ROUND") }
                Text(notice, color = Accent, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("ROUNDS", savedRounds.size.toString(), "Saved locally", Modifier.weight(1f))
                    StatCard("AVERAGE", average?.let { "%.2fx".format(it) } ?: "—", "Recorded sample", Modifier.weight(1f))
                    StatCard("BELOW 2X", if (savedRounds.isEmpty()) "—" else "$lowPct%", "Historical share", Modifier.weight(1f))
                }
            }
            item { Text("ROUND DISTRIBUTION", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            items(bins) { (label, count) ->
                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(11.dp)) {
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
            item { Text("SHA-256 HASH CHECK", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            item {
                Text("Check a revealed seed against its published hash. A hash cannot be decrypted to reveal a future result.", color = Muted, fontSize = 11.sp)
                OutlinedTextField(value = hashInput, onValueChange = { hashInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Revealed seed / text") }, minLines = 2)
                OutlinedTextField(value = hashExpected, onValueChange = { hashExpected = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Published SHA-256 hash") }, minLines = 2)
                Text("Calculated: ${if (hashInput.isBlank()) "—" else digest}", color = Color.White, fontSize = 10.sp)
                if (hashExpected.isNotBlank()) {
                    Text(if (hashMatches) "✓ HASH MATCH" else "✕ HASH DOES NOT MATCH", color = if (hashMatches) Good else Bad, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RECENT ROUNDS", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("${savedRounds.size} saved", color = Muted, fontSize = 10.sp)
                }
            }
            items(savedRounds.takeLast(20).reversed()) { value ->
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(9.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recorded multiplier", color = Muted, modifier = Modifier.weight(1f), fontSize = 11.sp)
                    Text("%.2fx".format(value), color = if (value >= 2.0) Good else Color.White, fontWeight = FontWeight.Bold)
                }
            }
            item {
                OutlinedButton(onClick = {
                    savedRounds = emptyList()
                    notice = "All saved rounds cleared."
                }, enabled = savedRounds.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("CLEAR ROUND HISTORY") }
                Text("Important: this app analyzes recorded history and verifies hashes. It cannot predict independent future crash multipliers or guarantee winnings. Never enter passwords, OTPs or gambling account credentials.", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 9.sp)
        }
    }
}

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

fun parseRounds(raw: String): List<Double> {
    if (raw.isBlank()) return emptyList()
    val lines = raw.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()
    val delimiters = charArrayOf(',', ';', '\\t')
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

fun loadRounds(raw: String): List<Double> = raw.split(',')
    .mapNotNull { it.trim().toDoubleOrNull()?.takeIf { value -> value >= 1.0 && value.isFinite() } }
    .takeLast(10000)
