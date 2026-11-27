package dev.project.app3

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import dev.project.app3.ui.theme.AppTypography
import dev.project.app3.ui.theme.GoogleBodyFontFamily
import dev.project.app3.ui.theme.ToyStoryFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 1. МОДЕЛЬ ДАННЫХ И ПАЛИТРЫ
// ============================================================================

data class WishItem(
    val id: Long = 0,
    val title: String,
    val brandTag: String,
    val category: String,
    val holiday: String,
    val priceUsd: Int,
    val priceLabel: String,
    val description: String,
    val imageUrl: String,
    val brandColor: Color,
    val isFulfilled: Boolean = false
)

data class WishPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color,
    val success: Color
)

val DarkWishPalette = WishPalette(
    background = Color(0xFF0A0B0F),
    surface = Color(0xFF13151D),
    surfaceVariant = Color(0xFF1C1F2B),
    border = Color(0xFF282C3C),
    textPrimary = Color(0xFFF3F4F8),
    textSecondary = Color(0xFF9197A8),
    primaryAccent = Color(0xFFF3F4F8),
    success = Color(0xFF10B981)
)

val LightWishPalette = WishPalette(
    background = Color(0xFFF2F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9ECF2),
    border = Color(0xFFDDE1EB),
    textPrimary = Color(0xFF111318),
    textSecondary = Color(0xFF5D6375),
    primaryAccent = Color(0xFF18181B),
    success = Color(0xFF059669)
)

// ============================================================================
// 2. ЛОКАЛЬНАЯ БАЗА ДАННЫХ SQLITE
// ============================================================================

class WishlistDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "wishlist_storage.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_WISHES = "wishes"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_BRAND = "brand_tag"
        private const val COL_CATEGORY = "category"
        private const val COL_HOLIDAY = "holiday"
        private const val COL_PRICE_USD = "price_usd"
        private const val COL_PRICE_LABEL = "price_label"
        private const val COL_DESC = "description"
        private const val COL_IMAGE_URL = "image_url"
        private const val COL_COLOR = "brand_color"
        private const val COL_FULFILLED = "is_fulfilled"

        private const val TABLE_WALLET = "wallet_state"
        private const val COL_WALLET_ID = "id"
        private const val COL_SAVED_USD = "saved_usd"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createWishesSql = """
            CREATE TABLE $TABLE_WISHES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_BRAND TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_HOLIDAY TEXT NOT NULL,
                $COL_PRICE_USD INTEGER NOT NULL,
                $COL_PRICE_LABEL TEXT NOT NULL,
                $COL_DESC TEXT NOT NULL,
                $COL_IMAGE_URL TEXT NOT NULL,
                $COL_COLOR INTEGER NOT NULL,
                $COL_FULFILLED INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        val createWalletSql = """
            CREATE TABLE $TABLE_WALLET (
                $COL_WALLET_ID INTEGER PRIMARY KEY,
                $COL_SAVED_USD INTEGER NOT NULL
            )
        """.trimIndent()

        db.execSQL(createWishesSql)
        db.execSQL(createWalletSql)

        val walletValues = ContentValues().apply {
            put(COL_WALLET_ID, 1)
            put(COL_SAVED_USD, 1650)
        }
        db.insert(TABLE_WALLET, null, walletValues)

        seedDefaultWishes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WISHES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WALLET")
        onCreate(db)
    }

    private fun seedDefaultWishes(db: SQLiteDatabase) {
        val defaults = listOf(
            WishItem(
                title = "NVIDIA GeForce RTX 5090 Founders Edition",
                brandTag = "NVIDIA • 32GB GDDR7",
                category = "Залізо",
                holiday = "Новий рік 2027",
                priceUsd = 2199,
                priceLabel = "$2,199",
                description = "Флагманська відеокарта на архітектурі Blackwell із 32 ГБ пам'яті GDDR7 та підтримкою DLSS 4. Максимальний FPS у 4K з повним трасуванням променів.",
                imageUrl = "https://images.unsplash.com/photo-1591488320449-011701bb6704?auto=format&fit=crop&w=800&q=80",
                brandColor = Color(0xFF76B900),
                isFulfilled = false
            ),
            WishItem(
                title = "Підписка Claude Max (20x Pro Usage)",
                brandTag = "ANTHROPIC • AI MAX",
                category = "Софт та ШІ",
                holiday = "Новий рік 2027",
                priceUsd = 200,
                priceLabel = "$200 / міс",
                description = "Найпотужніший тарифний план Anthropic із максимальними лімітами запитів, пріоритетним доступом до флагманських моделей та роботою з великими проєктами.",
                imageUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=800&q=80",
                brandColor = Color(0xFFD97757),
                isFulfilled = true
            ),
            WishItem(
                title = "VIP-квиток на WWE WrestleMania (Ringside)",
                brandTag = "WWE • RINGSIDE VIP",
                category = "Івенти",
                holiday = "День народження",
                priceUsd = 1450,
                priceLabel = "$1,450",
                description = "Місце біля самого рингу на головному шоу року WWE. Жива енергетика арени, виходи суперзірок, піротехніка та колекційний мерч у VIP-пакеті.",
                imageUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=800&q=80",
                brandColor = Color(0xFFE11D48),
                isFulfilled = false
            ),
            WishItem(
                title = "ASUS ROG Swift OLED PG32UCDM (32\" 4K)",
                brandTag = "ASUS ROG • 240Hz OLED",
                category = "Залізо",
                holiday = "Новий рік 2027",
                priceUsd = 1299,
                priceLabel = "$1,299",
                description = "32-дюймовий QD-OLED монітор у пару до RTX 5090: роздільна здатність 4K, частота оновлення 240 Гц, відгук 0.03 мс та ідеальний чорний колір.",
                imageUrl = "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80",
                brandColor = Color(0xFF2563EB),
                isFulfilled = false
            ),
            WishItem(
                title = "Крісло Herman Miller × Logitech G Embody",
                brandTag = "HERMAN MILLER • PRO",
                category = "Комфорт",
                holiday = "День народження",
                priceUsd = 1795,
                priceLabel = "$1,795",
                description = "Ергономічне крісло преміум-класу з динамічною підтримкою хребта та охолоджуючою піною для багатогодинного кодингу та ігрових сесій без втоми.",
                imageUrl = "https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?auto=format&fit=crop&w=800&q=80",
                brandColor = Color(0xFF0891B2),
                isFulfilled = false
            )
        )

        defaults.forEach { item ->
            db.insert(TABLE_WISHES, null, itemToContentValues(item))
        }
    }

    private fun itemToContentValues(item: WishItem): ContentValues {
        return ContentValues().apply {
            put(COL_TITLE, item.title)
            put(COL_BRAND, item.brandTag)
            put(COL_CATEGORY, item.category)
            put(COL_HOLIDAY, item.holiday)
            put(COL_PRICE_USD, item.priceUsd)
            put(COL_PRICE_LABEL, item.priceLabel)
            put(COL_DESC, item.description)
            put(COL_IMAGE_URL, item.imageUrl)
            put(COL_COLOR, item.brandColor.toArgb())
            put(COL_FULFILLED, if (item.isFulfilled) 1 else 0)
        }
    }

    fun getAllWishes(): List<WishItem> {
        val list = mutableListOf<WishItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WISHES,
            null,
            null,
            null,
            null,
            null,
            "$COL_ID ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    WishItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        brandTag = it.getString(it.getColumnIndexOrThrow(COL_BRAND)),
                        category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY)),
                        holiday = it.getString(it.getColumnIndexOrThrow(COL_HOLIDAY)),
                        priceUsd = it.getInt(it.getColumnIndexOrThrow(COL_PRICE_USD)),
                        priceLabel = it.getString(it.getColumnIndexOrThrow(COL_PRICE_LABEL)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_DESC)),
                        imageUrl = it.getString(it.getColumnIndexOrThrow(COL_IMAGE_URL)),
                        brandColor = Color(it.getInt(it.getColumnIndexOrThrow(COL_COLOR))),
                        isFulfilled = it.getInt(it.getColumnIndexOrThrow(COL_FULFILLED)) == 1
                    )
                )
            }
        }
        return list
    }

    fun insertWish(item: WishItem): Long {
        return writableDatabase.insert(TABLE_WISHES, null, itemToContentValues(item))
    }

    fun updateFulfilled(id: Long, isFulfilled: Boolean) {
        val values = ContentValues().apply {
            put(COL_FULFILLED, if (isFulfilled) 1 else 0)
        }
        writableDatabase.update(TABLE_WISHES, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteWish(id: Long) {
        writableDatabase.delete(TABLE_WISHES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getSavedUsd(): Int {
        val cursor = readableDatabase.query(
            TABLE_WALLET,
            arrayOf(COL_SAVED_USD),
            "$COL_WALLET_ID = 1",
            null,
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 1650
    }

    fun updateSavedUsd(newAmount: Int) {
        val values = ContentValues().apply {
            put(COL_SAVED_USD, newAmount)
        }
        writableDatabase.update(TABLE_WALLET, values, "$COL_WALLET_ID = 1", null)
    }

    fun resetDatabase() {
        val db = writableDatabase
        db.delete(TABLE_WISHES, null, null)
        updateSavedUsd(1650)
        seedDefaultWishes(db)
    }
}

// ============================================================================
// 3. СИСТЕМНЫЕ УВЕДОМЛЕНИЯ (ПРАКТИКА)
// ============================================================================

private const val WISH_CHANNEL_ID = "wishlist_notifications_channel"
private var notificationCounter = 100

fun createWishNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            WISH_CHANNEL_ID,
            "Сповіщення Списку Бажань",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Нагадування про цілі та свята з вішліста"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission")
fun sendWishNotification(context: Context, title: String, description: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "Дозвольте сповіщення у налаштуваннях", Toast.LENGTH_SHORT).show()
        return
    }

    val notification = NotificationCompat.Builder(context, WISH_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(description)
        .setStyle(NotificationCompat.BigTextStyle().bigText(description))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(notificationCounter++, notification)
}

// ============================================================================
// 4. ПОДСЧЁТ ВХОДОВ И РЕЙТИНГ-ДИАЛОГ (ПРАКТИКА)
// ============================================================================

private const val LAUNCH_PREFS_NAME = "app_launch_prefs"
private const val KEY_LAUNCH_COUNT = "launch_count"
private const val KEY_RATE_COMPLETED = "rate_dialog_completed"

enum class RateDialogStep {
    NONE,
    TELL_US_WHAT_YOU_THINK,
    FEEDBACK_LOW_RATING,
    PLAY_MARKET_HIGH_RATING
}

fun openFavoriteAppInPlayMarket(context: Context, packageName: String = "com.spotify.music") {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

// ============================================================================
// 5. MAIN ACTIVITY И UI КОМПОНЕНТЫ
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Подсчет входов в приложение через SharedPreferences
        val prefs = getSharedPreferences(LAUNCH_PREFS_NAME, Context.MODE_PRIVATE)
        val launchCount = if (savedInstanceState == null) {
            val nextCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
            prefs.edit().putInt(KEY_LAUNCH_COUNT, nextCount).apply()
            nextCount
        } else {
            prefs.getInt(KEY_LAUNCH_COUNT, 1)
        }
        val shouldShowRateOnStart = launchCount >= 3 && !prefs.getBoolean(KEY_RATE_COMPLETED, false)

        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val palette = if (isDarkTheme) DarkWishPalette else LightWishPalette

            MaterialTheme(
                colorScheme = if (isDarkTheme) {
                    darkColorScheme(
                        background = palette.background,
                        surface = palette.surface,
                        primary = palette.primaryAccent
                    )
                } else {
                    lightColorScheme(
                        background = palette.background,
                        surface = palette.surface,
                        primary = palette.primaryAccent
                    )
                },
                typography = AppTypography
            ) {
                DreamWishlistScreen(
                    palette = palette,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    launchCount = launchCount,
                    initialShowRateDialog = shouldShowRateOnStart,
                    onMarkRateCompleted = {
                        prefs.edit().putBoolean(KEY_RATE_COMPLETED, true).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun DreamWishlistScreen(
    palette: WishPalette,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    launchCount: Int = 1,
    initialShowRateDialog: Boolean = false,
    onMarkRateCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val dbHelper = remember { WishlistDbHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sendWishNotification(
                context = context,
                title = "Список Бажань // Сповіщення увімкнено",
                description = "Головна ціль до Нового року 2027 — NVIDIA GeForce RTX 5090 ($2,199)."
            )
        }
    }

    LaunchedEffect(Unit) {
        createWishNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val wishList = remember { mutableStateListOf<WishItem>().apply { addAll(dbHelper.getAllWishes()) } }
    var savedBudgetUsd by remember { mutableIntStateOf(dbHelper.getSavedUsd()) }

    val holidayTabs = listOf("Усі свята", "Новий рік 2027", "День народження", "Виконані")
    var selectedTab by remember { mutableStateOf("Усі свята") }
    var selectedWishForDialog by remember { mutableStateOf<WishItem?>(null) }
    var showAddWishDialog by remember { mutableStateOf(false) }
    var rateDialogStep by remember {
        mutableStateOf(
            if (initialShowRateDialog) RateDialogStep.TELL_US_WHAT_YOU_THINK else RateDialogStep.NONE
        )
    }
    var selectedStarRating by remember { mutableIntStateOf(0) }

    fun reloadFromDb() {
        wishList.clear()
        wishList.addAll(dbHelper.getAllWishes())
        savedBudgetUsd = dbHelper.getSavedUsd()
    }

    fun triggerNotification(title: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            sendWishNotification(context, title, description)
        }
    }

    val filteredWishes = wishList.filter { item ->
        when (selectedTab) {
            "Новий рік 2027" -> item.holiday == "Новий рік 2027"
            "День народження" -> item.holiday == "День народження"
            "Виконані" -> item.isFulfilled
            else -> true
        }
    }

    val totalDreamsCost = wishList.sumOf { it.priceUsd }
    val fulfilledCount = wishList.count { it.isFulfilled }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            WishlistTopBar(
                palette = palette,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onSendTestNotification = {
                    triggerNotification(
                        title = "Нагадування: Новий рік 2027 вже скоро!",
                        description = "У твоєму вішлісті ${wishList.size} цілей. Головна мрія — NVIDIA GeForce RTX 5090 ($2,199). Накопичено: $$savedBudgetUsd."
                    )
                },
                onResetDb = {
                    dbHelper.resetDatabase()
                    reloadFromDb()
                },
                fulfilledCount = fulfilledCount,
                totalCount = wishList.size
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddWishDialog = true },
                containerColor = palette.primaryAccent,
                contentColor = palette.surface,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Додати", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp)
        ) {
            item {
                UpcomingHolidayBanner(
                    palette = palette,
                    newYearCount = wishList.count { it.holiday == "Новий рік 2027" },
                    onSelectHoliday = { selectedTab = "Новий рік 2027" }
                )
            }

            item {
                UsaFlagCard(palette = palette)
            }

            item {
                SavingsHeroCard(
                    palette = palette,
                    savedUsd = savedBudgetUsd,
                    totalCostUsd = totalDreamsCost,
                    onAddSavings = {
                        val updated = savedBudgetUsd + 250
                        dbHelper.updateSavedUsd(updated)
                        savedBudgetUsd = updated
                    }
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(holidayTabs) { tab ->
                        val active = tab == selectedTab
                        val bg by animateColorAsState(
                            if (active) palette.primaryAccent else palette.surface,
                            label = "tabAnim"
                        )
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (active) palette.primaryAccent else palette.border),
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab,
                                color = if (active) palette.surface else palette.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            items(filteredWishes, key = { it.id }) { wish ->
                EditorialWishCard(
                    wish = wish,
                    palette = palette,
                    currentSavings = savedBudgetUsd,
                    onToggleFulfilled = {
                        val newState = !wish.isFulfilled
                        dbHelper.updateFulfilled(wish.id, newState)
                        reloadFromDb()
                        if (newState) {
                            triggerNotification(
                                title = "Мрію здійснено! ✓",
                                description = "Ти виконав ціль: ${wish.title} (${wish.priceLabel})."
                            )
                        }
                    },
                    onOpenDetails = { selectedWishForDialog = wish }
                )
            }
        }
    }

    selectedWishForDialog?.let { wish ->
        AlertDialog(
            onDismissRequest = { selectedWishForDialog = null },
            containerColor = palette.surface,
            shape = RoundedCornerShape(26.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(
                        model = wish.imageUrl,
                        contentDescription = wish.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = wish.brandTag,
                            color = wish.brandColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = wish.holiday,
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = wish.title,
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = wish.description,
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    HorizontalDivider(color = palette.border)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Вартість:", color = palette.textSecondary, fontSize = 12.sp)
                        Text(
                            text = wish.priceLabel,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newState = !wish.isFulfilled
                        dbHelper.updateFulfilled(wish.id, newState)
                        reloadFromDb()
                        if (newState) {
                            triggerNotification(
                                title = "Мрію здійснено! ✓",
                                description = "Ти виконав ціль: ${wish.title} (${wish.priceLabel})."
                            )
                        }
                        selectedWishForDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.success),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (wish.isFulfilled) "Повернути в список" else "Виконано ✓",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dbHelper.deleteWish(wish.id)
                        reloadFromDb()
                        selectedWishForDialog = null
                    }
                ) {
                    Text("Видалити з БД", color = Color(0xFFE11D48))
                }
            }
        )
    }

    if (showAddWishDialog) {
        NewWishDialog(
            palette = palette,
            onCreate = { newItem ->
                dbHelper.insertWish(newItem)
                reloadFromDb()
                triggerNotification(
                    title = "Нове бажання збережено в БД!",
                    description = "${newItem.title} (${newItem.priceLabel}) — свято: ${newItem.holiday}."
                )
                showAddWishDialog = false
            },
            onDismiss = { showAddWishDialog = false }
        )
    }

    when (rateDialogStep) {
        RateDialogStep.TELL_US_WHAT_YOU_THINK -> {
            TellUsWhatYouThinkDialog(
                palette = palette,
                launchCount = launchCount,
                rating = selectedStarRating,
                onRatingChange = { selectedStarRating = it },
                onSubmit = {
                    if (selectedStarRating in 1..3) {
                        rateDialogStep = RateDialogStep.FEEDBACK_LOW_RATING
                    } else if (selectedStarRating >= 4) {
                        rateDialogStep = RateDialogStep.PLAY_MARKET_HIGH_RATING
                    }
                },
                onDismiss = { rateDialogStep = RateDialogStep.NONE }
            )
        }

        RateDialogStep.FEEDBACK_LOW_RATING -> {
            LowRatingFeedbackDialog(
                palette = palette,
                rating = selectedStarRating,
                onSubmitFeedback = { feedbackText ->
                    onMarkRateCompleted()
                    rateDialogStep = RateDialogStep.NONE
                    Toast.makeText(
                        context,
                        if (feedbackText.isNotBlank()) "Дякуємо за відгук! Ми врахуємо ваші зауваження."
                        else "Дякуємо за оцінку!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = {
                    onMarkRateCompleted()
                    rateDialogStep = RateDialogStep.NONE
                }
            )
        }

        RateDialogStep.PLAY_MARKET_HIGH_RATING -> {
            HighRatingPlayMarketDialog(
                palette = palette,
                rating = selectedStarRating,
                onOpenPlayMarket = {
                    onMarkRateCompleted()
                    rateDialogStep = RateDialogStep.NONE
                    openFavoriteAppInPlayMarket(context, "com.spotify.music")
                },
                onDismiss = {
                    onMarkRateCompleted()
                    rateDialogStep = RateDialogStep.NONE
                }
            )
        }

        RateDialogStep.NONE -> Unit
    }
}

@Composable
fun WishlistTopBar(
    palette: WishPalette,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSendTestNotification: () -> Unit,
    onResetDb: () -> Unit,
    fulfilledCount: Int,
    totalCount: Int
) {
    Surface(
        color = palette.background.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Список Бажань",
                    color = palette.textPrimary,
                    fontFamily = ToyStoryFontFamily,
                    fontSize = 23.sp,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "SQLite • Виконано $fulfilledCount з $totalCount",
                    color = palette.textSecondary,
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Кнопка вызова системного уведомления
                Surface(
                    color = palette.surface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.clickable { onSendTestNotification() }
                ) {
                    Text(
                        text = "🔔",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }

                // Кнопка сброса БД
                Surface(
                    color = palette.surface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.clickable { onResetDb() }
                ) {
                    Text(
                        text = "↺",
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Переключатель темы
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.clickable { onToggleTheme() }
                ) {
                    Text(
                        text = if (isDarkTheme) "☀ Світла" else "☾ Темна",
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingHolidayBanner(
    palette: WishPalette,
    newYearCount: Int,
    onSelectHoliday: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectHoliday() }
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1D243B), palette.surface)
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "НАЙБЛИЖЧЕ СВЯТО",
                    color = Color(0xFF38BDF8),
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Новий рік 2027",
                    color = Color.White,
                    fontFamily = ToyStoryFontFamily,
                    fontSize = 19.sp
                )
                Text(
                    text = "Заплановано бажань: $newYearCount • Залишилось 41 дн.",
                    color = Color(0xFFCBD5E1),
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "41 ДН.",
                    color = Color(0xFF38BDF8),
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SavingsHeroCard(
    palette: WishPalette,
    savedUsd: Int,
    totalCostUsd: Int,
    onAddSavings: () -> Unit
) {
    val ratio = (savedUsd.toFloat() / totalCostUsd.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(500),
        label = "walletProgress"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "МОЯ СКАРБНИЧКА (SQLITE)",
                        color = palette.textSecondary,
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$%,d".format(savedUsd),
                            color = palette.textPrimary,
                            fontFamily = ToyStoryFontFamily,
                            fontSize = 26.sp
                        )
                        Text(
                            text = " / $%,d".format(totalCostUsd),
                            color = palette.textSecondary,
                            fontFamily = GoogleBodyFontFamily,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = onAddSavings,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "+ $250",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    color = palette.success,
                    trackColor = palette.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Покрито ${(animatedRatio * 100).toInt()}% від усіх бажань",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Зберігається локально",
                        color = palette.success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EditorialWishCard(
    wish: WishItem,
    palette: WishPalette,
    currentSavings: Int,
    onToggleFulfilled: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val canAfford = currentSavings >= wish.priceUsd && !wish.isFulfilled
    val itemProgress = (currentSavings.toFloat() / wish.priceUsd.coerceAtLeast(1)).coerceIn(0f, 1f)

    val borderColor by animateColorAsState(
        targetValue = when {
            wish.isFulfilled -> palette.success.copy(alpha = 0.6f)
            canAfford -> wish.brandColor.copy(alpha = 0.75f)
            else -> palette.border
        },
        label = "cardBorder"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(if (canAfford || wish.isFulfilled) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(palette.surfaceVariant)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                wish.brandColor.copy(alpha = 0.25f),
                                palette.surfaceVariant
                            )
                        )
                    )
                    drawCircle(
                        color = wish.brandColor.copy(alpha = 0.18f),
                        radius = size.height * 0.65f,
                        center = Offset(size.width * 0.85f, size.height * 0.25f),
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                AsyncImage(
                    model = wish.imageUrl,
                    contentDescription = wish.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                Surface(
                    color = wish.brandColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = wish.brandTag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    Text(
                        text = wish.holiday,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = wish.priceLabel,
                        color = Color.White,
                        fontFamily = ToyStoryFontFamily,
                        fontSize = 22.sp
                    )

                    if (canAfford) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Бюджету вистачає ✓",
                                color = Color.White,
                                fontFamily = GoogleBodyFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = wish.title,
                    color = if (wish.isFulfilled) palette.textSecondary else palette.textPrimary,
                    fontFamily = ToyStoryFontFamily,
                    fontSize = 18.sp,
                    textDecoration = if (wish.isFulfilled) TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = wish.description,
                    color = palette.textSecondary,
                    fontFamily = GoogleBodyFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (!wish.isFulfilled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.6f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (canAfford) "Готово до покупки" else "Залишилось зібрати: $${wish.priceUsd - currentSavings}",
                                color = if (canAfford) palette.success else palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(itemProgress * 100).toInt()}%",
                                color = palette.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { itemProgress },
                            color = wish.brandColor,
                            trackColor = palette.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (wish.isFulfilled) "Мрія здійснена!" else "Категорія: ${wish.category}",
                        color = if (wish.isFulfilled) palette.success else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onToggleFulfilled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wish.isFulfilled) palette.success.copy(alpha = 0.16f) else palette.primaryAccent,
                            contentColor = if (wish.isFulfilled) palette.success else palette.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (wish.isFulfilled) "Куплено ✓" else "Відмітити",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewWishDialog(
    palette: WishPalette,
    onCreate: (WishItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedHoliday by remember { mutableStateOf("Новий рік 2027") }
    var imageUrl by remember {
        mutableStateOf("https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80")
    }

    val imagePresets = listOf(
        "ПК / Залізо" to "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=800&q=80",
        "Гаджет" to "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
        "Івент / Шоу" to "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=800&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Нове бажання (SQLite)",
                color = palette.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Назва (напр. PlayStation 5 Pro)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("Вартість у $ (напр. 799)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Свято:", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Новий рік 2027", "День народження")) { holiday ->
                        val active = holiday == selectedHoliday
                        Surface(
                            color = if (active) palette.primaryAccent else palette.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { selectedHoliday = holiday }
                        ) {
                            Text(
                                text = holiday,
                                color = if (active) palette.surface else palette.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Опис бажання...", fontSize = 12.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Швидкий вибір обкладинки або свій URL:", color = palette.textSecondary, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(imagePresets) { (label, url) ->
                        Surface(
                            color = if (imageUrl == url) Color(0xFF2563EB) else palette.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { imageUrl = url }
                        ) {
                            Text(
                                text = label,
                                color = if (imageUrl == url) Color.White else palette.textPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    placeholder = { Text("URL зображення...", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedPrice = price.toIntOrNull() ?: 500
                        onCreate(
                            WishItem(
                                title = title.trim(),
                                brandTag = "CUSTOM • WISH",
                                category = "Залізо",
                                holiday = selectedHoliday,
                                priceUsd = parsedPrice,
                                priceLabel = "$$parsedPrice",
                                description = description.ifBlank { "Персональне побажання до свята ($selectedHoliday)." },
                                imageUrl = imageUrl.ifBlank { imagePresets.first().second },
                                brandColor = Color(0xFF7C3AED),
                                isFulfilled = false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Зберегти в БД", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = palette.textSecondary)
            }
        }
    )
}

// ============================================================================
// 6. КАСТОМНЫЙ РЕЙТИНГ-БАР И ДИАЛОГИ ОЦЕНКИ ("TELL US WHAT YOU THINK")
// ============================================================================

@Composable
fun CustomRatingBar(
    rating: Int,
    palette: WishPalette,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeGold = Color(0xFFF59E0B)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val isSelected = star <= rating
            val starScale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 1f,
                animationSpec = tween(200),
                label = "starScale$star"
            )
            val containerBg by animateColorAsState(
                targetValue = if (isSelected) activeGold.copy(alpha = 0.16f) else palette.surfaceVariant,
                animationSpec = tween(200),
                label = "starBg$star"
            )
            val borderCol by animateColorAsState(
                targetValue = if (isSelected) activeGold else palette.border,
                animationSpec = tween(200),
                label = "starBorder$star"
            )
            val starColor by animateColorAsState(
                targetValue = if (isSelected) activeGold else palette.textSecondary.copy(alpha = 0.45f),
                animationSpec = tween(200),
                label = "starFill$star"
            )

            Surface(
                color = containerBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, borderCol),
                modifier = Modifier
                    .size(46.dp)
                    .scale(starScale)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onRatingSelected(star) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerRadius = size.minDimension * 0.48f
                        val innerRadius = outerRadius * 0.44f
                        val path = buildStarPath(cx, cy, outerRadius, innerRadius)
                        drawPath(path = path, color = starColor)
                    }
                }
            }
        }
    }
}

@Composable
fun TellUsWhatYouThinkDialog(
    palette: WishPalette,
    launchCount: Int,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val ratingCaption = when (rating) {
        1 -> "Дуже погано • 1/5"
        2 -> "Можна краще • 2/5"
        3 -> "Нормально • 3/5"
        4 -> "Дуже добре • 4/5"
        5 -> "Чудовий додаток! • 5/5"
        else -> "Оберіть кількість зірок (1–5)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "ВХІД #$launchCount • ВАША ДУМКА",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Tell us what you think?",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Ви вже втретє користуєтеся «Список Бажань». Оцініть, будь ласка, наскільки вам подобається додаток!",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                CustomRatingBar(
                    rating = rating,
                    palette = palette,
                    onRatingSelected = onRatingChange
                )

                Text(
                    text = ratingCaption,
                    color = if (rating > 0) Color(0xFFF59E0B) else palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = rating > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Продовжити", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Пізніше", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun LowRatingFeedbackDialog(
    palette: WishPalette,
    rating: Int,
    onSubmitFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    val quickTags = listOf("Дизайн", "Незручно", "Мало функцій", "Помилка")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = Color(0xFFE11D48).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "ОЦІНКА $rating / 5 • ФІДБЕК",
                        color = Color(0xFFE11D48),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Шкода, що щось не так",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Чи не хотіли б ви написати, що саме не так? Ваші зауваження допоможуть нам покращити додаток.",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickTags) { tag ->
                        Surface(
                            color = palette.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.clickable {
                                feedbackText = if (feedbackText.isBlank()) tag else "$feedbackText, $tag"
                            }
                        ) {
                            Text(
                                text = "+ $tag",
                                color = palette.textPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Напишіть, що саме не так...", fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitFeedback(feedbackText.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE11D48),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Надіслати фідбек", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun HighRatingPlayMarketDialog(
    palette: WishPalette,
    rating: Int,
    onOpenPlayMarket: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = palette.success.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "ОЦІНКА $rating / 5 ⭐",
                        color = palette.success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Дякуємо за високу оцінку!",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ми раді, що вам подобається «Список Бажань»! Запрошуємо перейти в Google Play Market на сторінку нашого улюбленого додатку:",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = palette.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1DB954),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "▶",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spotify: Music & Podcasts",
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Google Play Market • Улюблений додаток",
                                color = palette.success,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenPlayMarket,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.success,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Перейти в Play Market", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Не зараз", color = palette.textSecondary)
            }
        }
    )
}

// ============================================================================
// 7. ГРАФІЧНІ ПРИМІТИВИ: ПРАПОР США (БЕЗ DRAWBITMAP І ГОТОВИХ ЗОБРАЖЕНЬ)
// ============================================================================

private fun buildStarPath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    for (i in 0 until 10) {
        val angleRad = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = cx + (r * cos(angleRad)).toFloat()
        val y = cy + (r * sin(angleRad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun UsaFlagCard(palette: WishPalette) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ПРАКТИКА №3 • CANVAS ПРИМІТИВИ",
                        color = Color(0xFF38BDF8),
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Прапор США",
                        color = palette.textPrimary,
                        fontFamily = ToyStoryFontFamily,
                        fontSize = 20.sp
                    )
                }

                Surface(
                    color = palette.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        text = "13 смуг • 50 зірок",
                        color = palette.textSecondary,
                        fontFamily = GoogleBodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            UsaFlagCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.9f)
                    .clip(RoundedCornerShape(14.dp))
            )

            Text(
                text = "Відмальовано виключно графічними примітивами Compose Canvas (drawRect та drawPath) без використання зображень і drawBitmap.",
                color = palette.textSecondary,
                fontFamily = GoogleBodyFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun UsaFlagCanvas(modifier: Modifier = Modifier) {
    val usaRed = Color(0xFFB22234)
    val usaWhite = Color(0xFFFFFFFF)
    val usaBlue = Color(0xFF3C3B6E)

    Canvas(modifier = modifier) {
        val stripeHeight = size.height / 13f

        // 13 горизонтальних смуг (7 червоних, 6 білих) через drawRect
        for (i in 0 until 13) {
            drawRect(
                color = if (i % 2 == 0) usaRed else usaWhite,
                topLeft = Offset(0f, i * stripeHeight),
                size = Size(size.width, stripeHeight + 0.5f)
            )
        }

        // Синій кантон (прямокутник у верхньому лівому куті шириною 0.40 та висотою 7 смуг)
        val cantonWidth = size.width * 0.40f
        val cantonHeight = stripeHeight * 7f
        drawRect(
            color = usaBlue,
            topLeft = Offset(0f, 0f),
            size = Size(cantonWidth, cantonHeight)
        )

        // 50 білих п'ятикутних зірок (9 рядів: чергування 6 і 5 зірок) через drawPath
        val rowStep = cantonHeight / 10f
        val colStep = cantonWidth / 12f
        val outerRadius = stripeHeight * 0.40f
        val innerRadius = outerRadius * 0.382f

        for (row in 0 until 9) {
            val cy = (row + 1) * rowStep
            val isSixStarRow = row % 2 == 0
            val count = if (isSixStarRow) 6 else 5
            val startCol = if (isSixStarRow) 1 else 2

            for (col in 0 until count) {
                val cx = (startCol + col * 2) * colStep
                drawPath(
                    path = buildStarPath(cx, cy, outerRadius, innerRadius),
                    color = usaWhite
                )
            }
        }
    }
}
