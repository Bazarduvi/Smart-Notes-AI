package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val topCompanies = listOf(
    "Toyota Motor", "Sony Group", "Keyence", "Mitsubishi UFJ", "Nippon Telegraph", "Tokyo Electron", "Shin-Etsu Chemical", "Fast Retailing",
    "Tesla", "BYD", "Apple", "Microsoft", "NVIDIA", "Amazon", "Alphabet", "Meta", "Berkshire Hathaway", "Eli Lilly", "TSMC", "Broadcom",
    "JPMorgan Chase", "Novo Nordisk", "Visa", "Walmart", "ExxonMobil", "Mastercard", "UnitedHealth", "Johnson & Johnson", "Procter & Gamble", "Home Depot"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {
    var selectedCompany by remember { mutableStateOf(topCompanies[0]) }
    var baseValue by remember { mutableStateOf(39450.50) }
    var changePercent by remember { mutableStateOf(1.24) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedCompany) {
        baseValue = (100..40000).random().toDouble() + Math.random() * 100
        changePercent = (-500..500).random() / 100.0
    }

    LaunchedEffect(Unit) {
        while(true) {
            delay(1500L)
            val fluctuation = (-100..100).random() / 100.0
            baseValue += fluctuation
            changePercent += fluctuation / 500.0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mercado Bursátil",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Índices en Tiempo Real",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { showTutorial = !showTutorial }, modifier = Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(8.dp))) {
                    Icon(Icons.Default.School, "Tutorial", tint = Color.White)
                }
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCompany,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    topCompanies.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, color = Color.White) },
                            onClick = {
                                selectedCompany = selectionOption
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Valor Actual - $selectedCompany", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("$%.2f", baseValue),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%s%.2f%%", if (changePercent >= 0) "+" else "", changePercent),
                                fontSize = 18.sp,
                                color = if (changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    if (showTutorial) TutorialBalloon("Muestra el precio simulado en tiempo real. Verde indica ganancia, Rojo pérdida.", Modifier.align(Alignment.TopEnd))
                }
            }
        }
        
        item {
            Box {
                JapaneseCandlestickChart(baseValue)
                if (showTutorial) TutorialBalloon("Las velas japonesas muestran la fluctuación de precios. Cuerpos rellenos indican dirección.", Modifier.align(Alignment.TopCenter).offset(y = 10.dp))
            }
        }

        item {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MarketStatCard(
                    title = "Volumen",
                    value = String.format("%.1fM", (10..500).random().toFloat() / 10f),
                    icon = Icons.Default.BarChart,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                MarketStatCard(
                    title = "Apertura",
                    value = String.format("$%.2f", baseValue * 0.99),
                    icon = Icons.Default.Login,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TutorialBalloon(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(16.dp).width(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MarketStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

data class Candle(val open: Float, val close: Float, val high: Float, val low: Float)

@Composable
fun JapaneseCandlestickChart(basePrice: Double) {
    val b = basePrice.toFloat()
    val candles by remember(basePrice) {
        mutableStateOf(List(12) {
            val open = b + (-5..5).random().toFloat() * (b * 0.001f)
            val close = b + (-5..5).random().toFloat() * (b * 0.001f)
            val max = maxOf(open.toFloat(), close.toFloat()) + listRandom(0f, b * 0.005f)
            val min = minOf(open.toFloat(), close.toFloat()) - listRandom(0f, b * 0.005f)
            Candle(open, close, max, min)
        })
    }

    var selectedCandle by remember { mutableStateOf<Candle?>(null) }
    var selectedXPos by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gráfico de Velas (Dinámico)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDragEnd = { selectedCandle = null },
                            onDragCancel = { selectedCandle = null }
                        ) { change, _ ->
                            val x = change.position.x
                            val width = size.width.toFloat()
                            val candleWidth = width / (candles.size * 1.5f)
                            val spacing = (width - (candleWidth * candles.size)) / (candles.size + 1)
                            
                            val idx = ((x - spacing) / (candleWidth + spacing)).toInt()
                            if (idx in candles.indices) {
                                selectedCandle = candles[idx]
                                selectedXPos = x
                            }
                        }
                    }
                    .pointerInput(candles) {
                        detectTapGestures(
                            onPress = { offset ->
                                val x = offset.x
                                val width = size.width.toFloat()
                                val candleWidth = width / (candles.size * 1.5f)
                                val spacing = (width - (candleWidth * candles.size)) / (candles.size + 1)
                                
                                val idx = ((x - spacing) / (candleWidth + spacing)).toInt()
                                if (idx in candles.indices) {
                                    selectedCandle = candles[idx]
                                    selectedXPos = x
                                }
                                tryAwaitRelease()
                                selectedCandle = null
                            }
                        )
                    }
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val allVals = candles.flatMap { listOf(it.high, it.low) }
                    val maxPrice = allVals.maxOrNull() ?: 1f
                    val minPrice = allVals.minOrNull() ?: 0f
                    val priceRange = maxPrice - minPrice + 0.001f
                    
                    val candleWidth = width / (candles.size * 1.5f)
                    val spacing = (width - (candleWidth * candles.size)) / (candles.size + 1)
                    
                    candles.forEachIndexed { index, candle ->
                        val x = spacing + (index * (candleWidth + spacing))
                        
                        val highY = height - ((candle.high - minPrice) / priceRange * height)
                        val lowY = height - ((candle.low - minPrice) / priceRange * height)
                        val openY = height - ((candle.open - minPrice) / priceRange * height)
                        val closeY = height - ((candle.close - minPrice) / priceRange * height)
                        
                        val isBullish = candle.close >= candle.open
                        val color = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)
                        val drawAlpha = if (selectedCandle != null && selectedCandle != candle) 0.3f else 1f
                        
                        // Draw wick
                        drawLine(
                            color = color.copy(alpha = drawAlpha),
                            start = Offset(x + candleWidth / 2, highY),
                            end = Offset(x + candleWidth / 2, lowY),
                            strokeWidth = 2.dp.toPx()
                        )
                        
                        // Draw body
                        val top = minOf(openY, closeY)
                        val bottom = maxOf(openY, closeY)
                        drawRect(
                            color = color.copy(alpha = drawAlpha),
                            topLeft = Offset(x, top),
                            size = Size(candleWidth, maxOf(bottom - top, 2f)) // Ensure min height
                        )
                    }
                    
                    // Draw tooltip
                    selectedCandle?.let { candle ->
                        val dateText = "Dia " + (candles.indexOf(candle) + 1)
                        val details = "O:${String.format("%.2f", candle.open)} C:${String.format("%.2f", candle.close)}"
                        
                        val rectWidth = 140.dp.toPx()
                        val rectHeight = 60.dp.toPx()
                        val alignLeft = selectedXPos + rectWidth < width
                        val rectX = if (alignLeft) selectedXPos + 10.dp.toPx() else selectedXPos - rectWidth - 10.dp.toPx()
                        
                        drawRoundRect(
                            color = Color(0xFF0F172A).copy(alpha = 0.9f),
                            topLeft = Offset(rectX, 10.dp.toPx()),
                            size = Size(rectWidth, rectHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                        
                        // Paint text logic would go here. However in Canvas, drawing text requires native paint config
                        // We will instead overlay text in a Compose Box
                    }
                }
                
                selectedCandle?.let { candle ->
                    val idx = candles.indexOf(candle)
                    val isLeft = selectedXPos < (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.value / 2)
                    
                    val cal = Calendar.getInstance()
                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    cal.add(Calendar.DAY_OF_YEAR, - (12 - idx))
                    val candleDate = formatter.format(cal.time)

                    Box(modifier = Modifier.fillMaxSize()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                            modifier = Modifier
                                .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                                .padding(top = 10.dp, start = if (isLeft) 20.dp else 0.dp, end = if (!isLeft) 20.dp else 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(candleDate, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Apertura: ${String.format("$%.2f", candle.open)}", color = Color.White, fontSize = 11.sp)
                                Text("Cierre: ${String.format("$%.2f", candle.close)}", color = Color.White, fontSize = 11.sp)
                                Text("Max: ${String.format("$%.2f", candle.high)} | Min: ${String.format("$%.2f", candle.low)}", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun listRandom(min: Float, max: Float): Float {
    return min + (Math.random().toFloat() * (max - min))
}
