package id.kasir.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.kasir.app.data.DatabaseProvider
import id.kasir.app.data.ReservationEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import id.kasir.app.data.KasirDao
import id.kasir.app.data.SeedData
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.UnitEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Id sesi yang diminta dibuka dari ketukan notifikasi sistem.
    private var openSessionSignal by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureSessionChannel(this)
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
        openSessionSignal = intent
            ?.getLongExtra(EXTRA_OPEN_SESSION, -1L)
            ?.takeIf { it > 0 }
        setContent {
            KasirApp(
                openSignal = openSessionSignal,
                onOpenSignalConsumed = { openSessionSignal = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSessionSignal = intent
            .getLongExtra(EXTRA_OPEN_SESSION, -1L)
            .takeIf { it > 0 }
    }
}

// Transisi perpindahan mengikuti token gerak resmi Material 3 Expressive:
// pudar memakai effects spec dan geser memakai spatial spec dari
// motionScheme tema (MotionScheme.expressive() di Theme.kt), arah geser
// mengikuti arah perpindahannya. Bukan tween manual lagi.
private fun expressiveTransform(motionScheme: MotionScheme, direction: Int): ContentTransform {
    return (fadeIn(motionScheme.defaultEffectsSpec<Float>()) +
        slideInHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) {
            direction * it / 4
        }) togetherWith
        (fadeOut(motionScheme.defaultEffectsSpec<Float>()) +
            slideOutHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) {
                -direction * it / 4
            })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KasirApp(
    openSignal: Long? = null,
    onOpenSignalConsumed: () -> Unit = {}
) {
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
    // Sinyal dari pop-up waktu habis: tab Unit membuka rincian sesi ini.
    var openRequest by remember { mutableStateOf<Long?>(null) }
    // Sesi yang pop-up/notifikasinya sudah ditangani pada jalan aplikasi ini.
    var expiredNotified by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // Sinyal dari notifikasi sistem: buka sesi ini tanpa pop-up ganda.
    LaunchedEffect(openSignal) {
        val sid = openSignal
        if (sid != null) {
            expiredNotified = expiredNotified + sid
            tab = 0
            openRequest = sid
            onOpenSignalConsumed()
        }
    }
    // Lencana stok menipis di ikon tab Makanan.
    val snacksAll by dao.snacksFlow().collectAsState(initial = emptyList())
    val lowFoodCount = lowStockSnacks(snacksAll).size
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val tabTitles = listOf("Unit", "Makanan", "Laporan", "Lainnya")

    LaunchedEffect(Unit) { SeedData.ensure(dao) }

    KasirTheme(darkTheme = dark) {
        val motionScheme = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = showUnitManage,
            transitionSpec = {
                expressiveTransform(motionScheme, if (targetState) 1 else -1)
            },
            label = "unitManage"
        ) { manage ->
            if (manage) {
                UnitManageScreen(dao = dao, notify = notify, onBack = { showUnitManage = false })
            } else {
            Scaffold(
                topBar = {
                    // H2: header berupa pita hijau tua dengan ikon layar
                    // di lingkaran putih, seragam di keempat tab.
                    val tabIcons = listOf(
                        Icons.Default.SportsEsports,
                        Icons.Default.Fastfood,
                        Icons.Default.Assessment,
                        Icons.Default.MoreHoriz
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16324F),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    tabIcons[tab],
                                    contentDescription = null,
                                    tint = Color(0xFF16324F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                tabTitles[tab],
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
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
                        transitionSpec = {
                            expressiveTransform(
                                motionScheme,
                                if (targetState > initialState) 1 else -1
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        label = "tab"
                    ) { target ->
                        when (target) {
                            0 -> UnitTab(
                                dao = dao,
                                notify = notify,
                                openRequest = openRequest,
                                onOpenRequestConsumed = { openRequest = null }
                            )
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
                        foodBadge = lowFoodCount,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            }
        }
        // Pop-up waktu habis: muncul sekali per sesi paket yang waktunya
        // habis, di tab mana pun kasir sedang berada.
        val activeSessions by dao.activeSessionsFlow()
            .collectAsState(initial = emptyList())
        val anyPaketRunning = activeSessions.any {
            it.mode == MODE_PAKET && it.status != STATUS_PAUSED
        }
        val appNow by rememberNowMillis(active = anyPaketRunning)
        val expiredPopup = activeSessions.firstOrNull {
            it.mode == MODE_PAKET && it.status != STATUS_PAUSED &&
                remainingMillis(it, appNow) <= 0 && it.id !in expiredNotified
        }
        // Rekonsiliasi alarm sistem: setiap sesi paket yang berjalan punya
        // alarm tepat di akhir paketnya, supaya notifikasi sistem muncul
        // walau aplikasi tertutup. Sesi yang jeda/selesai/hilang dari
        // daftar aktif alarmnya dibatalkan; perubahan durasi (tambah
        // waktu) menjadwalkan ulang otomatis.
        val alarmContext = LocalContext.current
        var alarmedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
        LaunchedEffect(activeSessions) {
            val shouldAlarm = activeSessions.filter {
                it.mode == MODE_PAKET && it.status != STATUS_PAUSED
            }
            val shouldIds = shouldAlarm.map { it.id }.toSet()
            (alarmedIds - shouldIds).forEach {
                SessionAlarm.cancel(alarmContext, it)
            }
            shouldAlarm.forEach { SessionAlarm.schedule(alarmContext, it) }
            alarmedIds = shouldIds
        }
        // Pengingat reservasi: 15 menit sebelum jam mulai, untuk
        // reservasi mulai hari ini dan seterusnya.
        val allReservations by dao.reservationsFlow()
            .collectAsState(initial = emptyList())
        var alarmedReservationIds by remember {
            mutableStateOf<Set<Long>>(emptySet())
        }
        LaunchedEffect(allReservations) {
            val todayStart = startOfDay(System.currentTimeMillis())
            val upcomingReservations = allReservations.filter {
                it.startEpoch >= todayStart
            }
            val resIds = upcomingReservations.map { it.id }.toSet()
            (alarmedReservationIds - resIds).forEach {
                SessionAlarm.cancelReservation(alarmContext, it)
            }
            upcomingReservations.forEach {
                SessionAlarm.scheduleReservation(alarmContext, it)
            }
            alarmedReservationIds = resIds
        }
        if (expiredPopup != null) {
            AlertDialog(
                onDismissRequest = {
                    expiredNotified = expiredNotified + expiredPopup.id
                },
                title = { Text("Waktu Habis") },
                text = {
                    Text(
                        "Waktu sesi di " + expiredPopup.unitName +
                            " sudah habis (Paket " +
                            formatMinutes(expiredPopup.paketMinutes) + "). " +
                            "Buka sesinya untuk menyelesaikan pembayaran " +
                            "atau menambah waktu."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            expiredNotified = expiredNotified + expiredPopup.id
                            tab = 0
                            openRequest = expiredPopup.id
                        }
                    ) { Text("Buka Sesi") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            expiredNotified = expiredNotified + expiredPopup.id
                        }
                    ) { Text("Nanti Saja") }
                }
            )
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
    foodBadge: Int = 0,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Icons.Default.SportsEsports to "Unit",
        Icons.Default.Fastfood to "Makanan",
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
                        // Lencana jumlah menu stok menipis di tab Makanan.
                        if (index == 1 && foodBadge > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                            ) {
                                Text(
                                    if (foodBadge > 9) {
                                        "9+"
                                    } else {
                                        foodBadge.toString()
                                    },
                                    style = MaterialTheme
                                        .typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme
                                        .colorScheme.onError
                                )
                            }
                        }
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
private fun UnitTab(
    dao: KasirDao,
    notify: (String) -> Unit,
    openRequest: Long? = null,
    onOpenRequestConsumed: () -> Unit = {}
) {
    val units by dao.unitsFlow().collectAsState(initial = emptyList())
    val active by dao.activeSessionsFlow().collectAsState(initial = emptyList())
    val finished by dao.finishedSessionsFlow().collectAsState(initial = emptyList())
    val reservations by dao.reservationsFlow().collectAsState(initial = emptyList())
    var startUnit by remember { mutableStateOf<UnitEntity?>(null) }
    var startPrefill by remember { mutableStateOf<ReservationEntity?>(null) }
    var showReservationForm by remember { mutableStateOf(false) }
    var detailSessionId by remember { mutableStateOf<Long?>(null) }
    var receiptSession by remember { mutableStateOf<SessionEntity?>(null) }
    val scope = rememberCoroutineScope()

    // Pop-up waktu habis meminta sesi ini dibuka.
    LaunchedEffect(openRequest) {
        val sid = openRequest
        if (sid != null) {
            if (active.any { it.id == sid }) detailSessionId = sid
            onOpenRequestConsumed()
        }
    }

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
        // Reservasi: penanda pesanan unit berikutnya + formulir tambah.
        val dayStart = startOfDay(System.currentTimeMillis())
        val upcoming = reservations.filter { it.startEpoch >= dayStart }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reservasi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showReservationForm = true }) {
                        Text("+ Tambah")
                    }
                }
                if (upcoming.isEmpty()) {
                    Text(
                        "Belum ada reservasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                upcoming.take(3).forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                r.unitName + " - " + r.customerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatTime(r.startEpoch) + " - Paket " +
                                    formatMinutes(r.durationMinutes) +
                                    if (r.price > 0) {
                                        " - " + formatRupiah(r.price)
                                    } else {
                                        ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val u = units.firstOrNull { it.id == r.unitId }
                                if (u != null && active.none { it.unitId == u.id }) {
                                    startPrefill = r
                                    startUnit = u
                                } else {
                                    notify("Unit sedang dipakai")
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                scope.launch { dao.deleteReservation(r) }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
                if (upcoming.size > 3) {
                    Text(
                        "+" + (upcoming.size - 3) + " reservasi lainnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    reservation = nextReservationFor(
                        reservations,
                        unit.id,
                        startOfDay(System.currentTimeMillis())
                    ),
                    onClick = {
                        if (session == null) {
                            startPrefill = null
                            startUnit = unit
                        } else {
                            detailSessionId = session.id
                        }
                    }
                )
            }
        }
    }

    startUnit?.let { unit ->
        StartSessionDialog(
            dao = dao,
            unit = unit,
            initialPaket = startPrefill?.durationMinutes ?: 120,
            initialPrice = startPrefill?.price ?: 0L,
            onDismiss = {
                startUnit = null
                startPrefill = null
            },
            onStarted = {
                val usedReservation = startPrefill
                startUnit = null
                startPrefill = null
                if (usedReservation != null) {
                    scope.launch { dao.deleteReservation(usedReservation) }
                }
                notify("Sesi dimulai di " + unit.name)
            }
        )
    }
    if (showReservationForm) {
        ReservationDialog(
            units = units,
            dao = dao,
            onDismiss = { showReservationForm = false },
            onSaved = {
                showReservationForm = false
                notify("Reservasi tersimpan")
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
private fun UnitRow(
    unit: UnitEntity,
    session: SessionEntity?,
    reservation: ReservationEntity? = null,
    onClick: () -> Unit
) {
    // This row owns its clock: only the row with a running session recomposes
    // every second, not the whole tab.
    val now by rememberNowMillis(active = session != null)
    val paused = session?.status == STATUS_PAUSED
    val remaining = if (session != null && session.mode == MODE_PAKET) remainingMillis(session, now) else 0L
    val overtime = session != null && session.mode == MODE_PAKET && remaining < 0
    // Penanda sesi mau habis: sisa waktu 5 menit terakhir.
    val expiring = session != null && !paused && session.mode == MODE_PAKET &&
        remaining in 1..(5 * 60_000L)
    val pulse = rememberInfiniteTransition(label = "expiringPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (expiring) 0.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    // K6: satu nada lembut untuk semua keadaan; warna hanya pada
    // titik status kecil dan tombol Mulai. Waktu berjalan tetap
    // tampil di sisi kanan.
    val dot = when {
        paused -> MaterialTheme.colorScheme.tertiary
        overtime || expiring -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val timerColor = if (expiring || overtime) {
        MaterialTheme.colorScheme.error
    } else {
        Color.Unspecified
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        unit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        unit.consoleType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    when {
                        session == null && reservation != null ->
                            "Siap dipakai - Reservasi " +
                                formatTime(reservation.startEpoch) +
                                " (" + reservation.customerName + ")"
                        session == null -> "Siap dipakai"
                        paused -> "Sedang jeda"
                        overtime -> "Lewat waktu"
                        session.mode == MODE_PAKET ->
                            "Berjalan - Paket " + formatMinutes(session.paketMinutes)
                        else -> "Berjalan - Open billing"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (session == null) {
                OutlinedButton(onClick = onClick) { Text("Mulai") }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(if (expiring) dotAlpha else 1f)
                                .background(dot, CircleShape)
                        )
                        val timerText = when {
                            session.mode == MODE_OPEN ->
                                formatHms(elapsedMillis(session, now))
                            overtime -> "+" + formatHms(-remaining)
                            else -> formatHms(remaining)
                        }
                        Text(
                            timerText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = timerColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
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
private fun ReservationDialog(
    units: List<UnitEntity>,
    dao: KasirDao,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var unit by remember { mutableStateOf(units.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    var dateMillis by remember {
        mutableStateOf(
            java.time.LocalDate.now()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
    }
    val nextHour = LocalDateTime.now().plusHours(1).withMinute(0)
    var hour by remember { mutableIntStateOf(nextHour.hour) }
    var minute by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(120) }
    var priceText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pickedDate = Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    val startEpoch = LocalDateTime.of(
        pickedDate,
        LocalTime.of(hour, minute)
    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reservasi Unit") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Unit",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    units.forEach { u ->
                        FilterChip(
                            selected = unit?.id == u.id,
                            onClick = { unit = u },
                            label = { Text(u.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama pemesan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatDateTime(startEpoch).substringBefore(","))
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatTime(startEpoch))
                    }
                }
                Text(
                    "Durasi paket",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 14.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    PAKET_OPTIONS.forEach { m ->
                        FilterChip(
                            selected = duration == m,
                            onClick = { duration = m },
                            label = { Text(formatMinutes(m)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = groupThousands(priceText),
                    onValueChange = { v ->
                        priceText = v.filter { c -> c.isDigit() }
                    },
                    label = { Text("Harga paket (Rp, boleh kosong)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = unit != null && name.isNotBlank(),
                onClick = {
                    val u = unit
                    if (u != null) {
                        scope.launch {
                            dao.insertReservation(
                                ReservationEntity(
                                    unitId = u.id,
                                    unitName = u.name,
                                    customerName = name.trim(),
                                    startEpoch = startEpoch,
                                    durationMinutes = duration,
                                    price = priceText.toLongOrNull() ?: 0L
                                )
                            )
                            onSaved()
                        }
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Jam Mulai") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = timeState.hour
                        minute = timeState.minute
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSessionDialog(
    dao: KasirDao,
    unit: UnitEntity,
    initialPaket: Int = 120,
    initialPrice: Long = 0L,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    // Nilai awal bisa datang dari reservasi yang dimulai.
    var paket by remember { mutableIntStateOf(initialPaket) }
    var durationText by remember {
        mutableStateOf(
            if (initialPaket % 60 == 0) (initialPaket / 60).toString() else ""
        )
    }
    var priceText by remember {
        mutableStateOf(if (initialPrice > 0) initialPrice.toString() else "")
    }
    val scope = rememberCoroutineScope()
    val price = priceText.toLongOrNull() ?: 0L
    // P2: mulai sesi adalah halaman penuh, bukan dialog - ruang
    // lebih lega dan tombol Mulai Sesi menempel di bawah layar.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        "Mulai Sesi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16324F),
                            contentColor = Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                unit.name + " - " + unit.consoleType,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unit kosong, siap dimulai",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA9C4DE),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    Text(
                        "Durasi paket",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    // M1: kartu durasi besar yang mudah diketuk. Memilih
                    // kartu mengisi kolom durasi di bawahnya; durasinya juga
                    // bisa diketik manual (mis. 8 jam) di kolom itu.
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PAKET_OPTIONS.chunked(3).forEach { rowOptions ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowOptions.forEach { m ->
                                    val selected = paket == m
                                    Card(
                                        onClick = {
                                            paket = m
                                            durationText = if (m % 60 == 0) {
                                                (m / 60).toString()
                                            } else {
                                                ""
                                            }
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        border = if (selected) {
                                            BorderStroke(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                formatMinutes(m),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowOptions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Text(
                        "atau ketik manual:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { v ->
                            val cleaned = v.filter { c -> c.isDigit() }.take(2)
                            durationText = cleaned
                            cleaned.toIntOrNull()?.let { paket = it * 60 }
                        },
                        label = { Text("Durasi (jam)") },
                        placeholder = {
                            Text(
                                if (paket % 60 != 0) {
                                    formatMinutes(paket)
                                } else {
                                    "mis. 2"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = groupThousands(priceText),
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
                Button(
                    enabled = price > 0 && paket > 0,
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(54.dp)
                ) { Text("Mulai Sesi") }
            }
        }
    }
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
    var showCheckout by remember { mutableStateOf(false) }
    var confirmVoid by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var addMinutes by remember { mutableIntStateOf(60) }
    var addPriceText by remember { mutableStateOf("") }
    val addPrice = addPriceText.toLongOrNull() ?: 0L
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                Text(
                    when {
                        paused -> "Sedang jeda - waktu tidak dihitung"
                        session.mode == MODE_PAKET -> "Paket " + formatMinutes(session.paketMinutes) +
                            " - mulai " + formatTime(session.startEpoch)
                        else -> "Open billing - mulai " + formatTime(session.startEpoch)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // S6: the dialog content is split into three segmented
                // tabs - Sesi (billing), Makanan (session snacks), and
                // Waktu (pause plus add-time). Stop & Bayar stays below.
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    listOf("Sesi", "Makanan", "Waktu").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = tab == index,
                            onClick = { tab = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                            label = { Text(label) }
                        )
                    }
                }
                when (tab) {
                    0 -> {
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
                            Text("Makanan", style = MaterialTheme.typography.bodyMedium)
                            Text(formatRupiah(snackTotal), style = MaterialTheme.typography.bodyMedium)
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF16324F),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        }
                    }
                    1 -> {
                        Text(
                            "Makanan di sesi ini",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        if (sessionSnacks.isEmpty()) {
                            Text(
                                "Belum ada makanan.",
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
                        // T1: katalog menyatu di tab ini, jadi tidak
                        // ada dialog kedua yang menutupi dialog sesi.
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Text(
                            "Semua menu - ketuk + untuk menambah",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (snacks.isEmpty()) {
                            Text(
                                "Menu makanan masih kosong.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        snacks.forEach { snack ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        snack.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        formatRupiah(snack.price) + " - stok " +
                                            snack.stock,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme
                                            .colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    enabled = snack.stock > 0,
                                    onClick = {
                                        scope.launch {
                                            val existing = dao.sessionSnack(
                                                session.id,
                                                snack.id
                                            )
                                            if (existing != null) {
                                                dao.updateSessionSnack(
                                                    existing.copy(
                                                        qty = existing.qty + 1
                                                    )
                                                )
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
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                    else -> {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Text("Jeda", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                        if (session.mode == MODE_PAKET) {
                            Text(
                                "Tambah Waktu",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 18.dp)
                            )
                            // Template durasi + nominal manual: harga paket
                            // bertambah sebesar yang diketik, tidak dihitung
                            // dari tarif per jam.
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                listOf(30, 60, 120).forEach { add ->
                                    FilterChip(
                                        selected = addMinutes == add,
                                        onClick = { addMinutes = add },
                                        label = { Text("+" + formatMinutes(add)) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = groupThousands(addPriceText),
                                onValueChange = { v ->
                                    addPriceText = v.filter { c -> c.isDigit() }
                                },
                                label = { Text("Harga tambahan (Rp)") },
                                placeholder = { Text("Tulis nominalnya") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                            Button(
                                enabled = addPrice > 0,
                                onClick = {
                                    scope.launch {
                                        // Sesi lama tanpa harga manual: kunci
                                        // dulu biaya berjalannya sebagai dasar.
                                        val base = if (session.paketPrice > 0) {
                                            session.paketPrice
                                        } else {
                                            rentalCost(session, now)
                                        }
                                        dao.updateSession(
                                            session.copy(
                                                paketMinutes = session.paketMinutes + addMinutes,
                                                paketPrice = base + addPrice
                                            )
                                        )
                                        notify(
                                            "Waktu ditambah " + formatMinutes(addMinutes) +
                                                " - harga paket sekarang " +
                                                formatRupiah(base + addPrice)
                                        )
                                    }
                                    addPriceText = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) { Text("Tambah") }
                        } else {
                            Text(
                                "Sesi open billing tidak memakai paket waktu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
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
            text = { Text("Sesi dihapus dan stok makanan yang sudah ditambahkan dikembalikan.") },
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
                        Text("Makanan", style = MaterialTheme.typography.bodyMedium)
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
                        value = groupThousands(paidText),
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
