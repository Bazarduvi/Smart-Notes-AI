package com.example.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FloatingNoteService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Data Models
data class ReminderItem(
    val id: Int, 
    val text: String, 
    val time: String, 
    val priority: String = "Media", // Alta, Media, Baja
    val category: String = "General" // Bolsa, Clima, General, Oficina
)

data class ChatMessage(
    val text: String, 
    val isUser: Boolean,
    val suggestedReminder: ReminderItem? = null
)

data class SimulatedStickyNote(
    val id: Int,
    var text: String,
    val noteColor: Color,
    var offsetPosition: IntOffset = IntOffset(0, 0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAndRemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Dynamic theme settings from central AppSettingsStore
    val isDark = AppSettingsStore.isDarkMode.value
    val bgGradient = if (isDark) listOf(Color(0xFF0F172A), Color(0xFF0B0F19)) else listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.LightGray else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val accentColor = Color(0xFF3B82F6)

    // State Variables
    var reminders by remember { 
        mutableStateOf(listOf(
            ReminderItem(1, "Consultar volumen de Tesla en Nikkei", "Hoy 17:30", "Alta", "Bolsa"),
            ReminderItem(2, "Verificar precipitaciones en Radar", "Lunes 09:15", "Media", "Clima")
        )) 
    }
    var nextId by remember { mutableStateOf(3) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Asistente IA, 1: Recordatorios de Suite, 2: Sticky Board (Windows-style)

    // Window Sticky Notes state
    var stickyNotesList by remember {
        mutableStateOf(mutableListOf(
            SimulatedStickyNote(1, "¡Muevo esta pequeña nota con el dedo por toda la pantalla! Presiona el botón + para crear más o X para eliminarla.", Color(0xFFFEF08A), IntOffset(40, 20)),
            SimulatedStickyNote(2, "Comprar acciones BYD", Color(0xFF93C5FD), IntOffset(180, 240))
        ))
    }
    var nextStickyId by remember { mutableStateOf(3) }

    // Floating note permissions & launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "¡Permiso Concedido! Ahora puedes visualizar las notas flotantes fuera de la app.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso Denegado. La nota permanecerá solo interna.", Toast.LENGTH_SHORT).show()
        }
    }

    fun playNotificationSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startFloatingNoteService(text: String) {
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, FloatingNoteService::class.java).apply {
                putExtra("NOTE_TEXT", text)
            }
            context.startService(intent)
            Toast.makeText(context, "Nota flotante configurada externamente ✔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso especial para arrastrar notas flotantes fuera del app.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            overlayPermissionLauncher.launch(intent)
        }
    }

    // Natural Language Voice Parser
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                // Client-side NLP extraction
                val parsed = parseNaturalLanguageText(recognizedText)
                if (parsed.isNote) {
                    // Create simulated + service sticky note
                    val colors = listOf(Color(0xFFFEF08A), Color(0xFF93C5FD), Color(0xFFFCA5A5), Color(0xFF86EFAC))
                    val newSticky = SimulatedStickyNote(
                        id = nextStickyId++,
                        text = parsed.title,
                        noteColor = colors.random()
                    )
                    stickyNotesList = (stickyNotesList + newSticky).toMutableList()
                    startFloatingNoteService(parsed.title)
                    selectedTab = 2 // focus stickies
                } else {
                    val formattedTime = "${parsed.date} a las ${parsed.hour}"
                    val newReminder = ReminderItem(
                        id = nextId++,
                        text = parsed.title,
                        time = formattedTime,
                        priority = "Alta",
                        category = "General"
                    )
                    reminders = reminders + newReminder
                    playNotificationSound()
                    Toast.makeText(context, "🔔 Recordatorio por Voz Sugerido Creado con éxito.", Toast.LENGTH_SHORT).show()
                    selectedTab = 1 // focus reminders
                }
            }
        }
    }

    fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: 'Crear nota sobre Tesla' o 'Recordar reunión mañana 5:00 pm'")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Dispositivo no soporta reconocimiento por voz.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexus AI Suite", fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { startListening() }, modifier = Modifier.background(accentColor.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Mic, contentDescription = "Voz", tint = accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Brush.verticalGradient(bgGradient))
        ) {
            // High-Tech Custom Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = textPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = accentColor
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mesa Inteligente")
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recordatorios")
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dashboard, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sticky Board")
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> ChatAssistantSection(
                        onAddReminder = { r -> 
                            reminders = reminders + r.copy(id = nextId)
                            nextId++
                            playNotificationSound()
                        },
                        onAddSticky = { text ->
                            val newS = SimulatedStickyNote(id = nextStickyId++, text = text, noteColor = Color(0xFFFEF08A))
                            stickyNotesList = (stickyNotesList + newS).toMutableList()
                        },
                        onTriggerNativeSticky = { text ->
                            startFloatingNoteService(text)
                        },
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    1 -> AdvancedRemindersSection(
                        reminders = reminders,
                        onAddReminder = { r -> 
                            reminders = reminders + r.copy(id = nextId)
                            nextId++
                            playNotificationSound()
                        },
                        onDeleteReminder = { id -> 
                            reminders = reminders.filter { it.id != id }
                        },
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    2 -> StickyNoteBoardSection(
                        notes = stickyNotesList,
                        onNoteCreate = { text, color ->
                            val s = SimulatedStickyNote(id = nextStickyId++, text = text, noteColor = color)
                            stickyNotesList = (stickyNotesList + s).toMutableList()
                        },
                        onNoteUpdateText = { id, nText ->
                            stickyNotesList = stickyNotesList.map {
                                if (it.id == id) it.copy(text = nText) else it
                            }.toMutableList()
                        },
                        onNoteUpdateOffset = { id, offset ->
                            stickyNotesList = stickyNotesList.map {
                                if (it.id == id) it.copy(offsetPosition = offset) else it
                            }.toMutableList()
                        },
                        onDeleteNote = { id ->
                            stickyNotesList = stickyNotesList.filter { it.id != id }.toMutableList()
                        },
                        onLaunchExternal = { text ->
                            startFloatingNoteService(text)
                        },
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

// CLIENT-SIDE NATURAL LANGUAGE INTERPRETATION ALGORITHM
data class NaturalLanguageParsed(
    val title: String,
    val date: String,
    val hour: String,
    val isNote: Boolean
)

fun parseNaturalLanguageText(input: String): NaturalLanguageParsed {
    var title = input
    var date = "Hoy"
    var hour = "12:00"
    var isNote = false

    val lwInput = input.lowercase()

    // Detect if sticky note is requested
    if (lwInput.contains("nota") || lwInput.contains("sticky") || lwInput.contains("flotante") || lwInput.contains("papelito")) {
        isNote = true
    }

    // Days Extraction
    if (lwInput.contains("mañana")) {
        date = "Mañana"
        title = title.replace("mañana", "", ignoreCase = true)
    } else if (lwInput.contains("hoy")) {
        date = "Hoy"
        title = title.replace("hoy", "", ignoreCase = true)
    } else if (lwInput.contains("lunes")) {
        date = "Lunes"
        title = title.replace("lunes", "", ignoreCase = true)
    } else if (lwInput.contains("martes")) {
        date = "Martes"
        title = title.replace("martes", "", ignoreCase = true)
    } else if (lwInput.contains("miércoles")) {
        date = "Miércoles"
        title = title.replace("miércoles", "", ignoreCase = true)
    } else if (lwInput.contains("jueves")) {
        date = "Jueves"
        title = title.replace("jueves", "", ignoreCase = true)
    } else if (lwInput.contains("viernes")) {
        date = "Viernes"
        title = title.replace("viernes", "", ignoreCase = true)
    } else if (lwInput.contains("sábado")) {
        date = "Sábado"
        title = title.replace("sábado", "", ignoreCase = true)
    } else if (lwInput.contains("domingo")) {
        date = "Domingo"
        title = title.replace("domingo", "", ignoreCase = true)
    }

    // Time extraction (numbers layout matching regex)
    val hourRegex = "(\\d{1,2}:\\d{2})".toRegex()
    val hourMatches = hourRegex.find(lwInput)
    if (hourMatches != null) {
        hour = hourMatches.value
        title = title.replace(hour, "", ignoreCase = true)
    } else {
        if (lwInput.contains("las 3") || lwInput.contains("las tres")) {
            hour = "15:00"
            title = title.replace("las 3", "", ignoreCase = true).replace("las tres", "", ignoreCase = true)
        } else if (lwInput.contains("las 5") || lwInput.contains("las cinco")) {
            hour = "17:00"
            title = title.replace("las 5", "", ignoreCase = true).replace("las cinco", "", ignoreCase = true)
        } else if (lwInput.contains("las 9") || lwInput.contains("las nueve")) {
            hour = "09:00"
            title = title.replace("las 9", "", ignoreCase = true).replace("las nueve", "", ignoreCase = true)
        } else if (lwInput.contains("las 12") || lwInput.contains("las doce")) {
            hour = "12:00"
            title = title.replace("las 12", "", ignoreCase = true).replace("las doce", "", ignoreCase = true)
        }
    }

    // Clean prefix command noise verbs
    val noises = listOf("crea", "crear", "pon", "recordame", "recuérdame", "sobre", "que", "para", "de", "una nota", "nota", "recordatorio", "un recordatorio")
    var cleanTitle = title.trim()
    noises.forEach { noise ->
        if (cleanTitle.startsWith(noise, ignoreCase = true)) {
            cleanTitle = cleanTitle.substring(noise.length).trim()
        }
    }
    if (cleanTitle.endsWith(" para", ignoreCase = true)) {
        cleanTitle = cleanTitle.removeSuffix(" para")
    }
    if (cleanTitle.isEmpty()) {
        cleanTitle = if (isNote) "Nueva Nota de Trabajo" else "Tarea Inteligente Programada"
    }

    return NaturalLanguageParsed(
        title = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
        date = date,
        hour = hour,
        isNote = isNote
    )
}

// TAB 0: CHAT ASSISTANT SECTION WITH SMART NLP PREVIEW CARD
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAssistantSection(
    onAddReminder: (ReminderItem) -> Unit,
    onAddSticky: (String) -> Unit,
    onTriggerNativeSticky: (String) -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val accentColor = Color(0xFF3B82F6)
    var messages by remember {
        mutableStateOf(listOf(
            ChatMessage("¡Hola! Soy tu asistente Nexus. Puedes escribirme o dictarme con tu voz. Te ayudaré a estructurar recordatorios y crear sticky notes instantáneamente.", false)
        ))
    }
    var typedText by remember { mutableStateOf("") }
    var parsedPreview by remember { mutableStateOf<NaturalLanguageParsed?>(null) }
    val scope = rememberCoroutineScope()

    // Handle typing simulation & real-time auto-extract preview card
    LaunchedEffect(typedText) {
        if (typedText.length > 5) {
            parsedPreview = parseNaturalLanguageText(typedText)
        } else {
            parsedPreview = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Chat List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = true
        ) {
            val combinedMsgs = messages.reversed()
            items(combinedMsgs) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    val bg = if (isUser) Color(0xFF3B82F6) else cardBg
                    val textCol = if (isUser) Color.White else textPrimary
                    
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .padding(14.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(text = msg.text, color = textCol, fontSize = 14.sp)
                        
                        // Suggestion payload attached
                        msg.suggestedReminder?.let { sug ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Recordatorio Programado", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(sug.text, color = Color.White, fontSize = 12.sp)
                                    Text("Fecha/Hora: ${sug.time}", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating NLP Suggestion Preview Card
        parsedPreview?.let { preview ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101F30)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (preview.isNote) "Detección: Nota Adhesiva Flotante" else "Detección: Recordatorio Programado",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Título: ${preview.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (!preview.isNote) {
                        Text("Momento sugerido: ${preview.date} a las ${preview.hour}", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        Text("Destino: Pizarra Sticky Notes & Widget Externo", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (preview.isNote) {
                                    onAddSticky(preview.title)
                                    onTriggerNativeSticky(preview.title)
                                    messages = messages + ChatMessage("He creado la nota flotante '${preview.title}' y la he lanzado en primer plano. ¡Úsala y muévela!", false)
                                } else {
                                    val r = ReminderItem(100, preview.title, "${preview.date} ${preview.hour}", "Alta", "General")
                                    onAddReminder(r)
                                    messages = messages + ChatMessage("Recordatorio con IA programado: '${preview.title}'", false, r)
                                }
                                typedText = ""
                                parsedPreview = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Aceptar & Programar", fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { parsedPreview = null },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Ignorar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = { typedText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe ej: 'Llamar a papá mañana 14:00' o 'nota de tesla'...", color = Color.Gray, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (typedText.isNotBlank()) {
                        val input = typedText
                        messages = messages + ChatMessage(input, true)
                        typedText = ""
                        scope.launch {
                            delay(400)
                            val parsedResult = parseNaturalLanguageText(input)
                            if (parsedResult.isNote) {
                                onAddSticky(parsedResult.title)
                                onTriggerNativeSticky(parsedResult.title)
                                messages = messages + ChatMessage("¡Entendido! He procesado tu orden para crear una Nota Flotante Windows. ¡Agrégala o muévela!", false)
                            } else {
                                val timing = "${parsedResult.date} a las ${parsedResult.hour}"
                                val item = ReminderItem(0, parsedResult.title, timing, "Alta", "General")
                                onAddReminder(item)
                                messages = messages + ChatMessage("¡Perfecto! Un recordatorio ha sido programado con IA: '${parsedResult.title}' para el $timing.", false, item)
                            }
                        }
                    }
                },
                modifier = Modifier.background(accentColor, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}

// TAB 1: ADVANCED REMINDERS LIST SUITE
@Composable
fun AdvancedRemindersSection(
    reminders: List<ReminderItem>,
    onAddReminder: (ReminderItem) -> Unit,
    onDeleteReminder: (Int) -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val accentColor = Color(0xFF3B82F6)
    var titleInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Media") }
    var selectedCategory by remember { mutableStateOf("General") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var dateStr by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("") }

    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d -> dateStr = String.format("%02d/%02d/%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePicker = TimePickerDialog(
        context,
        { _, h, min -> timeStr = String.format("%02d:%02d", h, min) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick input card for user customization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Crear Recordatorio Avanzado", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        placeholder = { Text("Tema o asunto...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { datePicker.show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))) {
                            Text(if (dateStr.isEmpty()) "Día" else dateStr, fontSize = 11.sp, maxLines = 1)
                        }
                        Button(onClick = { timePicker.show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))) {
                            Text(if (timeStr.isEmpty()) "Hora" else timeStr, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Priority Selector row
                    Text("Prioridad", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Baja", "Media", "Alta").forEach { pr ->
                            val isSel = selectedPriority == pr
                            val col = when(pr) {
                                "Alta" -> if (isSel) Color(0xFFEF4444) else Color(0xFF7F1D1D)
                                "Media" -> if (isSel) Color(0xFFF59E0B) else Color(0xFF78350F)
                                else -> if (isSel) Color(0xFF10B981) else Color(0xFF064E3B)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(col)
                                    .clickable { selectedPriority = pr }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selector row
                    Text("Categoría", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("General", "Bolsa", "Clima", "Oficina")
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            val bgCol = if (isSel) Color(0xFF3B82F6) else Color(0xFF0F172A)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgCol)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(cat, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (titleInput.isNotBlank()) {
                                val t = if (dateStr.isNotEmpty() && timeStr.isNotEmpty()) "$dateStr $timeStr" else "Próximamente"
                                onAddReminder(
                                    ReminderItem(
                                        id = 0,
                                        text = titleInput,
                                        time = t,
                                        priority = selectedPriority,
                                        category = selectedCategory
                                    )
                                )
                                titleInput = ""
                                dateStr = ""
                                timeStr = ""
                                Toast.makeText(context, "🔔 Recordatorio Agregado con Éxito.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Programar Recordatorio")
                    }
                }
            }
        }

        item {
            Text("Lista de Recordatorios Activos", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Active List Display
        if (reminders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No tienes recordatorios programados en este momento.", color = textSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(reminders) { item ->
                val priorityColor = when (item.priority) {
                    "Alta" -> Color(0xFFEF4444)
                    "Media" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                val icon = when (item.category) {
                    "Bolsa" -> Icons.Default.TrendingUp
                    "Clima" -> Icons.Default.Cloud
                    "Oficina" -> Icons.Default.BusinessCenter
                    else -> Icons.Default.NotificationsActive
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Visual line
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.text, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Text(item.priority, color = priorityColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(item.time, color = textSecondary, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onDeleteReminder(item.id) }) {
                            Icon(Icons.Default.CheckCircle, "Eliminar/Completar", tint = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: INTERACTIVE STICKY NOTE BOARD (Simulated Desktop Environment)
@Composable
fun StickyNoteBoardSection(
    notes: List<SimulatedStickyNote>,
    onNoteCreate: (String, Color) -> Unit,
    onNoteUpdateText: (Int, String) -> Unit,
    onNoteUpdateOffset: (Int, IntOffset) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onLaunchExternal: (String) -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    var quickNoteInput by remember { mutableStateOf("") }
    val colorsList = listOf(
        Color(0xFFFEF08A), // Amarillo Post-it
        Color(0xFF93C5FD), // Azul Ciano
        Color(0xFFFCA5A5), // Rosa pastel
        Color(0xFF86EFAC)  // Verde pastel
    )
    var selectedNoteColor by remember { mutableStateOf(colorsList[0]) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pizarra Workspace Sticky Notes", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Crea notitas Windows, arrástralas con el dedo por la pantalla o mándalas al fondo de tu móvil Android.", color = textSecondary, fontSize = 11.sp)
        
        Spacer(modifier = Modifier.height(10.dp))

        // Create section
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = quickNoteInput,
                    onValueChange = { quickNoteInput = it },
                    placeholder = { Text("Escribe una nota rápida...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pick color
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colorsList.forEach { col ->
                            val isSel = selectedNoteColor == col
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (isSel) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedNoteColor = col }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (quickNoteInput.isNotBlank()) {
                                onNoteCreate(quickNoteInput, selectedNoteColor)
                                quickNoteInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir Papelito", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large simulated Desktop Sandbox frame to drag notes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (AppSettingsStore.isDarkMode.value) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            // Desk watermark
            Text(
                text = "Mesa de Trabajo Virtual\n(Arrastra las Notas)",
                color = textSecondary.copy(alpha = 0.2f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            // Render each sticky note with drag-pointer logic inside bounding box
            notes.forEach { note ->
                var offsetX by remember { mutableStateOf(note.offsetPosition.x.toFloat()) }
                var offsetY by remember { mutableStateOf(note.offsetPosition.y.toFloat()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = note.noteColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, 600f)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, 800f)
                                onNoteUpdateOffset(note.id, IntOffset(offsetX.roundToInt(), offsetY.roundToInt()))
                            }
                        }
                        .width(170.dp)
                        .padding(4.dp)
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Note, null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "System Overlay",
                                    tint = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onLaunchExternal(note.text) }
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Borrar",
                                    tint = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteNote(note.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.text,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
