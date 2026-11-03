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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val systemDark = isSystemInDarkTheme()
    var darkOverride by remember {
        mutableStateOf(
            if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false) else null
        )
    }
    val dark = darkOverride ?: systemDark
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    KasirTheme(darkTheme = dark) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Kasir", fontWeight = FontWeight.ExtraBold) }
                )
            },
            bottomBar = {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Beranda") }
                    )
                    ShortNavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        label = { Text("Kasir") }
                    )
                    ShortNavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        label = { Text("Produk") }
                    )
                    ShortNavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                        label = { Text("Lainnya") }
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0 || tab == 2) {
                    FloatingActionButton(
                        onClick = {
                            if (tab == 0) tab = 1 else notify("Fitur tambah produk segera hadir")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
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
                    0 -> BerandaTab(
                        onNewTransaction = { tab = 1 },
                        onAddProduct = { tab = 2 }
                    )
                    1 -> KasirTab(notify)
                    2 -> ProdukTab(notify)
                    else -> LainnyaTab(
                        dark = dark,
                        onDarkChange = { checked ->
                            darkOverride = checked
                            prefs.edit().putBoolean("dark_theme", checked).apply()
                        },
                        notify = notify
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BerandaTab(onNewTransaction: () -> Unit, onAddProduct: () -> Unit) {
    val periods = listOf("Hari Ini", "Minggu Ini", "Bulan Ini")
    var period by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Penjualan " + periods[period],
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Rp 0",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "0 transaksi - belum ada penjualan",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                LinearWavyProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            periods.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = period == index,
                    onClick = { period = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                    label = { Text(label) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onNewTransaction, modifier = Modifier.weight(1f)) {
                Text("Transaksi Baru")
            }
            FilledTonalButton(onClick = onAddProduct, modifier = Modifier.weight(1f)) {
                Text("Tambah Produk")
            }
        }
    }
}

@Composable
private fun KasirTab(notify: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Keranjang masih kosong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Mulai transaksi baru untuk menambahkan produk ke keranjang.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { notify("Fitur kasir segera hadir") },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Transaksi Baru", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProdukTab(notify: (String) -> Unit) {
    val filters = listOf("Semua", "Tersedia", "Habis")
    var filter by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { index, label ->
                FilterChip(
                    selected = filter == index,
                    onClick = { filter = index },
                    label = { Text(label) }
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Belum ada produk",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Tambahkan produk pertama untuk mulai berjualan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            FilledTonalButton(
                onClick = { notify("Fitur tambah produk segera hadir") },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Tambah Produk")
            }
        }
    }
}

@Composable
private fun LainnyaTab(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    notify: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        ListItem(
            headlineContent = { Text("Pengaturan Toko") },
            supportingContent = { Text("Nama, alamat, dan info struk") },
            leadingContent = { Icon(Icons.Default.Storefront, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable { notify("Fitur pengaturan toko segera hadir") }
        )
        ListItem(
            headlineContent = { Text("Printer Struk") },
            supportingContent = { Text("Belum terhubung") },
            leadingContent = { Icon(Icons.Default.Print, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable { notify("Fitur printer segera hadir") }
        )
        ListItem(
            headlineContent = { Text("Mode Gelap") },
            supportingContent = { Text(if (dark) "Aktif" else "Nonaktif") },
            leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
            trailingContent = { Switch(checked = dark, onCheckedChange = onDarkChange) }
        )
        ListItem(
            headlineContent = { Text("Tentang Kasir") },
            supportingContent = { Text("Versi 1.0.0") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable { notify("Kasir versi 1.0.0") }
        )
    }
}
