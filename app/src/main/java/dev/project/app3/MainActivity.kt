package dev.project.app3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

val DeepTitan = Color(0xFF050507)
val GlassSurface = Color(0xFF101015)
val GlassElevated = Color(0xFF181822)
val BorderMetal = Color(0xFF262633)
val TextLight = Color(0xFFF8F9FA)
val TextMuted = Color(0xFF8E8EA0)
val QuantumCyan = Color(0xFF00E5FF)
val QuantumBlue = Color(0xFF3B82F6)
val EnergyEmerald = Color(0xFF10B981)

data class QuantumHardware(
    val id: Int,
    val title: String,
    val subtitle: String,
    val category: String,
    val tier: String,
    val price: Int,
    val powerDrawWatts: Int,
    val efficiency: Float,
    val thermalStability: Float,
    val description: String,
    val specs: List<Pair<String, String>>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = DeepTitan,
                    surface = GlassSurface,
                    primary = QuantumCyan,
                    secondary = EnergyEmerald
                )
            ) {
                QuantumMarketDashboard()
            }
        }
    }
}

@Composable
fun QuantumMarketDashboard() {
    val hardwareCatalog = remember {
        listOf(
            QuantumHardware(
                id = 1,
                title = "Aetherium X-Core 9",
                subtitle = "Quantum Neural Processor",
                category = "Quantum Units",
                tier = "FLAGSHIP",
                price = 34500,
                powerDrawWatts = 120,
                efficiency = 0.98f,
                thermalStability = 0.94f,
                description = "Флагманский квантовый процессор для вычислений высокой плотности. Поддерживает параллельную обработку петабайтов данных в реальном времени.",
                specs = listOf("Тактовая частота" to "14.2 THz", "Архитектура" to "Qubit-9", "Охлаждение" to "Cryo-Vapor")
            ),
            QuantumHardware(
                id = 2,
                title = "Valkyrie Exo-Frame MK.IV",
                subtitle = "Powered Exoskeleton Unit",
                category = "Exo-Frames",
                tier = "MIL-SPEC",
                price = 19200,
                powerDrawWatts = 85,
                efficiency = 0.91f,
                thermalStability = 0.96f,
                description = "Легкий силовой каркас из титан-углеродного композита. Увеличивает грузоподъемность и скорость реакции оператора на 350%.",
                specs = listOf("Усиление силы" to "4.8x", "Автономность" to "72 часа", "Материал" to "Grade-5 Ti-C")
            ),
            QuantumHardware(
                id = 3,
                title = "Helios Ocular Suite",
                subtitle = "Multi-Spectrum HUD Glasses",
                category = "Neural Links",
                tier = "PRO",
                price = 14800,
                powerDrawWatts = 25,
                efficiency = 0.95f,
                thermalStability = 0.99f,
                description = "Голографическая система визуализации с бесконечным динамическим диапазоном и бесшовным подключением к орбитальным спутникам.",
                specs = listOf("Разрешение" to "Dual 32K Micro", "Частота кадров" to "360 Hz", "Связь" to "Quantum Link")
            ),
            QuantumHardware(
                id = 4,
                title = "CryoVector Matrix-V",
                subtitle = "Sub-Zero Thermal Dissipator",
                category = "Sub-Zero Coolers",
                tier = "INDUSTRIAL",
                price = 8900,
                powerDrawWatts = 45,
                efficiency = 0.97f,
                thermalStability = 1.0f,
                description = "Активная система криогенного охлаждения замкнутого контура. Гарантирует нулевой троттлинг даже при пиковых нагрузках на систему.",
                specs = listOf("Теплоотвод" to "500W TDP", "Хладагент" to "Helium-Liquid", "Уровень шума" to "12 dB")
            ),
            QuantumHardware(
                id = 5,
                title = "Synapse Link Pro",
                subtitle = "Direct Neural Transceiver",
                category = "Neural Links",
                tier = "ELITE",
                price = 22100,
                powerDrawWatts = 30,
                efficiency = 0.96f,
                thermalStability = 0.92f,
                description = "Нейросетевой трансивер нового поколения. Обеспечивает прямую связь между органической нервной системой и квантовым облаком.",
                specs = listOf("Пропускная способность" to "120 TB/s", "Задержка" to "0.001 ms", "Безопасность" to "Biometric Lock")
            )
        )
    }

    val categories = listOf("All Systems", "Quantum Units", "Exo-Frames", "Neural Links", "Sub-Zero Coolers")
    var selectedCategory by remember { mutableStateOf("All Systems") }
    var searchQuery by remember { mutableStateOf("") }
    var coreFrequency by remember { mutableFloatStateOf(0.78f) }
    val activeModules = remember { mutableStateListOf(1, 3) }
    var selectedItemForModal by remember { mutableStateOf<QuantumHardware?>(null) }
    var showSyncBanner by remember { mutableStateOf(false) }

    val filteredList = hardwareCatalog.filter { item ->
        val categoryMatch = selectedCategory == "All Systems" || item.category == selectedCategory
        val searchMatch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true)
        categoryMatch && searchMatch
    }

    val totalCost = hardwareCatalog.filter { it.id in activeModules }.sumOf { it.price }
    val totalPower = hardwareCatalog.filter { it.id in activeModules }.sumOf { it.powerDrawWatts }

    Scaffold(
        containerColor = DeepTitan,
        topBar = {
            QuantumTopBar(
                creditsBalance = 250000 - totalCost,
                activeModulesCount = activeModules.size
            )
        },
        bottomBar = {
            QuantumBottomDock(
                activeCount = activeModules.size,
                totalPrice = totalCost,
                totalPower = totalPower,
                onSyncTrigger = { showSyncBanner = !showSyncBanner }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            item {
                AnimatedVisibility(visible = showSyncBanner) {
                    Surface(
                        color = EnergyEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, EnergyEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Квантовая синхронизация успешна",
                                    color = EnergyEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Все активные модули (${activeModules.size}) интегрированы в центральное ядро",
                                    color = TextLight.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "ЗАКРЫТЬ",
                                color = EnergyEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { showSyncBanner = false }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                QuantumCoreDashboard(
                    powerWatts = totalPower,
                    frequency = coreFrequency,
                    onFrequencyChange = { coreFrequency = it }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Поиск квантовых модулей, процессоров...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuantumCyan,
                            unfocusedBorderColor = BorderMetal,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            val bgAnim by animateColorAsState(
                                if (isSelected) QuantumCyan else GlassSurface,
                                label = "catBg"
                            )
                            Surface(
                                color = bgAnim,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) QuantumCyan else BorderMetal
                                ),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) DeepTitan else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Каталог оборудования",
                        color = TextLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Доступно: ${filteredList.size}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            items(filteredList, key = { it.id }) { item ->
                QuantumHardwareCard(
                    hardware = item,
                    isActive = item.id in activeModules,
                    onToggleActive = {
                        if (item.id in activeModules) {
                            activeModules.remove(item.id)
                        } else {
                            activeModules.add(item.id)
                        }
                    },
                    onCardClick = { selectedItemForModal = item }
                )
            }
        }
    }

    selectedItemForModal?.let { hw ->
        AlertDialog(
            onDismissRequest = { selectedItemForModal = null },
            containerColor = GlassElevated,
            shape = RoundedCornerShape(26.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${hw.category.uppercase()} • ${hw.tier}",
                        color = QuantumCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = hw.title,
                        color = TextLight,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = hw.description,
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    HorizontalDivider(color = BorderMetal)
                    hw.specs.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, color = TextMuted, fontSize = 12.sp)
                            Text(
                                text = value,
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hw.id !in activeModules) activeModules.add(hw.id)
                        selectedItemForModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Интегрировать",
                        color = DeepTitan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
fun QuantumTopBar(creditsBalance: Int, activeModulesCount: Int) {
    Surface(
        color = DeepTitan.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(QuantumCyan)
                )
                Column {
                    Text(
                        text = "AETHERIUM // CORE",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Квантовый узел • Активно ($activeModulesCount)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                color = GlassElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderMetal)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CR",
                        color = QuantumCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%,d".format(creditsBalance),
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun QuantumCoreDashboard(
    powerWatts: Int,
    frequency: Float,
    onFrequencyChange: (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantumWave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, BorderMetal),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            QuantumCyan.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(120f, 100f),
                        radius = 600f
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QUANTUM FLUX TELEMETRY",
                            color = QuantumCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Статус центрального ядра",
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Энергопотребление: $powerWatts W / 500W Max",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 7.dp.toPx()
                            drawArc(
                                color = BorderMetal,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(QuantumCyan, QuantumBlue, EnergyEmerald, QuantumCyan)
                                ),
                                startAngle = -90f,
                                sweepAngle = (powerWatts / 500f).coerceIn(0f, 1f) * 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(powerWatts / 500f * 100).toInt()}%",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "LOAD",
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepTitan.copy(alpha = 0.7f))
                ) {
                    val path = Path()
                    val width = size.width
                    val midY = size.height / 2f
                    val amplitude = size.height * (0.2f + frequency * 0.25f)

                    path.moveTo(0f, midY)
                    var x = 0f
                    while (x <= width) {
                        val y = midY + sin((x / width) * 12f + waveAnim) * amplitude
                        path.lineTo(x, y)
                        x += 5f
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            listOf(QuantumCyan, QuantumBlue, QuantumCyan)
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Частота квантового резонанса",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(3.0f + frequency * 9.0f).toString().take(4)} THz",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = frequency,
                        onValueChange = onFrequencyChange,
                        colors = SliderDefaults.colors(
                            thumbColor = TextLight,
                            activeTrackColor = QuantumCyan,
                            inactiveTrackColor = BorderMetal
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuantumHardwareCard(
    hardware: QuantumHardware,
    isActive: Boolean,
    onToggleActive: () -> Unit,
    onCardClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) EnergyEmerald.copy(alpha = 0.5f) else BorderMetal,
        label = "borderCard"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = QuantumCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hardware.category,
                        color = QuantumCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = hardware.tier,
                    color = if (isActive) EnergyEmerald else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = hardware.title,
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hardware.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = hardware.description,
                color = TextMuted.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricBar(
                    label = "КПД",
                    progress = hardware.efficiency,
                    color = QuantumCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricBar(
                    label = "Стабильность",
                    progress = hardware.thermalStability,
                    color = EnergyEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Стоимость",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "CR %,d".format(hardware.price),
                        color = TextLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleActive,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) EnergyEmerald.copy(alpha = 0.16f) else QuantumCyan,
                        contentColor = if (isActive) EnergyEmerald else DeepTitan
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = if (isActive) "Активен ✓" else "Интегрировать",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBar(label: String, progress: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextMuted, fontSize = 10.sp)
            Text(
                text = "${(progress * 100).toInt()}%",
                color = TextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = DeepTitan,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
fun QuantumBottomDock(
    activeCount: Int,
    totalPrice: Int,
    totalPower: Int,
    onSyncTrigger: () -> Unit
) {
    Surface(
        color = GlassSurface,
        border = BorderStroke(1.dp, BorderMetal),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Модулей: $activeCount • Питание: ${totalPower}W",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "CR %,d".format(totalPrice),
                    color = TextLight,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onSyncTrigger,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextLight),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)
            ) {
                Text(
                    text = "Синхронизация",
                    color = DeepTitan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}