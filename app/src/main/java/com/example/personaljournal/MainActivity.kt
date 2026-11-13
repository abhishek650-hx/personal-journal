




package com.example.personaljournal

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.personaljournal.data.JournalDao
import com.example.personaljournal.data.JournalDatabase
import com.example.personaljournal.data.JournalEntry
import com.example.personaljournal.voice.KokoroVoice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import org.json.JSONArray
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = JournalDatabase.get(this).journalDao()

        setContent {
            MaterialTheme {
                JournalScreen(dao)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalScreen(dao: JournalDao) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val entries by dao.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var summary by remember { mutableStateOf<String?>(null) }
    var summaryLoading by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }

    // Two app windows: dashboard and previous records.
    var showPreviousRecords by remember { mutableStateOf(false) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var datePendingDeletion by remember { mutableStateOf<String?>(null) }

    // CLOUDFLARE SETTINGS

    val preferences = remember {
        context.getSharedPreferences("journal_settings", 0)
    }

    var accountId by remember {
        mutableStateOf(
            preferences.getString("cloudflare_account_id", "") ?: ""
        )
    }

    var apiToken by remember {
        mutableStateOf(
            preferences.getString("cloudflare_token", "") ?: ""
        )
    }


    // ANDROID TEXT TO SPEECH

    val voice = remember {
        KokoroVoice(context.applicationContext)
    }

    var voiceLoading by remember { mutableStateOf(false) }
    var voiceReady by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(voice) {
        onDispose {
            voice.release()
        }
    }

    // INITIALIZE ANDROID TTS

    LaunchedEffect(voice) {

        voiceLoading = true
        voiceMessage = "Preparing Android voice..."

        try {
            voice.initialize()

            voiceReady = true
            voiceMessage = "Voice ready."

        } catch (e: Exception) {

            voiceReady = false
            voiceMessage =
                "Voice initialization failed: ${e.message ?: e.javaClass.simpleName}"

        } finally {
            voiceLoading = false
        }
    }


    // SPEECH RECOGNITION

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()

        if (!spoken.isNullOrBlank()) {

            scope.launch {
                dao.insert(
                    JournalEntry(
                        text = spoken,
                        createdAt = System.currentTimeMillis()
                    )
                )

                Toast.makeText(
                    context,
                    "Record saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // TODAY'S ENTRIES

    val todayDate = remember {
        SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(Date())
    }

    val todayEntries = entries.filter {
        SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(Date(it.createdAt)) == todayDate
    }


    Scaffold(

        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showPreviousRecords) {
                        IconButton(
                            onClick = {
                                if (selectedDateKey != null) {
                                    selectedDateKey = null
                                } else {
                                    showPreviousRecords = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    Text(
                        when {
                            selectedDateKey != null -> formatDateTitle(selectedDateKey!!)
                            showPreviousRecords -> "Previous Records"
                            else -> "Personal Journal"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (!showPreviousRecords) {
                        IconButton(onClick = { showPreviousRecords = true }) {
                            Icon(Icons.Default.History, contentDescription = "Previous records")
                        }
                        IconButton(onClick = { showKeyDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showPreviousRecords) {
                Surface(tonalElevation = 3.dp) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // RECORD BUTTON

                        Button(
                            onClick = {

                                val intent = Intent(
                                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                                ).apply {

                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )

                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "Speak your journal entry"
                                    )

                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE,
                                        Locale.getDefault()
                                    )
                                }

                                try {
                                    speechLauncher.launch(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Speech recognition unavailable",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },

                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),

                            shape = CircleShape

                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))
                            Text("Record")
                        }


                        // SUMMARY BUTTON

                        OutlinedButton(
                            onClick = {

                                if (todayEntries.isEmpty()) {

                                    Toast.makeText(
                                        context,
                                        "No records for today yet",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else if (
                                    accountId.isBlank() || apiToken.isBlank()
                                ) {
                                    showKeyDialog = true

                                } else {

                                    summaryLoading = true
                                    summary = null
                                    voiceMessage = null

                                    scope.launch {

                                        summary = try {
                                            createSummary(
                                                accountId,
                                                apiToken,
                                                todayEntries
                                            )
                                        } catch (e: Exception) {
                                            "Unable to generate summary.\n\n" +
                                                    (e.message ?: "Please try again.")
                                        }

                                        summaryLoading = false
                                    }
                                }
                            },

                            modifier = Modifier.height(54.dp),
                            enabled = !summaryLoading

                        ) {
                            Text(
                                if (summaryLoading) "Please wait..."
                                else "Summarise today"
                            )
                        }
                    }
                }
            }
        }

    ) { padding ->


        if (!showPreviousRecords) {
            // DASHBOARD: today's information only
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),

                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp,
                    bottom = 18.dp
                ),

                verticalArrangement = Arrangement.spacedBy(14.dp)

            ) {


                // SUMMARY CARD

                if (summary != null || summaryLoading) {

                    item {

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {

                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                Text(
                                    "Today's summary",
                                    fontWeight = FontWeight.Bold
                                )

                                if (summaryLoading) {

                                    CircularProgressIndicator()
                                    Text("Generating your summary...")

                                } else {

                                    Text(summary.orEmpty())


                                    // ANDROID TTS LISTEN BUTTON

                                    Button(
                                        onClick = {

                                            if (!voiceReady || voiceLoading) {
                                                return@Button
                                            }

                                            val textToSpeak = summary.orEmpty()

                                            if (textToSpeak.isBlank()) {
                                                return@Button
                                            }

                                            scope.launch {

                                                voiceLoading = true
                                                voiceMessage = "Speaking..."

                                                try {
                                                    voice.speak(textToSpeak)
                                                    voiceMessage = "Playback completed."

                                                } catch (e: Exception) {
                                                    voiceMessage =
                                                        "Voice error: ${e.message ?: e.javaClass.simpleName}"

                                                } finally {
                                                    voiceLoading = false
                                                }
                                            }
                                        },

                                        enabled = voiceReady && !voiceLoading

                                    ) {

                                        Text(
                                            when {
                                                !voiceReady && voiceLoading ->
                                                    "Preparing voice..."

                                                !voiceReady ->
                                                    "Voice unavailable"

                                                voiceLoading ->
                                                    "Speaking..."

                                                else ->
                                                    "🔊 Listen"
                                            }
                                        )
                                    }


                                    // VOICE STATUS

                                    voiceMessage?.let { message ->
                                        Text(
                                            message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        summary = null
                                        voiceMessage = null
                                    }
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }


                // TODAY'S RECORDS
                if (todayEntries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (entries.isEmpty()) "Your journal starts here" else "No records for today",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Tap Record to add today's first entry.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Today's Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    lazyItems(
                        items = todayEntries.sortedByDescending { it.createdAt },
                        key = { it.id }
                    ) { entry ->
                        EntryRow(
                            entry = entry,
                            onDelete = {
                                scope.launch { dao.delete(entry) }
                            }
                        )
                    }
                }
            }
        } else if (selectedDateKey == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                PreviousRecordsScreen(
                    entries = entries,
                    onOpenDate = { selectedDateKey = it },
                    onRequestDeleteDate = { datePendingDeletion = it }
                )
            }
        } else {
            val selectedEntries = entries.filter {
                dateKey(it.createdAt) == selectedDateKey
            }.sortedByDescending { it.createdAt }

            Box(Modifier.fillMaxSize().padding(padding)) {
                DateRecordsScreen(entries = selectedEntries)
            }
        }
    }

    // Confirm before deleting every entry belonging to one date.
    datePendingDeletion?.let { dateKeyToDelete ->
        val entriesForDate = entries.filter { dateKey(it.createdAt) == dateKeyToDelete }
        AlertDialog(
            onDismissRequest = { datePendingDeletion = null },
            title = { Text("Delete this date's records?") },
            text = {
                Text("This will permanently delete all ${entriesForDate.size} records from ${formatDateTitle(dateKeyToDelete)}. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            entriesForDate.forEach { dao.delete(it) }
                            datePendingDeletion = null
                            if (selectedDateKey == dateKeyToDelete) selectedDateKey = null
                            Toast.makeText(context, "Date records deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { datePendingDeletion = null }) { Text("Cancel") }
            }
        )
    }


    // CLOUDFLARE SETTINGS DIALOG

    if (showKeyDialog) {

        AlertDialog(
            onDismissRequest = {
                showKeyDialog = false
            },

            title = {
                Text("Cloudflare AI Setup")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text("Enter your Cloudflare credentials.")

                    OutlinedTextField(
                        value = accountId,
                        onValueChange = { accountId = it },
                        label = { Text("Cloudflare Account ID") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("Cloudflare API Token") },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Text(
                        "Journal entries stay on your phone until you request an AI summary.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        accountId = accountId.trim()
                        apiToken = apiToken.trim()

                        preferences.edit()
                            .putString("cloudflare_account_id", accountId)
                            .putString("cloudflare_token", apiToken)
                            .apply()

                        showKeyDialog = false

                        Toast.makeText(
                            context,
                            "Cloudflare settings saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showKeyDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


// PREVIOUS RECORDS: date cards in a two-column grid

@Composable
private fun PreviousRecordsScreen(
    entries: List<JournalEntry>,
    onOpenDate: (String) -> Unit,
    onRequestDeleteDate: (String) -> Unit
) {
    val groupedByDate = entries.groupBy { dateKey(it.createdAt) }
        .toSortedMap(compareByDescending<String> { it })

    if (groupedByDate.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No previous records yet.")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groupedByDate.entries.toList(), key = { it.key }) { (key, dayEntries) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDate(key) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatDateTitle(key),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onRequestDeleteDate(key) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete all records for ${formatDateTitle(key)}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    dayEntries.sortedByDescending { it.createdAt }.take(2).forEach { entry ->
                        Text(
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3
                        )
                    }
                    Text(
                        "${dayEntries.size} ${if (dayEntries.size == 1) "record" else "records"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
private fun DateRecordsScreen(entries: List<JournalEntry>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No records for this date.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        lazyItems(entries, key = { it.id }) { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(entry.text, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
            }
        }
    }
}

private fun dateKey(timestamp: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))

private fun formatDateTitle(key: String): String = try {
    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(key)?.let { date ->
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
    } ?: key
} catch (_: Exception) {
    key
}


// JOURNAL ENTRY ROW

@Composable
private fun EntryRow(
    entry: JournalEntry,
    onDelete: () -> Unit
) {

    val time = remember(entry.createdAt) {
        SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        ).format(Date(entry.createdAt))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                time,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                entry.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete record",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider()
}


// CLOUDFLARE AI SUMMARY

private suspend fun createSummary(
    accountId: String,
    apiToken: String,
    entries: List<JournalEntry>
): String = withContext(Dispatchers.IO) {

    val journalText = entries.joinToString("\n") { entry ->

        val time = SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        ).format(Date(entry.createdAt))

        "$time: ${entry.text}"
    }

    val body = JSONObject().apply {

        put(
            "messages",
            JSONArray().apply {

                put(
                    JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            """
                            You are a personal journal assistant.
                            Summarise the user's journal accurately.
                            Give 4-6 short bullet points.
                            Highlight activities, plans and pending tasks.
                            Never claim a task is completed unless the user
                            explicitly says it is completed.
                            Do not invent information.
                            If an entry is unclear, say it is unclear.
                            """.trimIndent()
                        )
                    }
                )

                put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "content",
                            "Summarise my journal entries for today:\n\n$journalText"
                        )
                    }
                )
            }
        )

        put("max_tokens", 250)
        put("temperature", 0.2)
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(50, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url(
            "https://api.cloudflare.com/client/v4/accounts/" +
                    accountId +
                    "/ai/run/@cf/meta/llama-3.2-1b-instruct"
        )
        .header("Authorization", "Bearer $apiToken")
        .header("Content-Type", "application/json")
        .post(
            body.toString().toRequestBody(
                "application/json".toMediaType()
            )
        )
        .build()

    client.newCall(request).execute().use { response ->

        val responseText = response.body?.string().orEmpty()

        val json = try {
            JSONObject(responseText)
        } catch (_: Exception) {
            error("Invalid response from Cloudflare: $responseText")
        }

        if (
            !response.isSuccessful ||
            !json.optBoolean("success", false)
        ) {

            val errors = json.optJSONArray("errors")

            val errorMessage = errors
                ?.optJSONObject(0)
                ?.optString("message")
                ?: "Cloudflare request failed."

            error(
                "Cloudflare API error ${response.code}: $errorMessage"
            )
        }

        val result = json.optJSONObject("result")
            ?: error("Cloudflare returned no result.")

        result.optString("response")
            .takeIf { it.isNotBlank() }
            ?: error("The AI returned an empty summary.")
    }
}