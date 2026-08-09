@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kakeibo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kakeibo.KakeiboRepository
import com.example.kakeibo.model.CategoryModel
import com.example.kakeibo.model.CategoryBudgetSummary
import com.example.kakeibo.model.datePickerMillisToLocalDate
import com.example.kakeibo.model.localDateToDatePickerMillis
import com.example.kakeibo.model.parseAmountText
import com.example.kakeibo.model.validateAmountText
import com.example.kakeibo.model.MonthlySummary
import com.example.kakeibo.model.TransactionModel
import com.example.kakeibo.model.TransactionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val HOME = "home"
private const val HISTORY = "history"
private const val EDITOR = "editor?transactionId={transactionId}"
private const val BUDGET = "budget"
private const val CATEGORIES = "categories"

@Composable
fun KakeiboApp(repository: KakeiboRepository) {
    val navController = rememberNavController()
    val current by navController.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route
    val bottomRoute = when {
        currentRoute?.startsWith(HISTORY) == true -> HISTORY
        currentRoute?.startsWith(CATEGORIES) == true -> CATEGORIES
        else -> HOME
    }

    val showBottomBar = currentRoute == HOME || currentRoute == HISTORY || currentRoute == CATEGORIES
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(selected = bottomRoute == HOME, onClick = { navController.navigate(HOME) }, icon = { Icon(Icons.Filled.Home, contentDescription = "ホーム") }, label = { Text("ホーム") })
                    NavigationBarItem(selected = bottomRoute == HISTORY, onClick = { navController.navigate(HISTORY) }, icon = { Icon(Icons.Filled.History, contentDescription = "履歴") }, label = { Text("履歴") })
                    NavigationBarItem(selected = bottomRoute == CATEGORIES, onClick = { navController.navigate(CATEGORIES) }, icon = { Icon(Icons.Filled.Category, contentDescription = "カテゴリ") }, label = { Text("カテゴリ") })
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = HOME, modifier = Modifier.padding(padding)) {
            composable(HOME) {
                HomeScreen(repository, onAdd = { navController.navigate("editor?transactionId=-1") }, onBudget = { navController.navigate(BUDGET) }, onOpen = { navController.navigate("editor?transactionId=$it") })
            }
            composable(HISTORY) {
                HistoryScreen(repository, onOpen = { navController.navigate("editor?transactionId=$it") }, onAdd = { navController.navigate("editor?transactionId=-1") })
            }
            composable(
                route = EDITOR,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                EditorScreen(
                    repository,
                    entry.arguments?.getLong("transactionId")?.takeIf { it >= 0 },
                    onBack = { navController.navigateUp() },
                    onCategoryManagement = { navController.navigate(CATEGORIES) }
                )
            }
            composable(BUDGET) { BudgetScreen(repository, onBack = { navController.navigateUp() }) }
            composable(CATEGORIES) { CategoryScreen(repository) }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun HomeScreen(repository: KakeiboRepository, onAdd: () -> Unit, onBudget: () -> Unit, onOpen: (Long) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = YearMonth.parse(month)
    val summary by remember(selectedMonth) { repository.observeSummary(selectedMonth) }.collectAsState(initial = MonthlySummary(selectedMonth))
    val transactions by remember(selectedMonth) { repository.observeTransactions(selectedMonth) }.collectAsState(initial = emptyList())
    val categoryBudgets by remember(selectedMonth) { repository.observeCategoryBudgetSummaries(selectedMonth) }.collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, floatingActionButton = { Button(onClick = onAdd, shape = RoundedCornerShape(50)) { Text("＋ 記録") } }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MonthSelector(selectedMonth, { month = selectedMonth.minusMonths(1).toString() }, { month = selectedMonth.plusMonths(1).toString() }) }
            item {
                BudgetCard(summary, onBudget)
            }
            if (categoryBudgets.any { it.budget != null || it.spent > 0 }) {
                item { CategoryBudgetOverview(selectedMonth, categoryBudgets) }
            }
            item { SummaryCard(summary) }
            item {
                Text("最近の記録", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (transactions.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("この月の記録はありません")
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onAdd) { Text("最初の記録を追加") }
                        }
                    }
                }
            } else {
                items(transactions.take(5), key = { it.id }) { transaction ->
                    TransactionRow(transaction, onClick = { onOpen(transaction.id) })
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
private fun BudgetProgressBar(month: YearMonth, ratio: Float, overBudget: Boolean, modifier: Modifier = Modifier) {
    val darkTheme = isSystemInDarkTheme()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (darkTheme) 0.28f else 0.20f)
    val progressColor = when {
        overBudget -> MaterialTheme.colorScheme.error
        darkTheme -> Color(0xFF54D6E8)
        else -> Color(0xFF007C91)
    }
    val markerColor = MaterialTheme.colorScheme.onSurface
    val markerOutline = MaterialTheme.colorScheme.surface
    val todayRatio = if (month == YearMonth.now()) {
        LocalDate.now().dayOfMonth.toFloat() / month.lengthOfMonth()
    } else {
        null
    }
    val accessibilityText = if (overBudget) {
        "予算超過"
    } else {
        "予算使用率 ${((ratio.coerceAtLeast(0f)) * 100).toInt()}パーセント"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics { contentDescription = accessibilityText }
    ) {
        val radius = size.height / 2f
        drawRoundRect(trackColor, cornerRadius = CornerRadius(radius, radius))
        val progressWidth = size.width * ratio.coerceIn(0f, 1f)
        if (progressWidth > 0f) {
            drawRoundRect(
                color = progressColor,
                size = Size(progressWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        todayRatio?.let {
            val x = (size.width * it).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
            drawLine(markerOutline, Offset(x, -2.dp.toPx()), Offset(x, size.height + 2.dp.toPx()), 5.dp.toPx())
            drawLine(markerColor, Offset(x, -2.dp.toPx()), Offset(x, size.height + 2.dp.toPx()), 2.dp.toPx())
        }
    }
}

@Composable
private fun BudgetCard(summary: MonthlySummary, onBudget: () -> Unit) {
    val remaining = summary.remainingBudget
    val overBudget = remaining != null && remaining < 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (overBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (overBudget) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val title = if (summary.month == YearMonth.now()) "今月の予算" else "${summary.month.year}年${summary.month.monthValue}月の予算"
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBudget, modifier = Modifier.testTag("budget_action")) { Text(if (summary.budget == null) "設定" else "変更") }
            }
            if (summary.budget == null) {
                Text("予算未設定", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${yen(summary.expense)} / ${yen(summary.budget)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                BudgetProgressBar(summary.month, summary.usageRatio ?: 0f, overBudget, Modifier.testTag("budget_progress"))
                Spacer(Modifier.height(6.dp))
                val remainingAmount = remaining ?: 0
                Text(
                    if (remainingAmount >= 0) "残り ${yen(remainingAmount)}" else "${yen(-remainingAmount)} 超過",
                    color = if (remainingAmount >= 0) Color.Unspecified else MaterialTheme.colorScheme.error,
                    fontWeight = if (remainingAmount < 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetOverview(month: YearMonth, summaries: List<CategoryBudgetSummary>) {
    val visible = summaries.filter { it.budget != null || it.spent > 0 }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("カテゴリ別予算", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            visible.forEach { item ->
                val ratio = item.usageRatio
                val remaining = item.remaining
                val over = remaining?.takeIf { it < 0 }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(item.category.colorArgb).copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) { CategoryIcon(item.category, Modifier.padding(7.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(item.category.name, fontWeight = FontWeight.Medium)
                            Text(
                                if (item.budget == null) "${yen(item.spent)} / 予算なし"
                                else "${yen(item.spent)} / ${yen(item.budget)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (item.budget != null) {
                            Spacer(Modifier.height(4.dp))
                            BudgetProgressBar(month, ratio ?: 0f, overBudget = over != null)
                            Text(
                                when {
                                    over != null -> "${yen(-over)} 超過"
                                    remaining != null -> "残り ${yen(remaining)}"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (over != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: MonthlySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryItem("収入", summary.income, MaterialTheme.colorScheme.primary)
            SummaryItem("支出", summary.expense, MaterialTheme.colorScheme.error)
            SummaryItem("収支", summary.balance, if (summary.balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(yen(amount), color = color, fontWeight = FontWeight.Bold)
    }
}

private fun iconForCategory(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Filled.Restaurant
    "shopping" -> Icons.Filled.ShoppingCart
    "home" -> Icons.Filled.Home
    "bolt" -> Icons.Filled.Bolt
    "phone" -> Icons.Filled.Phone
    "train" -> Icons.Filled.Train
    "medical" -> Icons.Filled.MedicalServices
    "movie" -> Icons.Filled.Movie
    "work" -> Icons.Filled.Work
    "celebration" -> Icons.Filled.Celebration
    "laptop" -> Icons.Filled.Laptop
    else -> Icons.Filled.MoreHoriz
}

@Composable
private fun CategoryIcon(category: CategoryModel, modifier: Modifier = Modifier) {
    Icon(
        imageVector = iconForCategory(category.iconKey),
        contentDescription = category.name,
        tint = Color(category.colorArgb),
        modifier = modifier
    )
}

private val categoryIconOptions = listOf(
    "restaurant", "shopping", "home", "bolt", "phone", "train",
    "medical", "movie", "work", "celebration", "laptop", "more"
)

private fun defaultColorForIcon(iconKey: String): Int = when (iconKey) {
    "restaurant" -> 0xFFE57373.toInt()
    "shopping" -> 0xFF64B5F6.toInt()
    "home" -> 0xFF9575CD.toInt()
    "bolt" -> 0xFFFFB74D.toInt()
    "phone" -> 0xFF4DB6AC.toInt()
    "train" -> 0xFF81C784.toInt()
    "medical" -> 0xFFF06292.toInt()
    "movie" -> 0xFF7986CB.toInt()
    "work" -> 0xFF66BB6A.toInt()
    "celebration" -> 0xFF26A69A.toInt()
    "laptop" -> 0xFF42A5F5.toInt()
    else -> 0xFF90A4AE.toInt()
}

private fun iconLabel(iconKey: String): String = when (iconKey) {
    "restaurant" -> "食事"
    "shopping" -> "買い物"
    "home" -> "住居"
    "bolt" -> "光熱"
    "phone" -> "通信"
    "train" -> "交通"
    "medical" -> "医療"
    "movie" -> "娯楽"
    "work" -> "仕事"
    "celebration" -> "イベント"
    "laptop" -> "副業"
    else -> "その他"
}

@Composable
private fun IconPicker(selectedKey: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text("アイコン", style = MaterialTheme.typography.labelLarge)
        categoryIconOptions.chunked(6).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEach { key ->
                    val selected = selectedKey == key
                    Surface(
                        onClick = { onSelect(key) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).size(42.dp)
                    ) {
                        Icon(iconForCategory(key), contentDescription = iconLabel(key), tint = Color(defaultColorForIcon(key)), modifier = Modifier.padding(9.dp))
                    }
                }
                repeat(6 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryModel>,
    selectedId: Long?,
    onSelect: (CategoryModel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        categories.chunked(3).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowCategories.forEach { category ->
                    val selected = category.id == selectedId
                    Surface(
                        onClick = { onSelect(category) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (selected) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f).heightIn(min = 72.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            CategoryIcon(category, Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(category.name, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                        }
                    }
                }
                repeat(3 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun formattedAmountInput(value: String): String {
    val digits = value.filter(Char::isDigit)
    return digits.toLongOrNull()?.let { NumberFormat.getNumberInstance(Locale.JAPAN).format(it) }.orEmpty()
}

private fun updateAmountWithKey(value: String, key: String): String {
    val digits = value.filter(Char::isDigit)
    return when (key) {
        "⌫" -> digits.dropLast(1)
        else -> (digits + key).take(MAX_AMOUNT_DIGITS)
    }
}

private const val MAX_AMOUNT_DIGITS = 10

@Composable
private fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    label: String,
    fieldTestTag: String = "amount_input",
    keypadTestTagPrefix: String = "amount_key"
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = formattedAmountInput(value),
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)) },
            label = { Text(label) },
            readOnly = true,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(fieldTestTag).semantics {
                this[SemanticsActions.SetText] = AccessibilityAction(null) { text: AnnotatedString ->
                    onValueChange(text.text)
                    true
                }
                this[SemanticsActions.InsertTextAtCursor] = AccessibilityAction(null) { text: AnnotatedString ->
                    onValueChange((value + text.text).take(MAX_AMOUNT_DIGITS))
                    true
                }
            }
        )
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("00", "0", "⌫")
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            keys.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    rowKeys.forEach { key ->
                        OutlinedButton(
                            onClick = { onValueChange(updateAmountWithKey(value, key)) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("${keypadTestTagPrefix}_$key"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            if (key == "⌫") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "削除")
                            } else {
                                Text(key, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionModel, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(transaction.colorArgb).copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = iconForCategory(transaction.iconKey),
                    contentDescription = transaction.categoryName,
                    tint = Color(transaction.colorArgb),
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.categoryName, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(transaction.date.toString(), transaction.memo).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Text(yen(transaction.amount), color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryScreen(repository: KakeiboRepository, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var filter by rememberSaveable { mutableStateOf("すべて") }
    val selectedMonth = YearMonth.parse(month)
    val all by remember(selectedMonth) { repository.observeTransactions(selectedMonth) }.collectAsState(initial = emptyList())
    val transactions = all.filter { filter == "すべて" || (filter == "収入" && it.type == TransactionType.INCOME) || (filter == "支出" && it.type == TransactionType.EXPENSE) }
    Scaffold(topBar = { TopAppBar(title = { Text("履歴") }) }, floatingActionButton = { Button(onClick = onAdd, shape = RoundedCornerShape(50)) { Text("＋ 記録") } }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { MonthSelector(selectedMonth, { month = selectedMonth.minusMonths(1).toString() }, { month = selectedMonth.plusMonths(1).toString() }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("すべて", "支出", "収入").forEach { value ->
                        item { FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) }) }
                    }
                }
            }
            if (transactions.isEmpty()) item { Text("該当する記録はありません", modifier = Modifier.padding(16.dp)) }
            else items(transactions, key = { it.id }) { TransactionRow(it, { onOpen(it.id) }) }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    repository: KakeiboRepository,
    transactionId: Long?,
    onBack: () -> Unit,
    onCategoryManagement: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var memo by rememberSaveable { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val saveGuard = remember { AtomicBoolean(false) }
    var loaded by rememberSaveable { mutableStateOf(transactionId == null) }
    val currentType = TransactionType.valueOf(type)
    val visibleCategories = categories.filter { it.type == currentType }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            repository.findTransaction(transactionId)?.let {
                type = it.type.name; amount = it.amount.toString(); categoryId = it.categoryId; dateText = it.date.toString(); memo = it.memo.orEmpty()
            }
        }
        loaded = true
    }

    fun saveTransaction() {
        val parsed = parseAmountText(amount)
        amountError = validateAmountText(amount)
        categoryError = if (categoryId == null) "カテゴリを選択してください" else null
        if (amountError == null && categoryError == null && saveGuard.compareAndSet(false, true)) {
            isSaving = true
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    repository.saveTransaction(transactionId, currentType, parsed!!, categoryId!!, LocalDate.parse(dateText), memo)
                    onBack()
                } catch (error: Throwable) {
                    saveGuard.set(false)
                    isSaving = false
                    throw error
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (transactionId == null) "収支を追加" else "収支を編集") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") } }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    enabled = !isSaving,
                    onClick = ::saveTransaction,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).testTag("transaction_save")
                ) { Text("保存") }
            }
        }
    ) { inner ->
        if (!loaded) return@Scaffold
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = currentType == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE.name; categoryId = null }, label = { Text("支出") })
                    FilterChip(selected = currentType == TransactionType.INCOME, onClick = { type = TransactionType.INCOME.name; categoryId = null }, label = { Text("収入") })
                }
            }
            item {
                AmountInput(amount, { amount = it; amountError = null }, amountError, "金額（円）")
            }
            item {
                Text("カテゴリ", style = MaterialTheme.typography.labelLarge)
                categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (visibleCategories.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("選択できるカテゴリがありません")
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onCategoryManagement) { Text("カテゴリを管理") }
                        }
                    }
                } else {
                    CategoryGrid(visibleCategories, categoryId) { category -> categoryId = category.id; categoryError = null }
                }
            }
            item {
                OutlinedButton(onClick = { isDatePickerVisible = true }, modifier = Modifier.fillMaxWidth()) { Text("日付　$dateText") }
            }
            item {
                OutlinedTextField(value = memo, onValueChange = { if (it.length <= 200) memo = it }, label = { Text("メモ（任意）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            if (transactionId != null) item {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("この記録を削除", color = MaterialTheme.colorScheme.error) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (isDatePickerVisible) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = localDateToDatePickerMillis(LocalDate.parse(dateText)))
        DatePickerDialog(onDismissRequest = { isDatePickerVisible = false }, confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { dateText = datePickerMillisToLocalDate(it).toString() }; isDatePickerVisible = false }) { Text("決定") } }, dismissButton = { TextButton(onClick = { isDatePickerVisible = false }) { Text("キャンセル") } }) { DatePicker(state = picker) }
    }
    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("記録を削除しますか？") }, text = { Text("削除した記録は元に戻せません。") }, confirmButton = { TextButton(onClick = { scope.launch(Dispatchers.Main.immediate) { repository.deleteTransaction(transactionId!!); onBack() } }) { Text("削除") } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text("キャンセル") } })
    }
}

@Composable
private fun BudgetScreen(repository: KakeiboRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val summaries by remember { repository.observeCategoryBudgetSummaries(YearMonth.now()) }.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<CategoryBudgetSummary?>(null) }
    var draftAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("予算設定") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") } })
    }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val categoryTotal = summaries.mapNotNull { it.budget }.sum()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Text("合計 ${yen(categoryTotal)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                }
            }
            items(summaries, key = { it.category.id }) { item ->
                Surface(
                    onClick = {
                        editing = item
                        draftAmount = item.budget?.toString().orEmpty()
                        error = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(item.category.colorArgb).copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) { CategoryIcon(item.category, Modifier.padding(8.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text(item.category.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(item.budget?.let(::yen) ?: "—", color = if (item.budget == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editing?.let { item ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(item.category, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${item.category.name}の予算")
                }
            },
            text = { AmountInput(draftAmount, { draftAmount = it; error = null }, error, "予算（円）", fieldTestTag = "budget_amount_input", keypadTestTagPrefix = "budget_key") },
            confirmButton = {
                TextButton(modifier = Modifier.testTag("budget_save"), onClick = {
                    val parsed = parseAmountText(draftAmount)
                    error = validateAmountText(draftAmount)?.replace("金額", "予算")
                    if (error == null) {
                        scope.launch {
                            repository.saveCategoryBudget(item.category.id, parsed)
                            editing = null
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    if (item.budget != null) {
                        TextButton(onClick = {
                            scope.launch {
                                repository.saveCategoryBudget(item.category.id, null)
                                editing = null
                            }
                        }) { Text("解除") }
                    }
                    TextButton(onClick = { editing = null }) { Text("キャンセル") }
                }
            }
        )
    }
}

@Composable
private fun CategoryScreen(repository: KakeiboRepository) {
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryModel?>(null) }
    var newName by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("more") }
    var error by remember { mutableStateOf<String?>(null) }
    val categories by repository.observeCategories(activeOnly = false).collectAsState(initial = emptyList())
    val selected = TransactionType.valueOf(type)
    Scaffold(topBar = { TopAppBar(title = { Text("カテゴリ管理") }) }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selected == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE.name }, label = { Text("支出") })
                    FilterChip(selected = selected == TransactionType.INCOME, onClick = { type = TransactionType.INCOME.name }, label = { Text("収入") })
                }
            }
            items(categories.filter { it.type == selected }, key = { it.id }) { category ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    CategoryIcon(category, Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(category.name, modifier = Modifier.weight(1f))
                    Text(if (category.isActive) "表示中" else "非表示", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { editing = category; newName = category.name; selectedIconKey = category.iconKey; error = null }) { Text("編集") }
                    TextButton(onClick = { scope.launch { repository.setCategoryActive(category.id, !category.isActive) } }) { Text(if (category.isActive) "隠す" else "戻す") }
                }
                HorizontalDivider()
            }
            item {
                OutlinedButton(onClick = { showAdd = true; newName = ""; selectedIconKey = "more"; error = null }, modifier = Modifier.fillMaxWidth()) { Text("カテゴリを追加") }
            }
        }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("カテゴリを追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it; error = null }, label = { Text("カテゴリ名") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
                    IconPicker(selectedIconKey) { selectedIconKey = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { repository.addCategory(selected, newName, selectedIconKey, defaultColorForIcon(selectedIconKey)) }
                            .onSuccess { newName = ""; showAdd = false }
                            .onFailure { error = it.message }
                    }
                }) { Text("追加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("キャンセル") } }
        )
    }
    editing?.let { category ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("カテゴリを編集") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it; error = null }, label = { Text("カテゴリ名") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
                    IconPicker(selectedIconKey) { selectedIconKey = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            repository.renameCategory(category.id, newName)
                            repository.updateCategoryAppearance(category.id, selectedIconKey, defaultColorForIcon(selectedIconKey))
                        }
                            .onSuccess { editing = null }
                            .onFailure { error = it.message }
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("キャンセル") } }
        )
    }
}

private fun yen(amount: Long): String = NumberFormat.getNumberInstance(Locale.JAPAN).format(amount) + "円"
