package com.example.javabrewestimator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JavaBrewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showWelcomeScreen by remember { mutableStateOf(true) }
                    var initialBrewMethod by remember { mutableStateOf(BrewMethod.ESPRESSO) }

                    if (showWelcomeScreen) {
                        WelcomeScreen(
                            onNavigateToEspresso = {
                                initialBrewMethod = BrewMethod.ESPRESSO
                                showWelcomeScreen = false
                            },
                            onNavigateToPourOver = {
                                initialBrewMethod = BrewMethod.POUR_OVER
                                showWelcomeScreen = false
                            }
                        )
                    } else {
                        JavaBrewEstimatorScreen(
                            initialMethod = initialBrewMethod
                        )
                    }
                }
            }
        }
    }
}

// --- ENUMS & DATA MODELS ---

enum class BrewMethod(val label: String) {
    ESPRESSO("Espresso"),
    POUR_OVER("Pour Over")
}

enum class ReferenceTab(val title: String) {
    EXTRACTION_PRINCIPLES("Extraction Principles"),
    BEAN_PHILOSOPHY("Bean Philosophy"),
    PROCESSING_SPECTRUM("Processing Spectrum"),
    ROAST_DYNAMICS("Roast Dynamics")
}

enum class RoastLevel(
    val label: String,
    val multiplier: Float,
    val baseColor: Color,
    val highlightColor: Color,
    val creaseColor: Color
) {
    LIGHT(
        label = "Light",
        multiplier = 0.85f,
        baseColor = Color(0xFFC89D72),
        highlightColor = Color(0xFFE6C59E),
        creaseColor = Color(0xFF7A5432)
    ),
    MEDIUM(
        label = "Medium",
        multiplier = 0.98f,
        baseColor = Color(0xFF8D5B3C),
        highlightColor = Color(0xFFB58362),
        creaseColor = Color(0xFF472814)
    ),
    DARK(
        label = "Dark",
        multiplier = 1.10f,
        baseColor = Color(0xFF4A2E20),
        highlightColor = Color(0xFF785543),
        creaseColor = Color(0xFF1F110A)
    )
}

// --- COFFEE & CREAM PASTEL THEME ---

private val LightPastelColors = lightColorScheme(
    primary = Color(0xFFB07D62),         // Warm Pastel Caramel
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2E2D2),// Soft Latte Foam
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF4EE6D4),       // Cyan/Teal (#4ee6d4)
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF5F0),      // Warm Cream
    onBackground = Color(0xFF3D312A),    // Deep Espresso Text
    surface = Color(0xFFF3ECE4),         // Oat Milk Card Surface
    onSurface = Color(0xFF3D312A),
    surfaceVariant = Color(0xFFE6DCD2),
    onSurfaceVariant = Color(0xFF6D5D53)
)

private val DarkPastelColors = darkColorScheme(
    primary = Color(0xFFDEB896),         // Soft Whipped Caramel
    onPrimary = Color(0xFF2B1B12),
    primaryContainer = Color(0xFF4A372D),// Muted Mocha Container
    onPrimaryContainer = Color(0xFFF5E6D8),
    secondary = Color(0xFF4EE6D4),       // Cyan/Teal (#4ee6d4)
    onSecondary = Color(0xFF1C2D24),
    background = Color(0xFF1E1916),      // Dark Roast Backdrop
    onBackground = Color(0xFFEFE6DD),    // Steamed Milk Text
    surface = Color(0xFF29221E),         // Cocoa Surface
    onSurface = Color(0xFFEFE6DD),
    surfaceVariant = Color(0xFF3B312B),
    onSurfaceVariant = Color(0xFFBDAE9F)
)

@Composable
fun JavaBrewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPastelColors else LightPastelColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// --- EXTRACTION MATH ENGINE ---

fun calculateExtractionYield(
    method: BrewMethod,
    roast: RoastLevel,
    beanWeightGrams: Float,
    liquidYieldGrams: Float,
    brewTimeSec: Float,
    pourOverRatio: Float,
    drawdownTimeMin: Float,
    waterTempC: Float,
    grinderRpm: Float,
    beanAgeDays: Float
): Float {
    val rpmFinesMultiplier = 1.0f + ((grinderRpm - 800f) / 700f) * 0.18f
    val ageMultiplier = 0.82f + ((beanAgeDays - 1f) / 29f) * 0.23f
    val tempFactor = waterTempC / 92f

    val baseExtraction = when (method) {
        BrewMethod.ESPRESSO -> {
            val safeDose = beanWeightGrams.coerceAtLeast(1f)
            (liquidYieldGrams / safeDose) * (brewTimeSec / 30f) * tempFactor * 10.2f
        }
        BrewMethod.POUR_OVER -> {
            (pourOverRatio / 15f) * (drawdownTimeMin / 3.5f) * tempFactor * 24.4f
        }
    }

    return (baseExtraction * roast.multiplier * rpmFinesMultiplier * ageMultiplier).coerceIn(10f, 30f)
}

fun calculateBalancedBrewTime(
    roast: RoastLevel,
    beanWeightGrams: Float,
    liquidYieldGrams: Float,
    waterTempC: Float,
    grinderRpm: Float,
    beanAgeDays: Float
): Float {
    val rpmFinesMultiplier = 1.0f + ((grinderRpm - 800f) / 700f) * 0.18f
    val ageMultiplier = 0.82f + ((beanAgeDays - 1f) / 29f) * 0.23f
    val tempFactor = waterTempC / 92f
    val safeDose = beanWeightGrams.coerceAtLeast(1f)
    val ratio = (liquidYieldGrams / safeDose).coerceAtLeast(0.5f)

    val targetYield = 21.0f
    val denominator = ratio * tempFactor * 10.2f * roast.multiplier * rpmFinesMultiplier * ageMultiplier
    val idealSec = (targetYield * 30f) / denominator

    return idealSec.roundToInt().toFloat().coerceIn(15f, 50f)
}

fun calculateBalancedPourOverRatio(
    roast: RoastLevel,
    drawdownTimeMin: Float,
    waterTempC: Float,
    grinderRpm: Float,
    beanAgeDays: Float
): Float {
    val rpmFinesMultiplier = 1.0f + ((grinderRpm - 800f) / 700f) * 0.18f
    val ageMultiplier = 0.82f + ((beanAgeDays - 1f) / 29f) * 0.23f
    val tempFactor = waterTempC / 92f
    val drawdownFactor = (drawdownTimeMin / 3.5f).coerceAtLeast(0.1f)

    val targetYield = 21.0f
    val denominator = drawdownFactor * tempFactor * 24.4f * roast.multiplier * rpmFinesMultiplier * ageMultiplier
    val idealRatio = (targetYield * 15f) / denominator

    val snapped = (idealRatio * 5f).roundToInt() / 5f
    return snapped.coerceIn(13.0f, 17.0f)
}

private const val PREFS_NAME = "java_brew_prefs"
private const val KEY_GRINDER_RPM = "key_grinder_rpm"
private const val KEY_WATER_TEMP = "key_water_temp"

// --- WELCOME DASHBOARD SCREEN ---

fun getBackgroundDrawableForTime(time: LocalTime): Int {
    val hour = time.hour
    return when (hour) {
        in 1..11 -> R.drawable.morning     // 01:00 to 11:59
        in 12..17 -> R.drawable.afternoon  // 12:00 to 17:59
        else -> R.drawable.evening         // 18:00 to 00:59
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToEspresso: () -> Unit,
    onNavigateToPourOver: () -> Unit,
    timeProvider: () -> LocalTime = { LocalTime.now() }
) {
    val backgroundDrawableRes = remember(timeProvider) {
        getBackgroundDrawableForTime(timeProvider())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Time-based Full-bleed Background
        Image(
            painter = painterResource(id = backgroundDrawableRes),
            contentDescription = "Time-based Welcome Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Coffee Image (Positioned higher up, rendered BEHIND buttons)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        val drawable = ContextCompat.getDrawable(ctx, R.drawable.coffee_anim)
                        setImageDrawable(drawable)
                        if (drawable is android.graphics.drawable.Animatable) {
                            drawable.start()
                        }
                    }
                },
                modifier = Modifier.size(280.dp)
            )
        }

        // Layer 3: Dark Gradient & Navigation Buttons (Rendered ON TOP of the animation)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
                .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 59.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateToEspresso,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "Espresso",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onNavigateToPourOver,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "Pour Over",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun WelcomeScreenPreview() {
    JavaBrewTheme {
        WelcomeScreen(
            onNavigateToEspresso = {},
            onNavigateToPourOver = {}
        )
    }
}

// --- MAIN UI SCREEN ---

@Composable
fun JavaBrewEstimatorScreen(
    initialMethod: BrewMethod = BrewMethod.ESPRESSO
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val savedRpm = remember { sharedPrefs.getFloat(KEY_GRINDER_RPM, 900f) }
    val savedTemp = remember { sharedPrefs.getFloat(KEY_WATER_TEMP, 93f) }

    var brewMethod by remember { mutableStateOf(initialMethod) }
    var activeReferenceTab by remember { mutableStateOf<ReferenceTab?>(null) }

    var espressoRoast by remember { mutableStateOf(RoastLevel.MEDIUM) }
    var pourOverRoast by remember { mutableStateOf(RoastLevel.LIGHT) }
    val activeRoast = if (brewMethod == BrewMethod.ESPRESSO) espressoRoast else pourOverRoast

    var beanWeight by remember { mutableFloatStateOf(18f) }
    var isManualYieldOverride by remember { mutableStateOf(false) }
    var liquidYield by remember { mutableFloatStateOf(36f) }
    var waterTemp by remember { mutableFloatStateOf(savedTemp) }
    var grinderRpm by remember { mutableFloatStateOf(savedRpm) }
    var beanAge by remember { mutableFloatStateOf(14f) }

    var drawdownTime by remember { mutableFloatStateOf(3.5f) }

    var brewTime by remember {
        mutableFloatStateOf(
            calculateBalancedBrewTime(
                roast = RoastLevel.MEDIUM,
                beanWeightGrams = 18f,
                liquidYieldGrams = 36f,
                waterTempC = savedTemp,
                grinderRpm = savedRpm,
                beanAgeDays = 14f
            )
        )
    }

    var pourOverRatio by remember {
        mutableFloatStateOf(
            calculateBalancedPourOverRatio(
                roast = RoastLevel.LIGHT,
                drawdownTimeMin = 3.5f,
                waterTempC = savedTemp,
                grinderRpm = savedRpm,
                beanAgeDays = 14f
            )
        )
    }

    val onRpmChange: (Float) -> Unit = { newRpm ->
        val rounded = ((newRpm / 100f).roundToInt() * 100).toFloat().coerceIn(800f, 1500f)
        grinderRpm = rounded
        sharedPrefs.edit { putFloat(KEY_GRINDER_RPM, rounded) }

        brewTime = calculateBalancedBrewTime(
            roast = espressoRoast,
            beanWeightGrams = beanWeight,
            liquidYieldGrams = liquidYield,
            waterTempC = waterTemp,
            grinderRpm = rounded,
            beanAgeDays = beanAge
        )

        pourOverRatio = calculateBalancedPourOverRatio(
            roast = pourOverRoast,
            drawdownTimeMin = drawdownTime,
            waterTempC = waterTemp,
            grinderRpm = rounded,
            beanAgeDays = beanAge
        )
    }

    val onTempChange: (Float) -> Unit = { newTemp ->
        val rounded = newTemp.roundToInt().toFloat()
        waterTemp = rounded
        sharedPrefs.edit { putFloat(KEY_WATER_TEMP, rounded) }

        brewTime = calculateBalancedBrewTime(
            roast = espressoRoast,
            beanWeightGrams = beanWeight,
            liquidYieldGrams = liquidYield,
            waterTempC = rounded,
            grinderRpm = grinderRpm,
            beanAgeDays = beanAge
        )

        pourOverRatio = calculateBalancedPourOverRatio(
            roast = pourOverRoast,
            drawdownTimeMin = drawdownTime,
            waterTempC = rounded,
            grinderRpm = grinderRpm,
            beanAgeDays = beanAge
        )
    }

    val onAgeChange: (Float) -> Unit = { newAge ->
        val roundedAge = newAge.roundToInt().toFloat()
        beanAge = roundedAge

        brewTime = calculateBalancedBrewTime(
            roast = espressoRoast,
            beanWeightGrams = beanWeight,
            liquidYieldGrams = liquidYield,
            waterTempC = waterTemp,
            grinderRpm = grinderRpm,
            beanAgeDays = roundedAge
        )

        pourOverRatio = calculateBalancedPourOverRatio(
            roast = pourOverRoast,
            drawdownTimeMin = drawdownTime,
            waterTempC = waterTemp,
            grinderRpm = grinderRpm,
            beanAgeDays = roundedAge
        )
    }

    val isDark = isSystemInDarkTheme()
    val espressoSliderColor = if (isDark) Color(0xFFC89B73) else Color(0xFF8C583E)
    val pourOverSliderColor = if (isDark) Color(0xFFEDD0B4) else Color(0xFFC99A7E)
    val activeSliderColor = if (brewMethod == BrewMethod.ESPRESSO) espressoSliderColor else pourOverSliderColor

    val espressoAgeSliderColor = if (
        (espressoRoast == RoastLevel.DARK && (beanAge !in 8f..28f)) ||
        (espressoRoast == RoastLevel.MEDIUM && (beanAge !in 11f..35f)) ||
        (espressoRoast == RoastLevel.LIGHT && (beanAge !in 15f..56f))
    ) Color(0xFFF2CA8D) else espressoSliderColor

    val pourOverAgeSliderColor = if (
        (pourOverRoast == RoastLevel.DARK && (beanAge !in 6f..35f)) ||
        (pourOverRoast == RoastLevel.MEDIUM && (beanAge !in 8f..42f)) ||
        (pourOverRoast == RoastLevel.LIGHT && (beanAge !in 11f..56f))
    ) Color(0xFFF2CA8D) else pourOverSliderColor

    val currentExtraction = calculateExtractionYield(
        method = brewMethod,
        roast = activeRoast,
        beanWeightGrams = beanWeight,
        liquidYieldGrams = liquidYield,
        brewTimeSec = brewTime,
        pourOverRatio = pourOverRatio,
        drawdownTimeMin = drawdownTime,
        waterTempC = waterTemp,
        grinderRpm = grinderRpm,
        beanAgeDays = beanAge
    )

    val scrollState = rememberScrollState()

    LaunchedEffect(activeReferenceTab, brewMethod) {
        scrollState.scrollTo(0)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp)
        ) {
            Text(
                text = "Java Brew Estimator",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrewMethodToggle(
                    selectedMethod = if (activeReferenceTab != null) null else brewMethod,
                    onMethodSelected = { clickedMethod ->
                        brewMethod = clickedMethod
                        activeReferenceTab = null
                    }
                )

                if (activeReferenceTab != null) {
                    ReferenceButtonsGrid(
                        activeTab = activeReferenceTab,
                        onTabSelected = { activeReferenceTab = it }
                    )
                    ReferenceContentCard(tab = activeReferenceTab!!)
                } else {
                    RoastLevelSelector(
                        selectedRoast = activeRoast,
                        onRoastSelected = { selected ->
                            if (brewMethod == BrewMethod.ESPRESSO) {
                                espressoRoast = selected
                                brewTime = calculateBalancedBrewTime(
                                    roast = selected,
                                    beanWeightGrams = beanWeight,
                                    liquidYieldGrams = liquidYield,
                                    waterTempC = waterTemp,
                                    grinderRpm = grinderRpm,
                                    beanAgeDays = beanAge
                                )
                            } else {
                                pourOverRoast = selected
                                pourOverRatio = calculateBalancedPourOverRatio(
                                    roast = selected,
                                    drawdownTimeMin = drawdownTime,
                                    waterTempC = waterTemp,
                                    grinderRpm = grinderRpm,
                                    beanAgeDays = beanAge
                                )
                            }
                        }
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Brewing Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = activeSliderColor
                            )

                            val activeAgeSliderColor = if (brewMethod == BrewMethod.ESPRESSO) espressoAgeSliderColor else pourOverAgeSliderColor

                            ParameterSlider(
                                label = "Age of Beans",
                                valueText = "${beanAge.roundToInt()} days",
                                value = beanAge,
                                onValueChange = onAgeChange,
                                valueRange = 1f..56f,
                                steps = 54,
                                sliderColor = activeAgeSliderColor
                            )

                            GrinderAndTempRow(
                                grinderRpm = grinderRpm,
                                onRpmChange = onRpmChange,
                                waterTemp = waterTemp,
                                onTempChange = onTempChange,
                                sliderColor = activeSliderColor
                            )

                            AnimatedVisibility(visible = brewMethod == BrewMethod.ESPRESSO) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    ParameterSlider(
                                        label = "Bean Weight (Dose)",
                                        valueText = "${beanWeight.roundToInt()} g",
                                        value = beanWeight,
                                        onValueChange = { newWeight ->
                                            val roundedWeight = newWeight.roundToInt().toFloat()
                                            beanWeight = roundedWeight
                                            if (!isManualYieldOverride) {
                                                liquidYield = (roundedWeight * 2f).coerceIn(18f, 60f)
                                            }
                                        },
                                        valueRange = 14f..22f,
                                        steps = 7,
                                        sliderColor = espressoSliderColor
                                    )

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Liquid Yield",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isManualYieldOverride)
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    else
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.clickable {
                                                        isManualYieldOverride = !isManualYieldOverride
                                                        if (!isManualYieldOverride) {
                                                            liquidYield = (beanWeight * 2f).coerceIn(18f, 60f)
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (isManualYieldOverride) "Manual (Tap for 2x)" else "Auto 2x Dose",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isManualYieldOverride)
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        else
                                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${liquidYield.roundToInt()} g",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Slider(
                                            value = liquidYield,
                                            onValueChange = { newYield ->
                                                isManualYieldOverride = true
                                                liquidYield = newYield.roundToInt().toFloat()
                                            },
                                            valueRange = 18f..60f,
                                            steps = 41,
                                            colors = SliderDefaults.colors(
                                                thumbColor = espressoSliderColor,
                                                activeTrackColor = espressoSliderColor,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Brew Time",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.clickable {
                                                        brewTime = calculateBalancedBrewTime(
                                                            roast = espressoRoast,
                                                            beanWeightGrams = beanWeight,
                                                            liquidYieldGrams = liquidYield,
                                                            waterTempC = waterTemp,
                                                            grinderRpm = grinderRpm,
                                                            beanAgeDays = beanAge
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Match Ideal (21%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${brewTime.roundToInt()} sec",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Slider(
                                            value = brewTime,
                                            onValueChange = { brewTime = it.roundToInt().toFloat() },
                                            valueRange = 15f..50f,
                                            steps = 34,
                                            colors = SliderDefaults.colors(
                                                thumbColor = espressoSliderColor,
                                                activeTrackColor = espressoSliderColor,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = brewMethod == BrewMethod.POUR_OVER) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    ParameterSlider(
                                        label = "Drawdown Time",
                                        valueText = String.format(Locale.US, "%.1f min", drawdownTime),
                                        value = drawdownTime,
                                        onValueChange = { newMin ->
                                            val rounded = ((newMin * 2f).roundToInt() / 2f).coerceIn(2.0f, 5.0f)
                                            drawdownTime = rounded
                                            pourOverRatio = calculateBalancedPourOverRatio(
                                                roast = pourOverRoast,
                                                drawdownTimeMin = rounded,
                                                waterTempC = waterTemp,
                                                grinderRpm = grinderRpm,
                                                beanAgeDays = beanAge
                                            )
                                        },
                                        valueRange = 2.0f..5.0f,
                                        steps = 5,
                                        sliderColor = pourOverSliderColor
                                    )

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Water:Bean Ratio",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.clickable {
                                                        pourOverRatio = calculateBalancedPourOverRatio(
                                                            roast = pourOverRoast,
                                                            drawdownTimeMin = drawdownTime,
                                                            waterTempC = waterTemp,
                                                            grinderRpm = grinderRpm,
                                                            beanAgeDays = beanAge
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Match Ideal (21%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = String.format(Locale.US, "%.1fx", pourOverRatio),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Slider(
                                            value = pourOverRatio,
                                            onValueChange = { newRatio ->
                                                pourOverRatio = ((newRatio * 5f).roundToInt() / 5f).coerceIn(13.0f, 17.0f)
                                            },
                                            valueRange = 13.0f..17.0f,
                                            steps = 19,
                                            colors = SliderDefaults.colors(
                                                thumbColor = pourOverSliderColor,
                                                activeTrackColor = pourOverSliderColor,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ExtractionStatusBanner(extractionYield = currentExtraction)

                    ExtractionChartCard(
                        brewMethod = brewMethod,
                        roastLevel = activeRoast,
                        beanWeight = beanWeight,
                        liquidYield = liquidYield,
                        brewTime = brewTime,
                        pourOverRatio = pourOverRatio,
                        drawdownTime = drawdownTime,
                        waterTemp = waterTemp,
                        grinderRpm = grinderRpm,
                        beanAge = beanAge,
                        currentExtraction = currentExtraction,
                        lineColor = activeSliderColor
                    )

                    ReferenceButtonsGrid(
                        activeTab = activeReferenceTab,
                        onTabSelected = { activeReferenceTab = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun BrewMethodToggle(
    selectedMethod: BrewMethod?,
    onMethodSelected: (BrewMethod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BrewMethod.entries.forEach { method ->
            val isSelected = method == selectedMethod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onMethodSelected(method) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = method.label,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReferenceButtonsGrid(
    activeTab: ReferenceTab?,
    onTabSelected: (ReferenceTab) -> Unit
) {
    val tabs = ReferenceTab.entries
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.take(2).forEach { tab ->
                ReferenceGridButton(
                    tab = tab,
                    isSelected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.drop(2).forEach { tab ->
                ReferenceGridButton(
                    tab = tab,
                    isSelected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ReferenceGridButton(
    tab: ReferenceTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReferenceContentCard(tab: ReferenceTab) {
    val bodyTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            when (tab) {
                ReferenceTab.EXTRACTION_PRINCIPLES -> ExtractionPrinciplesContent(bodyTextStyle)
                ReferenceTab.BEAN_PHILOSOPHY -> BeanPhilosophyContent(bodyTextStyle)
                ReferenceTab.PROCESSING_SPECTRUM -> ProcessingSpectrumContent(bodyTextStyle)
                ReferenceTab.ROAST_DYNAMICS -> RoastDynamicsContent(bodyTextStyle)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "©kpy 2026",
                style = bodyTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExtractionPrinciplesContent(bodyTextStyle: TextStyle) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Text(
        text = "A roasted coffee bean is roughly 30% water-soluble; the remaining 70% is insoluble cellulose and plant fiber. The \"extraction zone\" measures the Extraction Yield (EY)—the percentage of the coffee bean's total dry mass that actually dissolves into the water. The global standard for a balanced extraction targets 18% to 22%, with modern specialty coffee often aiming specifically for the 20% to 22% sweet spot. This narrow window is dictated by the chronological order in which water dissolves coffee compounds:",
        style = bodyTextStyle,
        color = textColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BulletItem("Under 18% (Under-extracted): ", "Water dissolves fruit acids and salts first, resulting in an overwhelmingly sour, sharp, and slightly salty cup lacking in sugars.", bodyTextStyle)
        BulletItem("20% to 22% (The Ideal Zone): ", "Water dissolves complex sugars and carbohydrates, yielding a balanced cup where sweetness harmonizes with bright fruit acids.", bodyTextStyle)
        BulletItem("Above 22% (Over-extracted): ", "The slowest-dissolving heavy plant alkaloids, tannins, and dry distillates overwhelm the cup, creating harsh, dry, and bitter flavors that mask delicate sweetness.", bodyTextStyle)
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )

    Text(
        text = "In the Java Brew Estimator, every slider manipulates this chemical reality:",
        style = bodyTextStyle,
        color = textColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BulletItem("Age of Beans: ", "Accounts for trapped carbon dioxide; highly fresh beans off-gas violently, pushing water away and actively suppressing extraction, while rested beans allow effortless water penetration.", bodyTextStyle)
        BulletItem("Grinder: ", "Dictates particle distribution—spinning your variable-speed grinder faster shatters the beans more violently, creating microscopic \"fines\" that drastically increase surface area and drive extraction upward.", bodyTextStyle)
        BulletItem("Water Temp: ", "Acts as the kinetic engine; raising the temperature accelerates the dissolution of stubborn compounds, which is why dense, lightly roasted beans demand higher heat.", bodyTextStyle)
        BulletItem("Liquid Yield & Water:Bean Ratio: ", "Determine the sheer volume of the solvent (for espresso and pour over, respectively); pushing more water through the coffee bed naturally extracts a higher total mass.", bodyTextStyle)
        BulletItem("Brew Time & Drawdown Time: ", "Control contact duration, ensuring the water has the necessary physical time to break down cellular structures and pull the targeted 21% yield into your cup.", bodyTextStyle)
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )

    Text(
        text = "Sources",
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Text(
        text = "This standard is not subjective; it is based on decades of empirical sensory science and chemistry.",
        style = bodyTextStyle,
        color = textColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BulletItem("Dr. E.E. Lockhart and the Coffee Brewing Institute (1950s): ", "The 18–22% standard was originally established by Dr. E.E. Lockhart at MIT. Through extensive consumer taste-testing, he proved that human preference consistently peaked when exactly 18% to 22% of the coffee bean's mass was dissolved into the water.", bodyTextStyle)
        BulletItem("The Specialty Coffee Association (SCA): ", "The SCA adopted Lockhart's research to create the Coffee Brewing Control Chart, which remains the global standard for coffee professionals, Q-Graders, and competitive baristas. The chart formally defines the \"Golden Cup Standard\" as an extraction yield of 18–22%.", bodyTextStyle)
        BulletItem("Scott Rao (\"The Professional Barista's Handbook\"): ", "A leading modern coffee scientist and author who popularized the use of digital refractometers to measure Total Dissolved Solids (TDS). Rao's contemporary research reinforced that while 18-22% is the baseline, modern precision flat-burr grinders often produce the highest sensory quality directly in the 20-22% window.", bodyTextStyle)
    }
}

@Composable
private fun BeanPhilosophyContent(bodyTextStyle: TextStyle) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Text(
        text = "Coffee flavor is fundamentally shaped by the intersection of terroir (the soil, climate, and elevation where it grows) and genetics (the botanical variety of the plant). Heirloom varieties like Typica and Bourbon provide the classic, sweet baseline of Arabica coffee. Rare mutations like Geisha (famed in Panama) offer highly complex, perfume-like aromatics, while sturdy hybrids like SL-28 (from Kenya) are engineered for pronounced, juicy acidity.",
        style = bodyTextStyle,
        color = textColor
    )

    Text(
        text = "How you choose to brew these beans determines how you will perceive them. Espresso acts as a magnifying glass—because it extracts at a highly concentrated 1:2 ratio under 9 bars of pressure, it amplifies body, intense sweetness, and sharp acidity. Conversely, pour over acts as a prism—diluting the extraction at a 1:15 ratio using only gravity, it separates complex flavors, allowing delicate floral aromas and subtle tea-like clarities to shine without being overpowered.",
        style = bodyTextStyle,
        color = textColor
    )

    Text(
        text = "When genetics interact with regional terroirs, signature profiles emerge:",
        style = bodyTextStyle,
        color = textColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BulletItem("Ethiopia: ", "Produces highly floral coffees with delicate notes of jasmine, bergamot, and blueberry (a prism-like pour over perfectly separates these delicate florals).", bodyTextStyle)
        BulletItem("Kenya: ", "Yields a bolder, juicy mouthfeel defined by bright blackcurrant, grapefruit, and savory acidity.", bodyTextStyle)
        BulletItem("Colombia: ", "Serves as a versatile crowd-pleaser, offering a medium body with balanced notes of red apple, milk chocolate, and panela.", bodyTextStyle)
        BulletItem("Brazil: ", "The global baseline for traditional espresso, featuring low acidity and a heavy body dominated by dark chocolate, roasted nuts, and caramel, which the espresso's magnifying glass transforms into a thick, syrupy shot.", bodyTextStyle)
        BulletItem("Panama: ", "The premier micro-climate famed for cultivating the Geisha varietal, producing intensely complex, floral, and sweet profiles.", bodyTextStyle)
        BulletItem("Guatemala: ", "Boasts rich volcanic soil and high-altitude microclimates, producing remarkably clean coffees with crisp apple acidity, floral aromas, and a sweet cocoa finish.", bodyTextStyle)
        BulletItem("Honduras: ", "Delivers a dynamic profile, showcasing a syrupy body layered with caramel, tropical fruits, and a balanced, lively acidity.", bodyTextStyle)
        BulletItem("Indonesia (Sumatra): ", "Produces coffees with exceptionally low acidity and a heavy, syrupy body, defined by savory, earthy, tobacco, and dark spice characteristics that yield weighty espressos.", bodyTextStyle)
    }
}

@Composable
private fun ProcessingSpectrumContent(bodyTextStyle: TextStyle) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Text(
        text = "Processing refers to how the coffee seed is removed from the fruit cherry, profoundly altering the chemical precursors available for roasting. Just as with terroir, your brewing method changes the perception of these processed beans. A natural processed bean pulled as an espresso might taste like an overwhelming punch of dense blueberry syrup, while that exact same bean brewed as a pour over transforms into a light, delicate cup resembling sweet strawberry black tea.",
        style = bodyTextStyle,
        color = textColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProcessItem(
            title = "Washed (Wet Process): ",
            methodDesc = "The fruit is entirely depulped and fermented in water tanks to remove all mucilage before drying. ",
            profileDesc = "Extremely clean, vibrant, and transparent. Highlights the bean's intrinsic terroir and bright acidity with tea-like clarity. Ideal for pour overs seeking prism-like separation.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Natural (Dry Process): ",
            methodDesc = "The coffee is dried whole with the cherry skin and fruit fully intact, allowing sugars to ferment directly into the seed. ",
            profileDesc = "Heavy body, intensely sweet, and fruit-forward. Often bursting with notes of strawberry, blueberry, or fermented wine. Produces wild, syrupy espressos.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Honey Process: ",
            methodDesc = "The skin is removed, but varying amounts of the sticky, sugar-rich mucilage are left on the bean during drying (classified as White, Yellow, Red, or Black Honey). ",
            profileDesc = "A perfect bridge between washed and natural. Pronounced sweetness, medium-heavy body, and subdued acidity with notes of stone fruit and brown sugar.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Wet-Hulled (Giling Basah): ",
            methodDesc = "Traditional to Indonesia, the parchment is removed while the bean is still highly moist, exposing the soft seed to the elements during final drying. ",
            profileDesc = "Very low acidity, intense syrupy body, with savory, cedar, earthy, and spicy notes.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Anaerobic Fermentation: ",
            methodDesc = "Cherries are sealed in oxygen-deprived tanks, forcing yeast and bacteria to metabolize sugars differently, producing unique lactic or malic acids. ",
            profileDesc = "Wild, highly exotic, and creamy. Often features cinnamon, gingerbread, bubblegum, or intense tropical fruit.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Carbonic Maceration: ",
            methodDesc = "Borrowed from winemaking, whole cherries are fermented in a sealed, carbon-dioxide-flushed environment. ",
            profileDesc = "Bright, crisp, and intensely red-fruit focused, often resembling fine wine or boozy cherry liqueur.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Thermal Shock: ",
            methodDesc = "Cherries undergo alternating hot and cold water baths to open the bean's cellular pores, absorbing fermentation flavors before rapidly sealing them in. ",
            profileDesc = "Uncannily distinct and artificial-seeming clarity, such as unmistakable peach, mango, or lemongrass.",
            bodyTextStyle = bodyTextStyle
        )
        ProcessItem(
            title = "Co-Fermented / Infused: ",
            methodDesc = "Fermenting coffee cherries alongside other fruits (strawberries, peaches) or yeast cultures. ",
            profileDesc = "Dominant, intense fruit flavors that mimic the infused ingredient, pushing the boundaries of traditional coffee tasting.",
            bodyTextStyle = bodyTextStyle
        )
    }
}

@Composable
private fun RoastDynamicsContent(bodyTextStyle: TextStyle) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Text(
        text = "The roasting process physically alters the coffee bean's cellular matrix and dictates how the coffee behaves as it ages. During roasting, massive amounts of carbon dioxide are generated and trapped inside the bean. The rate at which this gas escapes—and the exact point at which the coffee reaches its peak flavor—depends entirely on the roast degree and the brewing method.",
        style = bodyTextStyle,
        color = textColor
    )

    Text(
        text = "Light roasts are dropped from the roaster early, meaning they retain a dense, unexpanded cellular structure. The trapped carbon dioxide escapes very slowly through tightly packed cell walls. Conversely, high roasting temperatures aggressively rupture the cell structures of dark roasts, leaving the beans brittle, porous, and coated with surface oils. Carbon dioxide escapes from dark roasts rapidly during the first few days, but the direct exposure of their surface lipids to ambient oxygen accelerates oxidation, causing them to go stale much faster than light roasts.",
        style = bodyTextStyle,
        color = textColor
    )

    Text(
        text = "Because espresso extraction relies on 9 bars of pressurized water, pulling a shot too early off-roast causes violent degassing. This turbulent resistance leads to micro-channeling, excessive frothy crema, and an aggressively sour, under-extracted cup. Pour overs are brewed at atmospheric pressure, allowing escaping gas to vent naturally during a 45-second bloom phase, which means filter coffee requires significantly less resting time to reach its ideal window.",
        style = bodyTextStyle,
        color = textColor
    )

    Spacer(modifier = Modifier.height(4.dp))

    RoastAgingTable()
}

@Composable
private fun RoastAgingTable() {
    val rows = listOf(
        Triple("Light", "Minimum Rest", "10 days" to "7 days"),
        Triple("Light", "Peak Window", "14–35 days" to "10–45 days"),
        Triple("Light", "Max Usable Age", "8 weeks" to "12 weeks"),
        Triple("Medium", "Minimum Rest", "7 days" to "5 days"),
        Triple("Medium", "Peak Window", "10–28 days" to "7–30 days"),
        Triple("Medium", "Max Usable Age", "6 weeks" to "8 weeks"),
        Triple("Dark", "Minimum Rest", "5 days" to "3 days"),
        Triple("Dark", "Peak Window", "7–21 days" to "5–21 days"),
        Triple("Dark", "Max Usable Age", "5 weeks" to "6 weeks")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Roast Level & Metric",
                modifier = Modifier.weight(1.45f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Espresso",
                modifier = Modifier.weight(0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Pour Over",
                modifier = Modifier.weight(0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        rows.forEachIndexed { index, (roast, metric, values) ->
            val (espressoVal, pourOverVal) = values
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(roast)
                        }
                        append("\n")
                        append(metric)
                    },
                    modifier = Modifier.weight(1.45f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = espressoVal,
                    modifier = Modifier.weight(0.9f),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pourOverVal,
                    modifier = Modifier.weight(0.9f),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = if ((index + 1) % 3 == 0) 1.5.dp else 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun BulletItem(
    boldPrefix: String,
    description: String,
    bodyTextStyle: TextStyle
) {
    Text(
        text = buildAnnotatedString {
            append("° ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldPrefix)
            }
            append(description)
        },
        style = bodyTextStyle,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ProcessItem(
    title: String,
    methodDesc: String,
    profileDesc: String,
    bodyTextStyle: TextStyle
) {
    Text(
        text = buildAnnotatedString {
            append("° ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(title)
            }
            append(methodDesc)
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)) {
                append("Cup Profile: ")
            }
            append(profileDesc)
        },
        style = bodyTextStyle,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun RoastLevelSelector(
    selectedRoast: RoastLevel,
    onRoastSelected: (RoastLevel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoastLevel.entries.forEach { roast ->
            val isSelected = roast == selectedRoast
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRoastSelected(roast) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoffeeBeanCanvas(
                        roast = roast,
                        modifier = Modifier.size(36.2.dp)
                    )
                    Text(
                        text = roast.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CoffeeBeanCanvas(
    roast: RoastLevel,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        rotate(degrees = -22f, pivot = center) {
            drawOval(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(w * 0.18f, h * 0.12f),
                size = Size(w * 0.68f, h * 0.82f)
            )

            val beanBrush = Brush.radialGradient(
                colors = listOf(roast.highlightColor, roast.baseColor, roast.creaseColor),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            )
            drawOval(
                brush = beanBrush,
                topLeft = Offset(w * 0.15f, h * 0.08f),
                size = Size(w * 0.68f, h * 0.82f)
            )

            drawOval(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(w * 0.26f, h * 0.18f),
                size = Size(w * 0.18f, h * 0.30f)
            )

            val creasePath = Path().apply {
                moveTo(w * 0.49f, h * 0.10f)
                cubicTo(
                    w * 0.34f, h * 0.36f,
                    w * 0.64f, h * 0.62f,
                    w * 0.49f, h * 0.88f
                )
            }
            drawPath(
                path = creasePath,
                color = roast.creaseColor,
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun ParameterSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    sliderColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun GrinderAndTempRow(
    grinderRpm: Float,
    onRpmChange: (Float) -> Unit,
    waterTemp: Float,
    onTempChange: (Float) -> Unit,
    sliderColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grinder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${grinderRpm.roundToInt()} RPM",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Slider(
                value = grinderRpm,
                onValueChange = onRpmChange,
                valueRange = 800f..1500f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = sliderColor,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Water Temp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${waterTemp.roundToInt()} °C",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Slider(
                value = waterTemp,
                onValueChange = onTempChange,
                valueRange = 85f..98f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = sliderColor,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun ExtractionStatusBanner(extractionYield: Float) {
    val isUnder = extractionYield < 20.0f
    val isOver = extractionYield > 22.0f

    val statusText: String
    val containerColor: Color
    val contentColor: Color

    val darkTheme = isSystemInDarkTheme()
    when {
        isUnder -> {
            statusText = "Under-extracted (Sour)"
            containerColor = if (darkTheme) Color(0xFF4A3D28) else Color(0xFFFAE6C8)
            contentColor = if (darkTheme) Color(0xFFFFDF9E) else Color(0xFF6B4C1B)
        }
        isOver -> {
            statusText = "Over-extracted (Bitter)"
            containerColor = if (darkTheme) Color(0xFF4D2B2B) else Color(0xFFF9D6D5)
            contentColor = if (darkTheme) Color(0xFFFFB4B0) else Color(0xFF732624)
        }
        else -> {
            statusText = "Balanced (Ideal)"
            containerColor = if (darkTheme) Color(0xFF1D4540) else Color(0xFFE0F8F5)
            contentColor = if (darkTheme) Color(0xFF80F0E4) else Color(0xFF0F7A6E)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Estimated Extraction Yield",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Text(
                text = String.format(Locale.US, "%.1f%%", extractionYield),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ExtractionChartCard(
    brewMethod: BrewMethod,
    roastLevel: RoastLevel,
    beanWeight: Float,
    liquidYield: Float,
    brewTime: Float,
    pourOverRatio: Float,
    drawdownTime: Float,
    waterTemp: Float,
    grinderRpm: Float,
    beanAge: Float,
    currentExtraction: Float,
    lineColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val idealBandColor = Color(0xFF4EE6D4).copy(alpha = 0.25f)
    val idealBorderColor = Color(0xFF4EE6D4).copy(alpha = 0.65f)

    val (ballHighlight, ballMid, ballDark) = when {
        currentExtraction < 20.0f -> Triple(
            Color(0xFFFFF6D6),
            Color(0xFFF2B84B),
            Color(0xFF9E6708)
        )
        currentExtraction > 22.0f -> Triple(
            Color(0xFFFFE3E0),
            Color(0xFFE06C65),
            Color(0xFF82201A)
        )
        else -> Triple(
            Color(0xFFE6FCF9),
            Color(0xFF4EE6D4),
            Color(0xFF0F7A6E)
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (brewMethod == BrewMethod.ESPRESSO)
                        "Extraction Curve vs. Brew Time"
                    else
                        "Extraction Curve vs. Drawdown Time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4EE6D4))
                    )
                    Text(
                        text = "20–22% Ideal Zone",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantColor
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp)
            ) {
                val leftPad = 42.dp.toPx()
                val rightPad = 18.dp.toPx()
                val topPad = 18.dp.toPx()
                val bottomPad = 52.dp.toPx()

                val plotWidth = size.width - leftPad - rightPad
                val plotHeight = size.height - topPad - bottomPad

                val minY = 12f
                val maxY = 28f

                fun yToPx(yVal: Float): Float {
                    val norm = ((yVal - minY) / (maxY - minY)).coerceIn(0f, 1f)
                    return topPad + plotHeight * (1f - norm)
                }

                val minX = if (brewMethod == BrewMethod.ESPRESSO) 15f else 2.0f
                val maxX = if (brewMethod == BrewMethod.ESPRESSO) 50f else 5.0f

                fun xToPx(xVal: Float): Float {
                    val norm = ((xVal - minX) / (maxX - minX)).coerceIn(0f, 1f)
                    return leftPad + plotWidth * norm
                }

                val yTicks = listOf(14f, 18f, 20f, 22f, 26f)
                val labelStyle = TextStyle(
                    color = onSurfaceVariantColor,
                    fontSize = 11.sp
                )
                yTicks.forEach { tick ->
                    val yPx = yToPx(tick)
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.16f),
                        start = Offset(leftPad, yPx),
                        end = Offset(leftPad + plotWidth, yPx),
                        strokeWidth = 1.dp.toPx()
                    )
                    val measured = textMeasurer.measure("${tick.toInt()}%", style = labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            leftPad - measured.size.width - 6.dp.toPx(),
                            yPx - measured.size.height / 2f
                        )
                    )
                }

                val y22Px = yToPx(22f)
                val y20Px = yToPx(20f)
                drawRect(
                    color = idealBandColor,
                    topLeft = Offset(leftPad, y22Px),
                    size = Size(plotWidth, y20Px - y22Px)
                )
                drawLine(
                    color = idealBorderColor,
                    start = Offset(leftPad, y22Px),
                    end = Offset(leftPad + plotWidth, y22Px),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                drawLine(
                    color = idealBorderColor,
                    start = Offset(leftPad, y20Px),
                    end = Offset(leftPad + plotWidth, y20Px),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                val xTicks = if (brewMethod == BrewMethod.ESPRESSO) {
                    listOf(15f, 25f, 35f, 45f, 50f)
                } else {
                    listOf(2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f)
                }

                xTicks.forEach { tick ->
                    val xPx = xToPx(tick)
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.14f),
                        start = Offset(xPx, topPad),
                        end = Offset(xPx, topPad + plotHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    val tickLabel = if (brewMethod == BrewMethod.ESPRESSO) {
                        "${tick.toInt()}s"
                    } else {
                        String.format(Locale.US, "%.1fm", tick)
                    }
                    val measured = textMeasurer.measure(tickLabel, style = labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            xPx - measured.size.width / 2f,
                            topPad + plotHeight + 6.dp.toPx()
                        )
                    )
                }

                val xAxisTitleText = if (brewMethod == BrewMethod.ESPRESSO) {
                    "Brew Time (seconds)"
                } else {
                    "Drawdown Time (minutes)"
                }
                val axisTitleStyle = TextStyle(
                    color = onSurfaceVariantColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val measuredTitle = textMeasurer.measure(xAxisTitleText, style = axisTitleStyle)
                drawText(
                    textLayoutResult = measuredTitle,
                    topLeft = Offset(
                        leftPad + (plotWidth - measuredTitle.size.width) / 2f,
                        topPad + plotHeight + 28.dp.toPx()
                    )
                )

                val curvePath = Path()
                val steps = 60
                for (i in 0..steps) {
                    val xVal = minX + (maxX - minX) * (i.toFloat() / steps)
                    val yVal = calculateExtractionYield(
                        method = brewMethod,
                        roast = roastLevel,
                        beanWeightGrams = beanWeight,
                        liquidYieldGrams = liquidYield,
                        brewTimeSec = if (brewMethod == BrewMethod.ESPRESSO) xVal else brewTime,
                        pourOverRatio = pourOverRatio,
                        drawdownTimeMin = if (brewMethod == BrewMethod.POUR_OVER) xVal else drawdownTime,
                        waterTempC = waterTemp,
                        grinderRpm = grinderRpm,
                        beanAgeDays = beanAge
                    )
                    val px = xToPx(xVal)
                    val py = yToPx(yVal)
                    if (i == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
                }

                drawPath(
                    path = curvePath,
                    color = lineColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val selectedXVal = if (brewMethod == BrewMethod.ESPRESSO) brewTime else drawdownTime
                val ballCenter = Offset(xToPx(selectedXVal), yToPx(currentExtraction))
                val ballRadius = 13.dp.toPx()

                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = ballCenter,
                    end = Offset(ballCenter.x, topPad + plotHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                drawCircle(
                    color = Color.Black.copy(alpha = 0.30f),
                    radius = ballRadius * 1.08f,
                    center = ballCenter + Offset(2.dp.toPx(), 4.dp.toPx())
                )

                val sphereGradient = Brush.radialGradient(
                    colors = listOf(ballHighlight, ballMid, ballDark),
                    center = ballCenter - Offset(ballRadius * 0.35f, ballRadius * 0.35f),
                    radius = ballRadius * 1.35f
                )
                drawCircle(
                    brush = sphereGradient,
                    radius = ballRadius,
                    center = ballCenter
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = ballRadius,
                    center = ballCenter,
                    style = Stroke(width = 1.dp.toPx())
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.78f),
                    radius = ballRadius * 0.28f,
                    center = ballCenter - Offset(ballRadius * 0.35f, ballRadius * 0.35f)
                )
            }
        }
    }
}