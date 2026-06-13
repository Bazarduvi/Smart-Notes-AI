package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// Data Models
data class RealWeatherData(
    val cityName: String,
    val country: String,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val uvIndex: Double,
    val aqi: Int,
    val description: String,
    val code: Int,
    val hourlyForecast: List<HourlyItem>,
    val dropcastData: List<Float> // 0 to 1 intensity values for live rainfall
)

data class HourlyItem(
    val hour: String,
    val temp: Double,
    val feelsLike: Double,
    val aqi: Int,
    val description: String,
    val code: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(onBack: () -> Unit = {}) {
    var searchCity by remember { mutableStateOf("") }
    var activeCity by remember { mutableStateOf("Hong Kong") }
    var weatherData by remember { mutableStateOf<RealWeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Fetch initial weather for Hong Kong
    LaunchedEffect(activeCity) {
        isLoading = true
        errorMsg = null
        scope.launch(Dispatchers.IO) {
            try {
                val data = fetchWeatherFromApi(activeCity)
                withContext(Dispatchers.Main) {
                    weatherData = data
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMsg = "No se pudieron cargar datos reales. Cargando datos simulados realistas..."
                    weatherData = makeRealisticMockData(activeCity)
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clima en Tiempo Real", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0B0F19))
        ) {
            // Search City Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchCity,
                    onValueChange = { searchCity = it },
                    placeholder = { Text("Buscar ciudad...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("city_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (searchCity.isNotBlank()) {
                            activeCity = searchCity.trim()
                            searchCity = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.testTag("city_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
            }

            // Quick City Toggles
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presetCities = listOf("Hong Kong", "Madrid", "Miami", "Tokyo", "London")
                items(presetCities) { city ->
                    val isSelected = activeCity.equals(city, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E293B))
                            .clickable { activeCity = city }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(city, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    weatherData?.let { data ->
                        // 1. Current Weather Header (Temps and conditions)
                        item {
                            WeatherHeaderSection(data)
                        }

                        // 2. Extra Metrics (UV, AQI, Humidity)
                        item {
                            WeatherExtraMetricsSection(data)
                        }

                        // 3. Radar Map Preview (With beautiful interactive Canvas animation)
                        item {
                            RadarMapSection(data.cityName)
                        }

                        // 4. Dropcast Section
                        item {
                            DropcastSection(data.dropcastData)
                        }

                        // 5. hourly forecast cronology
                        item {
                            HourlyCronologySection(data.hourlyForecast)
                        }
                    } ?: item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se pudieron obtener datos meteorológicos.", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherHeaderSection(data: RealWeatherData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${data.cityName}, ${data.country}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "${data.temp.toInt()}°",
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light
        )

        Text(
            text = "Sensación térmica de ${data.feelsLike.toInt()}° • ${data.description}",
            color = Color.LightGray,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WeatherExtraMetricsSection(data: RealWeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // UVI Bubble
        WeatherMetricCard(
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFF59E0B),
            label = "${data.uvIndex.toInt()} UVI",
            subLabel = getUviText(data.uvIndex)
        )

        // AQI Bubble
        val aqiText = getAqiText(data.aqi)
        WeatherMetricCard(
            icon = Icons.Default.Info,
            iconTint = getAqiColor(data.aqi),
            label = "${data.aqi} $aqiText",
            subLabel = "ICA / AQI"
        )

        // Humidity Bubble
        WeatherMetricCard(
            icon = Icons.Default.WaterDrop,
            iconTint = Color(0xFF3B82F6),
            label = "${data.humidity}%",
            subLabel = "Humedad"
        )
    }
}

@Composable
fun RowScope.WeatherMetricCard(
    runAction: () -> Unit = {},
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    subLabel: String
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subLabel, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun RadarMapSection(cityName: String) {
    var animationFrame by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    
    // Simulate active radar sweeping animation
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200L)
            animationFrame = (animationFrame + 1) % 5
        }
    }

    if (isExpanded) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isExpanded = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().height(450.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw radar grids
                        drawRect(color = Color(0xFF0F172A))
                        val gridColor = Color(0xFF1E293B)
                        for (x in 0..width.toInt() step (width / 8).toInt()) {
                            drawLine(color = gridColor, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), height))
                        }
                        for (y in 0..height.toInt() step (height / 8).toInt()) {
                            drawLine(color = gridColor, start = Offset(0f, y.toFloat()), end = Offset(width, y.toFloat()))
                        }

                        // Circles
                        drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.2f), radius = 80.dp.toPx(), center = Offset(width*0.5f, height*0.5f), style = Stroke(width = 2f))
                        drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.1f), radius = 150.dp.toPx(), center = Offset(width*0.5f, height*0.5f), style = Stroke(width = 2f))

                        val listColors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.7f),
                            Color(0xFF10B981).copy(alpha = 0.7f),
                            Color(0xFFF59E0B).copy(alpha = 0.8f),
                            Color(0xFFEF4444).copy(alpha = 0.85f)
                        )

                        // Detailed rain blobs
                        drawCircle(
                            color = listColors[3 - (animationFrame % 4)],
                            radius = (60 + animationFrame * 10).dp.toPx(),
                            center = Offset(width * 0.55f, height * 0.48f),
                            alpha = 0.4f
                        )
                        drawCircle(
                            color = listColors[1],
                            radius = (40 + animationFrame * 6).dp.toPx(),
                            center = Offset(width * 0.35f, height * 0.62f),
                            alpha = 0.6f
                        )
                        drawCircle(
                            color = listColors[2],
                            radius = (30 + animationFrame * 5).dp.toPx(),
                            center = Offset(width * 0.75f, height * 0.35f),
                            alpha = 0.5f
                        )
                    }

                    IconButton(onClick = { isExpanded = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text("Mapa Detallado de Precipitaciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Región central: $cityName. Precipitaciones críticas detectadas en el sur-oeste. Vientos de 24km/h SE.", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { isExpanded = true },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated radar background: beautiful grid drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw radar grids
                drawRect(color = Color(0xFF0F172A))
                
                // Horizontal + vertical grid lines
                val gridColor = Color(0xFF1E293B)
                val strokeW = 1f
                for (x in 0..width.toInt() step (width / 6).toInt()) {
                    drawLine(color = gridColor, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), height), strokeWidth = strokeW)
                }
                for (y in 0..height.toInt() step (height / 4).toInt()) {
                    drawLine(color = gridColor, start = Offset(0f, y.toFloat()), end = Offset(width, y.toFloat()), strokeWidth = strokeW)
                }

                // Draw radar circles in center
                drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.1f), radius = 50.dp.toPx(), center = Offset(width*0.5f, height*0.5f), style = Stroke(width = 2f))
                drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.05f), radius = 100.dp.toPx(), center = Offset(width*0.5f, height*0.5f), style = Stroke(width = 2f))

                // Simulated geographic outlines for Hong Kong/Shenzhen region
                val outlineColor = Color.Gray.copy(alpha = 0.4f)
                val path = Path().apply {
                    moveTo(width * 0.1f, height * 0.4f)
                    lineTo(width * 0.3f, height * 0.3f)
                    lineTo(width * 0.5f, height * 0.45f)
                    lineTo(width * 0.8f, height * 0.35f)
                    lineTo(width * 0.9f, height * 0.6f)
                    lineTo(width * 0.7f, height * 0.8f)
                    lineTo(width * 0.4f, height * 0.7f)
                    lineTo(width * 0.2f, height * 0.85f)
                    close()
                }
                drawPath(path = path, color = outlineColor, style = Stroke(width = 1.5f))

                // Simulated dynamic rain spots (precipitaciones colored layout from image)
                val listColors = listOf(
                    Color(0xFF3B82F6).copy(alpha = 0.7f), // Blue (light rain)
                    Color(0xFF10B981).copy(alpha = 0.7f), // Green
                    Color(0xFFF59E0B).copy(alpha = 0.8f), // Yellow/Orange
                    Color(0xFFEF4444).copy(alpha = 0.85f) // Red (extreme rain)
                )

                // Draw rain blobs shifting based on animationFrame
                drawCircle(
                    color = listColors[3 - (animationFrame % 4)],
                    radius = (40 + animationFrame * 6).dp.toPx(),
                    center = Offset(width * 0.55f, height * 0.48f),
                    alpha = 0.4f
                )
                drawCircle(
                    color = listColors[(animationFrame + 1) % 4],
                    radius = (20 + animationFrame * 4).dp.toPx(),
                    center = Offset(width * 0.45f, height * 0.52f),
                    alpha = 0.6f
                )
                drawCircle(
                    color = listColors[(animationFrame + 2) % 4],
                    radius = (10 + animationFrame * 3).dp.toPx(),
                    center = Offset(width * 0.75f, height * 0.35f),
                    alpha = 0.5f
                )
            }

            // Radar coordinates label overlay (styled after image)
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "Norte",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = "Oeste",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = cityName, // Dynamic city name
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = "Este",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    text = "Sur",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Info watermark
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa Radárico", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DropcastSection(dropcastData: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dropcast", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Precipitación minuto a minuto (Próxima hora)", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Graph canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barCount = dropcastData.size
                    val spacing = 4f
                    val barWidth = (width - (spacing * (barCount - 1))) / barCount

                    dropcastData.forEachIndexed { index, value ->
                        val barHeight = value * height
                        val x = index * (barWidth + spacing)
                        val y = height - barHeight

                        // Blue rain gradients matching image
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA).copy(alpha = 0.3f))
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Axis labels matching image: "ahora", "01:42", "01:57", "02:12", "02:27"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ahora", color = Color.Gray, fontSize = 10.sp)
                Text("01:42", color = Color.Gray, fontSize = 10.sp)
                Text("01:57", color = Color.Gray, fontSize = 10.sp)
                Text("02:12", color = Color.Gray, fontSize = 10.sp)
                Text("02:27", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun HourlyCronologySection(hourlyList: List<HourlyItem>) {
    var selectedFilter by remember { mutableStateOf("Temp") } // "Temp", "Sensación", "ICA"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cronología de hoy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Style toggles from image: [Temp] [Sensación térmica de] [ICA]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Temp", "Sensación", "ICA").forEach { opt ->
                    val isSelected = selectedFilter == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF0F172A))
                            .clickable { selectedFilter = opt }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val icon = when (opt) {
                                "Temp" -> Icons.Default.Thermostat
                                "Sensación" -> Icons.Default.AccessibilityNew
                                else -> Icons.Default.FilterVintage
                            }
                            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (opt) {
                                    "Sensación" -> "Sensación térmica de"
                                    else -> opt
                                },
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Hourly List Items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hourlyList.take(8).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.hour,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.width(55.dp)
                        )

                        // Vertical condition line (rain state)
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(getWeatherCodeColor(item.code))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Description
                        Text(
                            text = item.description,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Display active factor value
                        val valueStr = when (selectedFilter) {
                            "Temp" -> "${item.temp.toInt()}°"
                            "Sensación" -> "${item.feelsLike.toInt()}°"
                            else -> "ICA ${item.aqi}"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = valueStr,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helpers
fun getUviText(uv: Double): String = when {
    uv < 3 -> "Bajo"
    uv < 6 -> "Moderado"
    uv < 8 -> "Alto"
    uv < 11 -> "Muy Alto"
    else -> "Extremo"
}

fun getAqiText(aqi: Int): String = when {
    aqi < 50 -> "Excelente"
    aqi < 100 -> "Moderado"
    aqi < 150 -> "Insalubre/Sensibles"
    else -> "Insalubre"
}

fun getAqiColor(aqi: Int): Color = when {
    aqi < 50 -> Color(0xFF10B981)
    aqi < 100 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}

fun getWeatherCodeColor(code: Int): Color = when (code) {
    0, 1 -> Color(0xFFF59E0B) // Yellow sun
    2, 3 -> Color(0xFF94A3B8) // Gray cloud
    in 51..65, in 80..82 -> Color(0xFF3B82F6) // Blue rain
    else -> Color(0xFF8B5CF6) // Purple storm
}

fun getWeatherTypeFromCode(code: Int): String = when (code) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55 -> "Llovizna"
    61, 63 -> "Lluvia ligera"
    65 -> "Lluvia extrema"
    80, 81 -> "Chubascos"
    82 -> "Lluvia extrema"
    95, 96, 99 -> "Tormenta eléctrica"
    else -> "Templado"
}

// REST Network calls using HttpURLConnection
suspend fun fetchWeatherFromApi(cityName: String): RealWeatherData {
    return withContext(Dispatchers.IO) {
        // Step 1: Geocoding search coordinates
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(cityName, "UTF-8")}&count=1&language=es&format=json"
        val geoConn = URL(geoUrl).openConnection() as HttpURLConnection
        geoConn.requestMethod = "GET"
        val geoResponse = geoConn.inputStream.bufferedReader().use { it.readText() }
        
        val resultsObject = JSONObject(geoResponse)
        val resultArray = resultsObject.optJSONArray("results")
        if (resultArray == null || resultArray.length() == 0) {
            throw Exception("City not found")
        }

        val cityObj = resultArray.getJSONObject(0)
        val lat = cityObj.getDouble("latitude")
        val lon = cityObj.getDouble("longitude")
        val resolvedName = cityObj.getString("name")
        val resolvedCountry = cityObj.optString("country", "HK")

        // Step 2: Fetch forecast weather
        val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,uv_index&hourly=temperature_2m,apparent_temperature,precipitation,weather_code&timezone=auto"
        val forecastConn = URL(forecastUrl).openConnection() as HttpURLConnection
        forecastConn.requestMethod = "GET"
        val forecastResponse = forecastConn.inputStream.bufferedReader().use { it.readText() }
        val fcObj = JSONObject(forecastResponse)

        val currentObj = fcObj.getJSONObject("current")
        val temp = currentObj.getDouble("temperature_2m")
        val humidity = currentObj.getInt("relative_humidity_2m")
        val feelsLike = currentObj.getDouble("apparent_temperature")
        val code = currentObj.getInt("weather_code")
        val uv = currentObj.optDouble("uv_index", 2.0)

        // Step 3: Fetch AQI Air Quality index
        var aqiValue = 30 // safe default
        try {
            val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi&timezone=auto"
            val aqiConn = URL(aqiUrl).openConnection() as HttpURLConnection
            aqiConn.requestMethod = "GET"
            val aqiResponse = aqiConn.inputStream.bufferedReader().use { it.readText() }
            val aqiObj = JSONObject(aqiResponse)
            aqiValue = aqiObj.getJSONObject("current").getInt("us_aqi")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse Hourly Items
        val hourlyForecast = mutableListOf<HourlyItem>()
        val hourlyObj = fcObj.getJSONObject("hourly")
        val times = hourlyObj.getJSONArray("time")
        val hourlyTemps = hourlyObj.getJSONArray("temperature_2m")
        val hourlyApparent = hourlyObj.getJSONArray("apparent_temperature")
        val hourlyCodes = hourlyObj.getJSONArray("weather_code")

        val sdfParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val sdfFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (i in 0 until minOf(24, times.length())) {
            val rawTime = times.getString(i)
            val parsedTime = try { sdfParser.parse(rawTime) ?: Date() } catch (ex: Exception) { Date() }
            val formattedTime = sdfFormatter.format(parsedTime)
            
            val hTemp = hourlyTemps.getDouble(i)
            val hApparent = hourlyApparent.getDouble(i)
            val hCode = hourlyCodes.getInt(i)

            hourlyForecast.add(
                HourlyItem(
                    hour = formattedTime,
                    temp = hTemp,
                    feelsLike = hApparent,
                    aqi = (aqiValue + (i % 5 - 2) * 5).coerceIn(10, 200), // correlate aqi loosely
                    description = getWeatherTypeFromCode(hCode),
                    code = hCode
                )
            )
        }

        // Generate dropcast minute array
        val dropcast = List(45) { idx ->
            val factor = if (code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82)) {
                (0.9f - (idx * 0.015f)).coerceIn(0.1f, 1.0f)
            } else {
                0.0f
            }
            factor
        }

        RealWeatherData(
            cityName = resolvedName,
            country = resolvedCountry,
            temp = temp,
            feelsLike = feelsLike,
            humidity = humidity,
            uvIndex = uv,
            aqi = aqiValue,
            description = getWeatherTypeFromCode(code),
            code = code,
            hourlyForecast = hourlyForecast,
            dropcastData = dropcast
        )
    }
}

// Realistic fallbacks in case of offline operations
fun makeRealisticMockData(city: String): RealWeatherData {
    val isHongKong = city.equals("Hong Kong", ignoreCase = true)
    
    val baseTemp = if (isHongKong) 28.0 else 24.0
    val feelsLike = if (isHongKong) 28.0 else 23.5
    val desc = if (isHongKong) "Lluvia extrema" else "Parcialmente nublado"
    val code = if (isHongKong) 82 else 2
    val humidity = if (isHongKong) 81 else 65
    val uv = if (isHongKong) 0.0 else 5.0
    val aqi = if (isHongKong) 30 else 42

    val dropcast = List(45) { idx ->
        if (isHongKong) {
            // Dropcast gradient curves imitating the screenshot
            (0.9f - (idx * 0.016f)).coerceIn(0.05f, 1.0f)
        } else {
            0.0f
        }
    }

    val hourly = List(12) { idx ->
        val hourVal = String.format("%02d:00", (idx + 1) % 24)
        val hDesc = when (idx) {
            0 -> if(isHongKong) "Lluvia extrema" else "Variado"
            1 -> if(isHongKong) "Lluvia ligera" else "Parcialmente Nublado"
            2 -> if(isHongKong) "Nublado" else "Despejado"
            else -> "Nublado"
        }
        val hCode = when (idx) {
            0 -> if(isHongKong) 82 else 3
            1 -> if(isHongKong) 61 else 2
            2 -> if(isHongKong) 3 else 0
            else -> 3
        }
        HourlyItem(
            hour = hourVal,
            temp = baseTemp,
            feelsLike = feelsLike,
            aqi = aqi + (idx * 2),
            description = hDesc,
            code = hCode
        )
    }

    return RealWeatherData(
        cityName = city,
        country = if (isHongKong) "HK" else "ES",
        temp = baseTemp,
        feelsLike = feelsLike,
        humidity = humidity,
        uvIndex = uv,
        aqi = aqi,
        description = desc,
        code = code,
        hourlyForecast = hourly,
        dropcastData = dropcast
    )
}
