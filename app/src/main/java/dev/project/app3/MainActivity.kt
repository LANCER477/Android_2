package dev.project.app3

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
// 2. ЛОКАЛЬНАЯ БАЗА ДАННЫХ SQLITE (SQLiteOpenHelper)
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

        // Начальный баланс копилки
        val walletValues = ContentValues().apply {
            put(COL_WALLET_ID, 1)
            put(COL_SAVED_USD, 1650)
        }
        db.insert(TABLE_WALLET, null, walletValues)

        // Заполнение стартовых желаний
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
// 3. MAIN ACTIVITY И UI КОМПОНЕНТЫ
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                }
            ) {
                DreamWishlistScreen(
                    palette = palette,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun DreamWishlistScreen(
    palette: WishPalette,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { WishlistDbHelper(context) }

    // Состояние загружается напрямую из SQLite
    val wishList = remember { mutableStateListOf<WishItem>().apply { addAll(dbHelper.getAllWishes()) } }
    var savedBudgetUsd by remember { mutableIntStateOf(dbHelper.getSavedUsd()) }

    val holidayTabs = listOf("Усі свята", "Новий рік 2027", "День народження", "Виконані")
    var selectedTab by remember { mutableStateOf("Усі свята") }
    var selectedWishForDialog by remember { mutableStateOf<WishItem?>(null) }
    var showAddWishDialog by remember { mutableStateOf(false) }

    fun reloadFromDb() {
        wishList.clear()
        wishList.addAll(dbHelper.getAllWishes())
        savedBudgetUsd = dbHelper.getSavedUsd()
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
            // 1. Баннер ближайшего праздника
            item {
                UpcomingHolidayBanner(
                    palette = palette,
                    newYearCount = wishList.count { it.holiday == "Новий рік 2027" },
                    onSelectHoliday = { selectedTab = "Новий рік 2027" }
                )
            }

            // 2. Карточка копилки (сохраняется в SQLite)
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

            // 3. Фильтры по праздникам
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

            // 4. Кастомные карточки желаний (Изображение + Название + Описание + Стоимость)
            items(filteredWishes, key = { it.id }) { wish ->
                EditorialWishCard(
                    wish = wish,
                    palette = palette,
                    currentSavings = savedBudgetUsd,
                    onToggleFulfilled = {
                        val newState = !wish.isFulfilled
                        dbHelper.updateFulfilled(wish.id, newState)
                        reloadFromDb()
                    },
                    onOpenDetails = { selectedWishForDialog = wish }
                )
            }
        }
    }

    // Модальное окно просмотра и удаления записи из SQLite
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
                        dbHelper.updateFulfilled(wish.id, !wish.isFulfilled)
                        reloadFromDb()
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

    // Диалог добавления нового желания в SQLite
    if (showAddWishDialog) {
        NewWishDialog(
            palette = palette,
            onCreate = { newItem ->
                dbHelper.insertWish(newItem)
                reloadFromDb()
                showAddWishDialog = false
            },
            onDismiss = { showAddWishDialog = false }
        )
    }
}

@Composable
fun WishlistTopBar(
    palette: WishPalette,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
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
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "SQLite DB • Виконано $fulfilledCount з $totalCount",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка сброса демо-данных в SQLite
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Новий рік 2027",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Заплановано бажань: $newYearCount • Залишилось 41 дн.",
                    color = Color(0xFFCBD5E1),
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$%,d".format(savedUsd),
                            color = palette.textPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = " / $%,d".format(totalCostUsd),
                            color = palette.textSecondary,
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
            // 1. Изображение + Canvas-подложка
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

                // Тег бренда
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

                // Метка праздника (справа сверху)
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

                // Стоимость внизу слева
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
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (canAfford) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Бюджету вистачає ✓",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. Название, описание, прогресс и кнопка
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = wish.title,
                    color = if (wish.isFulfilled) palette.textSecondary else palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (wish.isFulfilled) TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = wish.description,
                    color = palette.textSecondary,
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
    var selectedCategory by remember { mutableStateOf("Залізо") }
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
                                category = selectedCategory,
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