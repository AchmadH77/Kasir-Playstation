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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import id.kasir.app.data.MemberEntity
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
    val tabTitles = listOf("Unit", "Jajan", "Member", "Laporan", "Lainnya")

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
                bottomBar = {
                    ShortNavigationBar {
                        ShortNavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                            label = { Text("Unit") }
                        )
                        ShortNavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                            label = { Text("Jajan") }
                        )
                        ShortNavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.Group, contentDescription = null) },
                            label = { Text("Member") }
                        )
                        ShortNavigationBarItem(
                            selected = tab == 3,
                            onClick = { tab = 3 },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                            label = { Text("Laporan") }
                        )
                        ShortNavigationBarItem(
                            selected = tab == 4,
                            onClick = { tab = 4 },
                            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                            label = { Text("Lainnya") }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { padding ->
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    modifier = Modifier.padding(padding),
                    label = "tab"
                ) { target ->
                    when (target) {
                        0 -> UnitTab(dao = dao, notify = notify)
                        1 -> JajanTab(dao = dao, notify = notify)
                        2 -> MemberTab(dao = dao, notify = notify)
                        3 -> LaporanTab(dao = dao, notify = notify)
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTab(dao: KasirDao, notify: (String) -> Unit) {
    val units by dao.unitsFlow().collectAsState(initial = emptyList())
    val active by dao.activeSessionsFlow().collectAsState(initial = emptyList())
    val finished by dao.finishedSessionsFlow().collectAsState(initial = emptyList())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    var startUnit by remember { mutableStateOf<UnitEntity?>(null) }
    var detailSessionId by remember { mutableStateOf<Long?>(null) }
    var receiptSession by remember { mutableStateOf<SessionEntity?>(null) }

    val todayStart = startOfDay(now)
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
                    now = now,
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
                now = now,
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
private fun UnitRow(unit: UnitEntity, session: SessionEntity?, now: Long, onClick: () -> Unit) {
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
                        session == null -> formatRupiah(unit.hourlyRate) + " / jam"
                        paused -> "Sedang jeda"
                        overtime -> "Lewat waktu"
                        session.mode == MODE_PAKET -> "Paket " + formatMinutes(session.paketMinutes) +
                            (session.memberName?.let { " - Member: $it" } ?: "")
                        else -> "Open billing" +
                            (session.memberName?.let { " - Member: $it" } ?: "")
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
    val members by dao.membersFlow().collectAsState(initial = emptyList())
    var mode by remember { mutableStateOf(MODE_PAKET) }
    var paket by remember { mutableIntStateOf(120) }
    var memberId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mulai Sesi - " + unit.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    unit.consoleType + " - " + formatRupiah(unit.hourlyRate) + " / jam",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    SegmentedButton(
                        selected = mode == MODE_PAKET,
                        onClick = { mode = MODE_PAKET },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Paket") }
                    )
                    SegmentedButton(
                        selected = mode == MODE_OPEN,
                        onClick = { mode = MODE_OPEN },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Open") }
                    )
                }
                if (mode == MODE_PAKET) {
                    Text(
                        "Durasi paket",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PAKET_OPTIONS.forEach { m ->
                            FilterChip(
                                selected = paket == m,
                                onClick = { paket = m },
                                label = { Text(formatMinutes(m)) }
                            )
                        }
                    }
                    Text(
                        "Harga paket: " + formatRupiah(unit.hourlyRate * paket / 60),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        "Open billing: timer jalan terus, ditagih per blok 10 menit saat stop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                Text(
                    "Member (opsional)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = memberId == null,
                        onClick = { memberId = null },
                        label = { Text("Tanpa member") }
                    )
                    members.forEach { m ->
                        FilterChip(
                            selected = memberId == m.id,
                            onClick = { memberId = m.id },
                            label = { Text(m.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    dao.insertSession(
                        SessionEntity(
                            unitId = unit.id,
                            unitName = unit.name,
                            consoleType = unit.consoleType,
                            hourlyRate = unit.hourlyRate,
                            memberId = memberId,
                            memberName = members.firstOrNull { it.id == memberId }?.name,
                            mode = mode,
                            paketMinutes = if (mode == MODE_PAKET) paket else 0,
                            startEpoch = System.currentTimeMillis()
                        )
                    )
                    onStarted()
                }
            }) { Text("Mulai") }
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
    now: Long,
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
                session.memberName?.let {
                    Text(
                        "Member: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
            now = now,
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
    now: Long,
    onDismiss: () -> Unit,
    onDone: (SessionEntity) -> Unit
) {
    val members by dao.membersFlow().collectAsState(initial = emptyList())
    val member: MemberEntity? = members.firstOrNull { it.id == session.memberId }
    val scope = rememberCoroutineScope()
    var useBalance by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PAY_TUNAI) }
    var paidText by remember { mutableStateOf("") }

    val billable = billableMinutes(session, now)
    val rentalGross = rentalCost(session, now)
    val snackTotal = sessionSnacks.sumOf { it.price * it.qty }
    val minutesFromBalance = if (useBalance && member != null) {
        minOf(member.timeBalanceMinutes, billable)
    } else {
        0
    }
    val rentalCash = session.hourlyRate * (billable - minutesFromBalance) / 60
    val balanceValue = rentalGross - rentalCash
    val discount = if (member != null) rentalCash * member.discountPercent / 100 else 0L
    val total = rentalCash - discount + snackTotal
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
                if (balanceValue > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Saldo waktu (" + formatMinutes(minutesFromBalance) + ")",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "-" + formatRupiah(balanceValue),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (discount > 0 && member != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Diskon member " + member.discountPercent + "%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "-" + formatRupiah(discount),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                if (member != null && member.timeBalanceMinutes > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pakai saldo waktu", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Saldo " + member.name + ": " +
                                    formatMinutes(member.timeBalanceMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = useBalance, onCheckedChange = { useBalance = it })
                    }
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
                            discountAmount = discount,
                            total = total,
                            paymentMethod = method,
                            paidAmount = if (method == PAY_QRIS) total else paid,
                            changeAmount = if (method == PAY_QRIS) 0L else paid - total,
                            balanceMinutesUsed = minutesFromBalance
                        )
                        dao.updateSession(finished)
                        if (minutesFromBalance > 0 && member != null) {
                            dao.useBalance(member.id, minutesFromBalance)
                        }
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
