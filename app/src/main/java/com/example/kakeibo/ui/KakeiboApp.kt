@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kakeibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kakeibo.KakeiboRepository
import com.example.kakeibo.model.CategoryModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val HOME = "home"
private const val HISTORY = "history"
private const val EDITOR = "editor?transactionId={transactionId}"
private const val BUDGET = "budget?yearMonth={yearMonth}"
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = bottomRoute == HOME, onClick = { navController.navigate(HOME) }, icon = { Text("⌂") }, label = { Text("ホーム") })
                NavigationBarItem(selected = bottomRoute == HISTORY, onClick = { navController.navigate(HISTORY) }, icon = { Text("≡") }, label = { Text("履歴") })
                NavigationBarItem(selected = bottomRoute == CATEGORIES, onClick = { navController.navigate(CATEGORIES) }, icon = { Text("⚙") }, label = { Text("設定") })
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = HOME, modifier = Modifier.padding(padding)) {
            composable(HOME) {
                HomeScreen(repository, onAdd = { navController.navigate("editor?transactionId=-1") }, onBudget = { month -> navController.navigate("budget?yearMonth=$month") }, onOpen = { navController.navigate("editor?transactionId=$it") })
            }
            composable(HISTORY) {
                HistoryScreen(repository, onOpen = { navController.navigate("editor?transactionId=$it") }, onAdd = { navController.navigate("editor?transactionId=-1") })
            }
            composable(
                route = EDITOR,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                EditorScreen(repository, entry.arguments?.getLong("transactionId")?.takeIf { it >= 0 }, onBack = { navController.navigateUp() })
            }
            composable(
                route = BUDGET,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType; defaultValue = YearMonth.now().toString() })
            ) { entry ->
                val initialMonth = entry.arguments?.getString("yearMonth")
                    ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                    ?: YearMonth.now()
                BudgetScreen(repository, initialMonth, onBack = { navController.navigateUp() })
            }
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
private fun HomeScreen(repository: KakeiboRepository, onAdd: () -> Unit, onBudget: (YearMonth) -> Unit, onOpen: (Long) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = YearMonth.parse(month)
    val summary by remember(selectedMonth) { repository.observeSummary(selectedMonth) }.collectAsState(initial = MonthlySummary(selectedMonth))
    val transactions by remember(selectedMonth) { repository.observeTransactions(selectedMonth) }.collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, floatingActionButton = { Button(onClick = onAdd, shape = RoundedCornerShape(50)) { Text("＋ 記録") } }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MonthSelector(selectedMonth, { month = selectedMonth.minusMonths(1).toString() }, { month = selectedMonth.plusMonths(1).toString() }) }
            item {
                BudgetCard(summary, { onBudget(selectedMonth) })
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
private fun BudgetCard(summary: MonthlySummary, onBudget: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val title = if (summary.month == YearMonth.now()) "今月の予算" else "${summary.month.year}年${summary.month.monthValue}月の予算"
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBudget, modifier = Modifier.testTag("budget_action")) { Text(if (summary.budget == null) "設定" else "変更") }
            }
            if (summary.budget == null) {
                Text("予算を設定すると、使える残額を確認できます")
            } else {
                Text("${yen(summary.expense)} / ${yen(summary.budget)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { (summary.usageRatio ?: 0f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                val remaining = summary.remainingBudget ?: 0
                Text(if (remaining >= 0) "残り ${yen(remaining)}" else "${yen(-remaining)} 超過", color = if (remaining >= 0) Color.Unspecified else MaterialTheme.colorScheme.error)
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

@Composable
private fun TransactionRow(transaction: TransactionModel, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(if (transaction.type == TransactionType.INCOME) "＋" else "−", color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
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
private fun EditorScreen(repository: KakeiboRepository, transactionId: Long?, onBack: () -> Unit) {
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

    Scaffold(topBar = { TopAppBar(title = { Text(if (transactionId == null) "収支を追加" else "収支を編集") }, navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } }) }) { inner ->
        if (!loaded) return@Scaffold
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = currentType == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE.name; categoryId = null }, label = { Text("支出") })
                    FilterChip(selected = currentType == TransactionType.INCOME, onClick = { type = TransactionType.INCOME.name; categoryId = null }, label = { Text("収入") })
                }
            }
            item {
                OutlinedTextField(value = amount, onValueChange = { amount = it; amountError = null }, label = { Text("金額（円）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = amountError != null, supportingText = { amountError?.let { Text(it) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text("カテゴリ", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(visibleCategories, key = { it.id }) { category ->
                        FilterChip(selected = category.id == categoryId, onClick = { categoryId = category.id; categoryError = null }, label = { Text(category.name) })
                    }
                }
                categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            item {
                OutlinedButton(onClick = { isDatePickerVisible = true }, modifier = Modifier.fillMaxWidth()) { Text("日付　$dateText") }
            }
            item {
                OutlinedTextField(value = memo, onValueChange = { if (it.length <= 200) memo = it }, label = { Text("メモ（任意）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            item {
                Button(enabled = !isSaving, onClick = {
                    if (!isSaving) {
                        val parsed = parseAmountText(amount)
                        amountError = validateAmountText(amount)
                        categoryError = if (categoryId == null) "カテゴリを選択してください" else null
                        if (amountError == null && categoryError == null) {
                            isSaving = true
                            scope.launch(Dispatchers.Main.immediate) {
                                try {
                                    repository.saveTransaction(transactionId, currentType, parsed!!, categoryId!!, LocalDate.parse(dateText), memo)
                                    onBack()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
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
private fun BudgetScreen(repository: KakeiboRepository, initialMonth: YearMonth, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var monthText by rememberSaveable { mutableStateOf(initialMonth.toString()) }
    val month = YearMonth.parse(monthText)
    val summary by remember(month) { repository.observeSummary(month) }.collectAsState(initial = MonthlySummary(month))
    var amount by rememberSaveable(monthText) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    LaunchedEffect(summary.budget) { amount = summary.budget?.toString().orEmpty() }
    Scaffold(topBar = { TopAppBar(title = { Text("月の予算") }, navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } }) }) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MonthSelector(month, { monthText = month.minusMonths(1).toString() }, { monthText = month.plusMonths(1).toString() })
            OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, label = { Text("予算（円）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error != null, supportingText = { error?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
            Button(enabled = !isSaving, onClick = {
                if (!isSaving) {
                    val parsed = parseAmountText(amount)
                    error = validateAmountText(amount)?.replace("金額", "予算")
                    if (error == null) {
                        isSaving = true
                        scope.launch(Dispatchers.Main.immediate) {
                            try {
                                repository.saveBudget(month, parsed)
                                onBack()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
            if (summary.budget != null) TextButton(enabled = !isSaving, onClick = {
                isSaving = true
                scope.launch(Dispatchers.Main.immediate) {
                    try {
                        repository.saveBudget(month, null)
                        onBack()
                    } finally {
                        isSaving = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("予算を解除") }
        }
    }
}

@Composable
private fun CategoryScreen(repository: KakeiboRepository) {
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryModel?>(null) }
    var newName by remember { mutableStateOf("") }
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
                    Text(category.name, modifier = Modifier.weight(1f))
                    Text(if (category.isActive) "表示中" else "非表示", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { editing = category; newName = category.name; error = null }) { Text("編集") }
                    TextButton(onClick = { scope.launch { repository.setCategoryActive(category.id, !category.isActive) } }) { Text(if (category.isActive) "隠す" else "戻す") }
                }
                HorizontalDivider()
            }
            item { OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("カテゴリを追加") } }
        }
    }
    if (showAdd) {
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("カテゴリを追加") }, text = { Column { OutlinedTextField(value = newName, onValueChange = { newName = it; error = null }, label = { Text("カテゴリ名") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } }) } }, confirmButton = { TextButton(onClick = { scope.launch { runCatching { repository.addCategory(selected, newName) }.onSuccess { newName = ""; showAdd = false }.onFailure { error = it.message } } }) { Text("追加") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("キャンセル") } })
    }
    editing?.let { category ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("カテゴリ名を変更") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it; error = null }, label = { Text("カテゴリ名") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { repository.renameCategory(category.id, newName) }
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
