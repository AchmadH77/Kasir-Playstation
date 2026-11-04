package id.kasir.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.kasir.app.data.DatabaseProvider
import id.kasir.app.data.KasirDao
import id.kasir.app.data.SeedData
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.UnitEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KasirApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val dao = remember { DatabaseProvider.get(context).dao() }
    val systemDark = isSystemInDarkTheme()
    var darkOverride by remember {
        mutableStateOf(
            if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false) else null
        )
    }
    val dark = darkOverride ?: systemDark
    var tab by remember { mutableIntStateOf(0) }
    var showUnitManage by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val tabTitles = listOf("Unit", "Jajan", "Laporan", "Lainnya")

    LaunchedEffect(Unit) { SeedData.ensure(dao) }

    KasirTheme(darkTheme = dark) {
        if (showUnitManage) {
            UnitManageScreen(dao = dao, notify = notify, onBack = { showUnitManage = false })
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(tabTitles[tab], fontWeight = FontWeight.ExtraBold) }
                    )
                },
                snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.padding(bottom = 112.dp)) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        label = "tab"
                    ) { target ->
                        when (target) {
                            0 -> UnitTab(dao = dao, notify = notify)
                            1 -> JajanTab(dao = dao, notify = notify)
                            2 -> LaporanTab(dao = dao, notify = notify)
                            else -> LainnyaTab(
                                dao = dao,
                                dark = dark,
                                onDarkChange = { checked ->
                                    darkOverride = checked
                                    prefs.edit().putBoolean("dark_theme", checked).apply()
                                },
                                notify = notify,
                                onOpenUnitManage = { showUnitManage = true }
                            )
                        }
                    }
                    FloatingDock(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

// Dock melayang menggantikan NavigationBar: kapsul surfaceContainerHigh
// dengan bayangan; semua tab ikon + label kecil di bawahnya, dan ikon tab
// aktif berlingkar primary (varian V6 - Ikon + Label Bawah).
@Composable
private fun FloatingDock(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Icons.Default.SportsEsports to "Unit",
        Icons.Default.Fastfood to "Jajan",
        Icons.Default.Assessment to "Laporan",
        Icons.Default.MoreHoriz to "Lainnya"
    )
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val isSelected = index == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * A one-second clock owned by whichever composable calls it. Keeping the
 * ticking state inside the small composables that actually show a timer
 * (unit rows, the session detail dialog) means only they recompose every
 * second; the header, the rest of the list, and the tab itself stay idle.
 * When [active] is false the value never changes and no loop runs.
 */
@Composable
private fun rememberNowMillis(active: Boolean = true): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        if (active) {
            while (true) {
                delay(1000)
                now.value = System.currentTimeMillis()
            }
        }
    }
    return now
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTab(dao: KasirDao, notify: (String) -> Unit) {
    val units by dao.unitsFlow().collectAsState(initial = emptyList())
    val active by dao.activeSessionsFlow().collectAsState(initial = emptyList())
    val finished by dao.finishedSessionsFlow().collectAsState(initial = emptyList())
    var startUnit by remember { mutableStateOf<UnitEntity?>(null) }
    var detailSessionId by remember { mutableStateOf<Long?>(null) }
    var receiptSession by remember { mutableStateOf<SessionEntity?>(null) }

    val todayStart = startOfDay(System.currentTimeMillis())
    val todayIncome = finished.filter { (it.endEpoch ?: 0L) >= todayStart }.sumOf { it.total }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hari ini " + formatRupiah(todayIncome),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    active.size.toString() + " unit bermain",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (units.isEmpty()) {
            Text(
                "Belum ada unit. Tambahkan lewat Lainnya - Atur Unit & Tarif.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(units, key = { it.id }) { unit ->
                val session = active.firstOrNull { it.unitId == unit.id }
                UnitRow(
                    unit = unit,
                    session = session,
                    onClick = {
                        if (session == null) startUnit = unit else detailSessionId = session.id
                    }
                )
            }
        }
    }

    startUnit?.let { unit ->
        StartSessionDialog(
            dao = dao,
            unit = unit,
            onDismiss = { startUnit = null },
            onStarted = {
                startUnit = null
                notify("Sesi dimulai di " + unit.name)
            }
        )
    }
    detailSessionId?.let { sid ->
        val session = active.firstOrNull { it.id == sid }
        if (session != null) {
            SessionDetailDialog(
                dao = dao,
                session = session,
                notify = notify,
                onDismiss = { detailSessionId = null },
                onCheckoutDone = { finishedSession ->
                    detailSessionId = null
                    receiptSession = finishedSession
                }
            )
        }
    }
    receiptSession?.let { s ->
        ReceiptDialog(dao = dao, session = s, onDismiss = { receiptSession = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitRow(unit: UnitEntity, session: SessionEntity?, onClick: () -> Unit) {
    // This row owns its clock: only the row with a running session recomposes
    // every second, not the whole tab.
    val now by rememberNowMillis(active = session != null)
    val paused = session?.status == STATUS_PAUSED
    val remaining = if (session != null && session.mode == MODE_PAKET) remainingMillis(session, now) else 0L
    val overtime = session != null && session.mode == MODE_PAKET && remaining < 0
    val container = when {
        session == null -> MaterialTheme.colorScheme.surfaceContainerHigh
        paused -> MaterialTheme.colorScheme.tertiaryContainer
        overtime -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val accent = when {
        paused -> MaterialTheme.colorScheme.tertiary
        overtime -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    unit.name + " - " + unit.consoleType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        session == null -> formatRupiah(effectiveRate(unit)) + " / jam"
                        paused -> "Sedang jeda"
                        overtime -> "Lewat waktu"
                        session.mode == MODE_PAKET -> "Paket " + formatMinutes(session.paketMinutes)
                        else -> "Open billing"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (session == null) {
                Column(
                    modifier = Modifier.padding(end = 14.dp, top = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Kosong",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onClick,
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Mulai") }
                }
            } else {
                Column(
                    modifier = Modifier.padding(end = 14.dp, top = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val timerText = when {
                        session.mode == MODE_OPEN -> formatHms(elapsedMillis(session, now))
                        overtime -> "+" + formatHms(-remaining)
                        else -> formatHms(remaining)
                    }
                    Text(
                        timerText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        formatRupiah(rentalCost(session, now)) + " berjalan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSessionDialog(
    dao: KasirDao,
    unit: UnitEntity,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    var paket by remember { mutableIntStateOf(120) }
    var priceText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val price = priceText.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mulai Sesi - " + unit.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    unit.consoleType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Durasi paket",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PAKET_OPTIONS.chunked(3).forEach { rowOptions ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowOptions.forEach { m ->
                                FilterChip(
                                    selected = paket == m,
                                    onClick = { paket = m },
                                    label = { Text(formatMinutes(m)) }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { c -> c.isDigit() } },
                    label = { Text("Harga paket (Rp)") },
                    placeholder = { Text("mis. 10000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )
                Text(
                    "Harga diisi manual dan tetap: lewat dari durasi paket tidak menambah biaya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = price > 0,
                onClick = {
                    scope.launch {
                        dao.insertSession(
                            SessionEntity(
                                unitId = unit.id,
                                unitName = unit.name,
                                consoleType = unit.consoleType,
                                hourlyRate = effectiveRate(unit),
                                mode = MODE_PAKET,
                                paketMinutes = paket,
                                paketPrice = price,
                                startEpoch = System.currentTimeMillis()
                            )
                        )
                        onStarted()
                    }
                }
            ) { Text("Mulai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailDialog(
    dao: KasirDao,
    session: SessionEntity,
    notify: (String) -> Unit,
    onDismiss: () -> Unit,
    onCheckoutDone: (SessionEntity) -> Unit
) {
    val sessionSnacks by dao.sessionSnacksFlow(session.id).collectAsState(initial = emptyList())
    val snacks by dao.snacksFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddSnack by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var confirmVoid by remember { mutableStateOf(false) }
    var showAddTime by remember { mutableStateOf(false) }
    // The dialog owns its clock (small subtree); the tab behind it stays idle.
    val now by rememberNowMillis()
    val snackTotal = sessionSnacks.sumOf { it.price * it.qty }
    val rental = rentalCost(session, now)
    val paused = session.status == STATUS_PAUSED
    val remaining = if (session.mode == MODE_PAKET) remainingMillis(session, now) else 0L
    val overtime = session.mode == MODE_PAKET && remaining < 0

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(
                        session.unitName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        session.consoleType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                val timerText = when {
                    session.mode == MODE_OPEN -> formatHms(elapsedMillis(session, now))
                    overtime -> "+" + formatHms(-remaining)
                    else -> formatHms(remaining)
                }
                Text(
                    timerText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    when {
                        paused -> "Sedang jeda - waktu tidak dihitung"
                        session.mode == MODE_PAKET -> "Paket " + formatMinutes(session.paketMinutes) +
                            " - mulai " + formatTime(session.startEpoch)
                        else -> "Open billing - mulai " + formatTime(session.startEpoch)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sewa berjalan", style = MaterialTheme.typography.bodyMedium)
                    Text(formatRupiah(rental), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jajan", style = MaterialTheme.typography.bodyMedium)
                    Text(formatRupiah(snackTotal), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total sementara",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatRupiah(rental + snackTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Jajan di sesi ini",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 18.dp)
                )
                if (sessionSnacks.isEmpty()) {
                    Text(
                        "Belum ada jajan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                sessionSnacks.forEach { item ->
                    val stock = snacks.firstOrNull { it.id == item.snackId }?.stock ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.snackName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatRupiah(item.price) + " x " + item.qty + " = " +
                                    formatRupiah(item.price * item.qty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                if (item.qty <= 1) {
                                    dao.deleteSessionSnack(item)
                                } else {
                                    dao.updateSessionSnack(item.copy(qty = item.qty - 1))
                                }
                                dao.increaseStock(item.snackId, 1)
                            }
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text(item.qty.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(
                            enabled = stock > 0,
                            onClick = {
                                scope.launch {
                                    dao.updateSessionSnack(item.copy(qty = item.qty + 1))
                                    dao.decreaseStock(item.snackId, 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
                FilledTonalButton(
                    onClick = { showAddSnack = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) { Text("Tambah Jajan") }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (paused) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val ps = session.pauseStartEpoch
                                    val extra = if (ps != null) {
                                        System.currentTimeMillis() - ps
                                    } else {
                                        0L
                                    }
                                    dao.updateSession(
                                        session.copy(
                                            status = STATUS_RUNNING,
                                            pausedMillis = session.pausedMillis + extra,
                                            pauseStartEpoch = null
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Lanjut", modifier = Modifier.padding(start = 6.dp))
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    dao.updateSession(
                                        session.copy(
                                            status = STATUS_PAUSED,
                                            pauseStartEpoch = System.currentTimeMillis()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Text("Jeda", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    if (session.mode == MODE_PAKET) {
                        FilledTonalButton(
                            onClick = { showAddTime = !showAddTime },
                            modifier = Modifier.weight(1f)
                        ) { Text("Tambah Waktu") }
                    }
                }
                if (showAddTime && session.mode == MODE_PAKET) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 60, 120).forEach { add ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        dao.updateSession(
                                            session.copy(
                                                paketMinutes = session.paketMinutes + add
                                            )
                                        )
                                        notify("Waktu ditambah " + formatMinutes(add))
                                    }
                                    showAddTime = false
                                },
                                label = { Text("+" + formatMinutes(add)) }
                            )
                        }
                    }
                }
                Button(
                    onClick = { showCheckout = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) { Text("Stop & Bayar") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { confirmVoid = true }) {
                        Text(
                            "Batalkan Sesi",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
            }
        }
    }

    if (showAddSnack) {
        AddSnackDialog(
            dao = dao,
            session = session,
            onDismiss = { showAddSnack = false }
        )
    }
    if (showCheckout) {
        CheckoutDialog(
            dao = dao,
            session = session,
            sessionSnacks = sessionSnacks,
            onDismiss = { showCheckout = false },
            onDone = { finished -> onCheckoutDone(finished) }
        )
    }
    if (confirmVoid) {
        AlertDialog(
            onDismissRequest = { confirmVoid = false },
            title = { Text("Batalkan sesi ini?") },
            text = { Text("Sesi dihapus dan stok jajan yang sudah ditambahkan dikembalikan.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        sessionSnacks.forEach { dao.increaseStock(it.snackId, it.qty) }
                        dao.deleteSessionSnacks(session.id)
                        dao.deleteSession(session)
                        confirmVoid = false
                        notify("Sesi dibatalkan")
                        onDismiss()
                    }
                }) { Text("Ya, Batalkan") }
            },
            dismissButton = {
                TextButton(onClick = { confirmVoid = false }) { Text("Kembali") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSnackDialog(dao: KasirDao, session: SessionEntity, onDismiss: () -> Unit) {
    val snacks by dao.snacksFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jajan") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (snacks.isEmpty()) {
                    Text("Menu jajan masih kosong.")
                }
                snacks.forEach { snack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(snack.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                formatRupiah(snack.price) + " - stok " + snack.stock,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            enabled = snack.stock > 0,
                            onClick = {
                                scope.launch {
                                    val existing = dao.sessionSnack(session.id, snack.id)
                                    if (existing != null) {
                                        dao.updateSessionSnack(existing.copy(qty = existing.qty + 1))
                                    } else {
                                        dao.insertSessionSnack(
                                            SessionSnackEntity(
                                                sessionId = session.id,
                                                snackId = snack.id,
                                                snackName = snack.name,
                                                price = snack.price,
                                                qty = 1
                                            )
                                        )
                                    }
                                    dao.decreaseStock(snack.id, 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutDialog(
    dao: KasirDao,
    session: SessionEntity,
    sessionSnacks: List<SessionSnackEntity>,
    onDismiss: () -> Unit,
    onDone: (SessionEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(PAY_TUNAI) }
    var paidText by remember { mutableStateOf("") }
    // Frozen at open: the bill must not tick up while payment is being taken.
    val now = remember { System.currentTimeMillis() }

    val billable = billedMinutes(session, now)
    val rentalGross = rentalCost(session, now)
    val snackTotal = sessionSnacks.sumOf { it.price * it.qty }
    val total = rentalGross + snackTotal
    val paid = paidText.toLongOrNull() ?: 0L
    val canPay = method == PAY_QRIS || paid >= total

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran - " + session.unitName) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Sewa (" + formatMinutes(billable) + ")",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(formatRupiah(rentalGross), style = MaterialTheme.typography.bodyMedium)
                }
                if (snackTotal > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jajan", style = MaterialTheme.typography.bodyMedium)
                        Text(formatRupiah(snackTotal), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatRupiah(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    SegmentedButton(
                        selected = method == PAY_TUNAI,
                        onClick = { method = PAY_TUNAI },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Tunai") }
                    )
                    SegmentedButton(
                        selected = method == PAY_QRIS,
                        onClick = { method = PAY_QRIS },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("QRIS") }
                    )
                }
                if (method == PAY_TUNAI) {
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { v -> paidText = v.filter { it.isDigit() } },
                        label = { Text("Uang diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { paidText = total.toString() },
                            label = { Text("Uang Pas") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { paidText = "50000" },
                            label = { Text("50 rb") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { paidText = "100000" },
                            label = { Text("100 rb") }
                        )
                    }
                    if (paidText.isNotEmpty()) {
                        Text(
                            if (paid >= total) {
                                "Kembalian: " + formatRupiah(paid - total)
                            } else {
                                "Kurang: " + formatRupiah(total - paid)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (paid >= total) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canPay,
                onClick = {
                    scope.launch {
                        val endNow = System.currentTimeMillis()
                        var pausedMillis = session.pausedMillis
                        val ps = session.pauseStartEpoch
                        if (session.status == STATUS_PAUSED && ps != null) {
                            pausedMillis += endNow - ps
                        }
                        val finished = session.copy(
                            status = STATUS_FINISHED,
                            endEpoch = endNow,
                            pausedMillis = pausedMillis,
                            pauseStartEpoch = null,
                            rentalCost = rentalGross,
                            snackCost = snackTotal,
                            total = total,
                            paymentMethod = method,
                            paidAmount = if (method == PAY_QRIS) total else paid,
                            changeAmount = if (method == PAY_QRIS) 0L else paid - total,
                        )
                        dao.updateSession(finished)
                        onDone(finished)
                    }
                }
            ) { Text("Bayar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
