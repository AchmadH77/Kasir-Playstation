package id.kasir.app

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.kasir.app.data.KasirDao
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.SnackEntity
import id.kasir.app.data.SnackSaleEntity
import id.kasir.app.data.BackupData
import id.kasir.app.data.DatabaseProvider
import id.kasir.app.data.UnitEntity
import id.kasir.app.data.applyBackup
import id.kasir.app.data.backupFileName
import id.kasir.app.data.backupToJson
import id.kasir.app.data.collectBackup
import id.kasir.app.data.parseBackup
import java.util.Calendar
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.launch

// ---------- Struk ----------

fun receiptText(
    storeName: String,
    session: SessionEntity,
    snacks: List<SessionSnackEntity>
): String {
    val end = session.endEpoch ?: session.startEpoch
    val billed = billedMinutes(session, end)
    val modeLabel = if (session.mode == MODE_PAKET) {
        "Paket " + formatMinutes(session.paketMinutes)
    } else {
        "Open billing"
    }
    val sb = StringBuilder()
    sb.appendLine(storeName)
    sb.appendLine("Struk Rental PS")
    sb.appendLine("------------------------------")
    sb.appendLine("Unit    : " + session.unitName + " (" + session.consoleType + ")")
    sb.appendLine("Mulai   : " + formatDateTime(session.startEpoch))
    sb.appendLine("Selesai : " + formatDateTime(end))
    sb.appendLine("Mode    : " + modeLabel + " (ditagih " + formatMinutes(billed) + ")")
    sb.appendLine("------------------------------")
    sb.appendLine("Sewa    : " + formatRupiah(session.rentalCost))
    snacks.forEach {
        sb.appendLine(it.snackName + " x" + it.qty + " : " + formatRupiah(it.price * it.qty))
    }
    sb.appendLine("------------------------------")
    sb.appendLine("TOTAL   : " + formatRupiah(session.total))
    sb.appendLine("Bayar   : " + (session.paymentMethod ?: "-") + " " + formatRupiah(session.paidAmount))
    if (session.changeAmount > 0) {
        sb.appendLine("Kembali : " + formatRupiah(session.changeAmount))
    }
    sb.appendLine("------------------------------")
    sb.appendLine("Terima kasih sudah bermain!")
    return sb.toString()
}

// Petak info kecil untuk struk S6: label mungil di atas, nilai tebal.
@Composable
private fun ReceiptInfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ReceiptDialog(dao: KasirDao, session: SessionEntity, onDismiss: () -> Unit) {
    val snacks by dao.sessionSnacksFlow(session.id).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val storeName = prefs.getString("store_name", "Rental PS") ?: "Rental PS"
    val text = receiptText(storeName, session, snacks)
    val end = session.endEpoch ?: session.startEpoch
    val modeLabel = if (session.mode == MODE_PAKET) {
        "Paket " + formatMinutes(session.paketMinutes)
    } else {
        "Open billing"
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    "Struk Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    storeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // S6 - Grid Ringkas: empat info utama jadi petak 2x2, total
                // dalam pita hijau tua, tombol bertumpuk. Teks bagikan tetap
                // dari receiptText seperti sebelumnya.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptInfoTile(
                        "UNIT",
                        session.unitName + " (" + session.consoleType + ")",
                        Modifier.weight(1f)
                    )
                    ReceiptInfoTile("MODE", modeLabel, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptInfoTile(
                        "MULAI",
                        formatDateTime(session.startEpoch),
                        Modifier.weight(1f)
                    )
                    ReceiptInfoTile("SELESAI", formatDateTime(end), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        "Sewa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        formatRupiah(session.rentalCost),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                snacks.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            item.snackName + " x" + item.qty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(item.price * item.qty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF123D26),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(session.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bayar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        formatRupiah(session.paidAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            session.paymentMethod ?: "-",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (session.changeAmount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            "Kembali",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(session.changeAmount),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text(
                    "Terima kasih sudah bermain!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) { Text("Tutup") }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan struk"))
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Bagikan Struk", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

// Snapshot penjualan makanan langsung yang baru dibayar, untuk struk.
private data class SnackSaleSnapshot(
    val items: List<Pair<SnackEntity, Int>>,
    val total: Long,
    val method: String,
    val paid: Long,
    val epoch: Long
)

fun snackReceiptText(
    storeName: String,
    snapshot: SnackSaleSnapshot
): String {
    val sb = StringBuilder()
    sb.appendLine(storeName)
    sb.appendLine("Struk Makanan")
    sb.appendLine("------------------------------")
    sb.appendLine("Waktu   : " + formatDateTime(snapshot.epoch))
    sb.appendLine("------------------------------")
    snapshot.items.forEach { (snack, qty) ->
        sb.appendLine(snack.name + " x" + qty + " : " + formatRupiah(snack.price * qty))
    }
    sb.appendLine("------------------------------")
    sb.appendLine("TOTAL   : " + formatRupiah(snapshot.total))
    sb.appendLine("Bayar   : " + snapshot.method + " " + formatRupiah(snapshot.paid))
    if (snapshot.paid > snapshot.total) {
        sb.appendLine("Kembali : " + formatRupiah(snapshot.paid - snapshot.total))
    }
    sb.appendLine("------------------------------")
    sb.appendLine("Terima kasih sudah jajan!")
    return sb.toString()
}

// Struk untuk penjualan makanan langsung, mengikuti gaya struk sesi S6:
// petak info, baris item, pita total hijau tua, baris bayar + metode.
@Composable
private fun SnackReceiptDialog(
    snapshot: SnackSaleSnapshot,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val storeName = prefs.getString("store_name", "Rental PS") ?: "Rental PS"
    val text = snackReceiptText(storeName, snapshot)
    val change = snapshot.paid - snapshot.total
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    "Struk Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    storeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptInfoTile(
                        "WAKTU",
                        formatDateTime(snapshot.epoch),
                        Modifier.weight(1f)
                    )
                    ReceiptInfoTile("METODE", snapshot.method, Modifier.weight(1f))
                }
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    snapshot.items.forEach { (snack, qty) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                snack.name + " x" + qty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                formatRupiah(snack.price * qty),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF123D26),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(snapshot.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bayar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        formatRupiah(snapshot.paid),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            snapshot.method,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (change > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            "Kembali",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(change),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text(
                    "Terima kasih sudah jajan!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) { Text("Tutup") }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan struk"))
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Bagikan Struk", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

// Menulis berkas teks ke cache lalu membukanya di lembar bagikan Android:
// dipakai backup JSON (tab Lainnya) dan ekspor laporan CSV (tab Laporan).
private fun shareTextFile(
    context: Context,
    fileName: String,
    content: String,
    mimeType: String,
    chooserTitle: String
) {
    val file = File(context.cacheDir, fileName)
    file.writeText(content)
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun csvCell(value: String): String =
    if (value.any { it == ';' || it == '"' || it == '\n' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

// Laporan satu periode sebagai CSV (pemisah ";", angka mentah) supaya
// langsung rapi dibuka di Excel/Sheets dan gampang dibagikan.
fun laporanCsv(
    storeName: String,
    periodLabel: String,
    generatedAtMillis: Long,
    grandTotal: Long,
    sessionTotal: Long,
    salesTotal: Long,
    byUnit: List<Pair<Triple<String, Int, Int>, Long>>,
    sessions: List<SessionEntity>,
    sales: List<SnackSaleEntity>
): String {
    val sb = StringBuilder()
    sb.appendLine(csvCell(storeName))
    sb.appendLine("Laporan Kasir - " + periodLabel)
    sb.appendLine("Dibuat;" + formatDateTime(generatedAtMillis))
    sb.appendLine()
    sb.appendLine("RINGKASAN")
    sb.appendLine("Total Pendapatan;" + grandTotal)
    sb.appendLine("Sewa & Sesi;" + sessionTotal)
    sb.appendLine("Makanan Langsung;" + salesTotal)
    sb.appendLine()
    sb.appendLine("PER UNIT")
    sb.appendLine("Unit;Sesi;Durasi (menit);Pendapatan")
    byUnit.forEach { (info, revenue) ->
        sb.appendLine(
            csvCell(info.first) + ";" + info.second + ";" + info.third + ";" + revenue
        )
    }
    sb.appendLine()
    sb.appendLine("RIWAYAT SESI")
    sb.appendLine("Tanggal Selesai;Unit;Konsol;Mode;Durasi (menit);Sewa;Makanan di Sesi;Total;Metode")
    sessions.forEach { session ->
        val end = session.endEpoch ?: session.startEpoch
        val modeLabel = if (session.mode == MODE_PAKET) {
            "Paket " + formatMinutes(session.paketMinutes)
        } else {
            "Open"
        }
        sb.appendLine(
            formatDateTime(end) + ";" + csvCell(session.unitName) + ";" +
                csvCell(session.consoleType) + ";" + modeLabel + ";" +
                billedMinutes(session, end) + ";" + session.rentalCost + ";" +
                session.snackCost + ";" + session.total + ";" +
                (session.paymentMethod ?: "-")
        )
    }
    sb.appendLine()
    sb.appendLine("PENJUALAN MAKANAN LANGSUNG")
    sb.appendLine("Waktu;Item;Qty;Harga Satuan;Total;Metode")
    sales.forEach { sale ->
        sb.appendLine(
            formatDateTime(sale.epoch) + ";" + csvCell(sale.snackName) + ";" +
                sale.qty + ";" + sale.price + ";" + sale.total + ";" +
                sale.paymentMethod
        )
    }
    return sb.toString()
}

// ---------- Tab Jajan ----------

// Warna petak "foto" menu: dipilih stabil dari nama menu, supaya tiap menu
// selalu tampil dengan warna yang sama (gaya varian 1 dan 2).
private val SNACK_TILE_COLORS = listOf(
    Color(0xFFE8833A), Color(0xFF6F4E37), Color(0xFFC98A2D), Color(0xFF4DA3FF),
    Color(0xFF8B7CFF), Color(0xFF7ED957), Color(0xFFFF5C8A), Color(0xFF2DD4BF)
)

private fun snackTileColor(name: String): Color =
    SNACK_TILE_COLORS[name.hashCode().mod(SNACK_TILE_COLORS.size)]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JajanTab(dao: KasirDao, notify: (String) -> Unit) {
    val snacks by dao.snacksFlow().collectAsState(initial = emptyList())
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SnackEntity?>(null) }
    var quantities by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var showBatchSell by remember { mutableStateOf(false) }

    fun setQty(snack: SnackEntity, qty: Int) {
        val q = qty.coerceIn(0, snack.stock)
        quantities = if (q == 0) quantities - snack.id else quantities + (snack.id to q)
    }

    val selected = snacks.filter { (quantities[it.id] ?: 0) > 0 }
    val totalQty = selected.sumOf { minOf(quantities[it.id] ?: 0, it.stock) }
    val totalPrice = selected.sumOf { it.price * minOf(quantities[it.id] ?: 0, it.stock) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Menu makanan & stok",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                editing = null
                showForm = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Tambah", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (snacks.isEmpty()) {
            Text(
                "Menu masih kosong. Tambahkan makanan pertama lewat tombol Tambah.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snacks, key = { it.id }) { snack ->
                val qty = minOf(quantities[snack.id] ?: 0, snack.stock)
                val habis = snack.stock == 0
                Card(
                    onClick = {
                        editing = snack
                        showForm = true
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (habis) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    } else {
                                        snackTileColor(snack.name)
                                    },
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                snack.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (habis) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    Color.White
                                }
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                snack.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (habis) {
                                    formatRupiah(snack.price) + " - Habis"
                                } else {
                                    formatRupiah(snack.price) + " - Stok " + snack.stock
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            enabled = qty > 0,
                            onClick = { setQty(snack, qty - 1) }
                        ) { Icon(Icons.Default.Remove, contentDescription = null) }
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                qty.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        IconButton(
                            enabled = qty < snack.stock,
                            onClick = { setQty(snack, qty + 1) }
                        ) { Icon(Icons.Default.Add, contentDescription = null) }
                    }
                }
            }
        }
        if (totalQty > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$totalQty item dipilih",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            formatRupiah(totalPrice),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = { quantities = emptyMap() }) { Text("Reset") }
                    Button(onClick = { showBatchSell = true }) { Text("Jual") }
                }
            }
        }
    }
    if (showForm) {
        SnackFormDialog(
            initial = editing,
            dao = dao,
            notify = notify,
            onDismiss = { showForm = false }
        )
    }
    if (showBatchSell) {
        BatchSellDialog(
            items = selected.map { it to minOf(quantities[it.id] ?: 0, it.stock) },
            dao = dao,
            notify = notify,
            onDismiss = { showBatchSell = false },
            onDone = {
                quantities = emptyMap()
                showBatchSell = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchSellDialog(
    items: List<Pair<SnackEntity, Int>>,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var method by remember { mutableStateOf(PAY_TUNAI) }
    var paidText by remember { mutableStateOf("") }
    var sold by remember { mutableStateOf<SnackSaleSnapshot?>(null) }
    val scope = rememberCoroutineScope()
    val total = items.sumOf { it.first.price * it.second }
    val paid = paidText.toLongOrNull() ?: 0L
    val canPay = method == PAY_QRIS || paid >= total
    val soldSnapshot = sold
    if (soldSnapshot != null) {
        // Pembayaran berhasil: tampilkan struk dulu; pilihan jualan
        // dibersihkan (onDone) setelah struk ditutup.
        SnackReceiptDialog(
            snapshot = soldSnapshot,
            onDismiss = {
                sold = null
                onDone()
            }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jual Makanan") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // B2: baris item memakai petak inisial berwarna yang sama
                // dengan daftar jajan (warna stabil dari nama menu).
                items.forEach { (snack, qty) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    snackTileColor(snack.name),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                snack.name.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            snack.name + " x" + qty,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(snack.price * qty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF123D26),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
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
                        val nowMs = System.currentTimeMillis()
                        items.forEach { (snack, qty) ->
                            dao.insertSnackSale(
                                SnackSaleEntity(
                                    snackId = snack.id,
                                    snackName = snack.name,
                                    price = snack.price,
                                    qty = qty,
                                    total = snack.price * qty,
                                    epoch = nowMs,
                                    paymentMethod = method
                                )
                            )
                            dao.decreaseStock(snack.id, qty)
                        }
                        sold = SnackSaleSnapshot(
                            items = items,
                            total = total,
                            method = method,
                            paid = if (method == PAY_QRIS) total else paid,
                            epoch = nowMs
                        )
                    }
                }
            ) { Text("Bayar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun SnackFormDialog(
    initial: SnackEntity?,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val price = priceText.toLongOrNull() ?: 0L
    val stock = stockText.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Makanan" else "Edit Makanan") },
        text = {
            Column {
                // J1 - Ikon + Isian Penuh: kolom isian penuh dengan chip ikon
                // berwarna; Simpan selebar dialog, Batal teks di bawahnya.
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama makanan") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF8B7CFF), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = groupThousands(priceText),
                    onValueChange = { v -> priceText = v.filter { c -> c.isDigit() } },
                    label = { Text("Harga (Rp)") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(9.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                TextField(
                    value = stockText,
                    onValueChange = { v -> stockText = v.filter { c -> c.isDigit() } },
                    label = { Text("Stok") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFE8833A), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                if (initial != null) {
                    if (confirmDelete) {
                        Text(
                            "Yakin hapus menu ini? Riwayat penjualan lama tidak ikut terhapus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Row(modifier = Modifier.padding(top = 6.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    dao.deleteSnack(initial)
                                    notify("Menu dihapus")
                                    onDismiss()
                                }
                            }) {
                                Text("Ya, Hapus", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
                        }
                    } else {
                        TextButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(
                                "Hapus menu",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
                Button(
                    enabled = name.isNotBlank() && price > 0,
                    onClick = {
                        scope.launch {
                            if (initial == null) {
                                dao.insertSnack(
                                    SnackEntity(name = name.trim(), price = price, stock = stock)
                                )
                                notify("Makanan ditambahkan")
                            } else {
                                dao.updateSnack(
                                    initial.copy(name = name.trim(), price = price, stock = stock)
                                )
                                notify("Makanan diperbarui")
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) { Text("Simpan") }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Batal") }
            }
        },
        confirmButton = {}
    )
}

// ---------- Tab Laporan ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanTab(dao: KasirDao, notify: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val storeName = prefs.getString("store_name", "Rental PS") ?: "Rental PS"
    val finished by dao.finishedSessionsFlow().collectAsState(initial = emptyList())
    val sales by dao.snackSalesFlow().collectAsState(initial = emptyList())
    var period by remember { mutableIntStateOf(0) }
    var receiptSession by remember { mutableStateOf<SessionEntity?>(null) }
    val nowMs = System.currentTimeMillis()
    val from = when (period) {
        0 -> startOfDay(nowMs)
        1 -> startOfWeek(nowMs)
        2 -> startOfMonth(nowMs)
        else -> 0L
    }
    val periodSessions = finished.filter { (it.endEpoch ?: 0L) >= from }
    val periodSales = sales.filter { it.epoch >= from }
    val sessionTotal = periodSessions.sumOf { it.total }
    val salesTotal = periodSales.sumOf { it.total }
    val grandTotal = sessionTotal + salesTotal
    val byUnit = periodSessions.groupBy { it.unitName }
        .map { (name, list) ->
            Triple(
                name,
                list.size,
                list.sumOf { elapsedMinutes(it, it.endEpoch ?: nowMs) }
            ) to list.sumOf { it.total }
        }
        .sortedByDescending { it.first.second }

    // Grafik 7 hari terakhir (selalu 7 hari, tidak mengikuti pilihan periode):
    // total sesi selesai + penjualan jajan langsung per hari.
    val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    val last7 = (6 downTo 0).map { back ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(nowMs)
        cal.add(Calendar.DAY_OF_YEAR, -back)
        val dayStart = cal.timeInMillis
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis
        val totalDay = finished
            .filter { (it.endEpoch ?: 0L) in dayStart until dayEnd }
            .sumOf { it.total } + sales
            .filter { it.epoch in dayStart until dayEnd }
            .sumOf { it.total }
        dayNames[dow - 1] to totalDay
    }
    val maxDay = last7.maxOf { it.second }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Semua").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = period == index,
                    onClick = { period = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                    label = { Text(label) }
                )
            }
        }
        // L5 - Kartu Berdampingan: sewa (hijau) dan jajan (oranye) jadi dua
        // kartu besar; total pendapatan jadi baris ringkasan di bawahnya.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Sewa & sesi", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatRupiah(sessionTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        periodSessions.size.toString() + " sesi selesai",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A2C14),
                    contentColor = Color(0xFFFFD9B0)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Makanan langsung", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatRupiah(salesTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        periodSales.size.toString() + " penjualan",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Pendapatan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    formatRupiah(grandTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        OutlinedButton(
            onClick = {
                val label = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Semua")[period]
                shareTextFile(
                    context = context,
                    fileName = "laporan-kasir-" + label.lowercase().replace(" ", "-") + ".csv",
                    content = laporanCsv(
                        storeName = storeName,
                        periodLabel = label,
                        generatedAtMillis = System.currentTimeMillis(),
                        grandTotal = grandTotal,
                        sessionTotal = sessionTotal,
                        salesTotal = salesTotal,
                        byUnit = byUnit,
                        sessions = periodSessions,
                        sales = periodSales
                    ),
                    mimeType = "text/csv",
                    chooserTitle = "Bagikan Laporan"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ekspor Laporan")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Pendapatan 7 Hari Terakhir",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        last7.forEach { (dayName, totalDay) ->
                            val frac = if (maxDay > 0) {
                                totalDay.toFloat() / maxDay.toFloat()
                            } else {
                                0f
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    formatRupiahShort(totalDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .padding(top = 4.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.62f)
                                            .height((90f * frac).coerceAtLeast(4f).dp)
                                            .background(
                                                if (totalDay == maxDay && maxDay > 0) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                    )
                                }
                                Text(
                                    dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Komposisi Pendapatan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                CompositionRow(
                    label = "Sewa & sesi",
                    value = sessionTotal,
                    grandTotal = grandTotal,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                CompositionRow(
                    label = "Makanan langsung",
                    value = salesTotal,
                    grandTotal = grandTotal,
                    color = Color(0xFFE8833A)
                )
            }
            if (byUnit.isNotEmpty()) {
                item {
                    Text(
                        "Unit Terlaris",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(byUnit) { (info, revenue) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            info.first + " - " + info.second + " sesi - " + formatMinutes(info.third),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(formatRupiah(revenue), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Text(
                    "Riwayat Sesi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (periodSessions.isEmpty()) {
                item {
                    Text(
                        "Belum ada sesi selesai pada periode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(periodSessions, key = { it.id }) { session ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { receiptSession = session }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                session.unitName + " (" + session.consoleType + ")",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatDateTime(session.endEpoch ?: session.startEpoch),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatRupiah(session.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                session.paymentMethod ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    receiptSession?.let { s ->
        ReceiptDialog(dao = dao, session = s, onDismiss = { receiptSession = null })
    }
}

// Satu baris komposisi pendapatan: titik warna, label, nilai + persen, dan bar proporsi.
@Composable
private fun CompositionRow(label: String, value: Long, grandTotal: Long, color: Color) {
    val pct = if (grandTotal > 0) (value * 100 / grandTotal).toInt() else 0
    val frac = if (grandTotal > 0) value.toFloat() / grandTotal.toFloat() else 0f
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            Text(
                formatRupiah(value) + " (" + pct + "%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

// ---------- Tab Lainnya ----------

@Composable
fun LainnyaTab(
    dao: KasirDao,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    notify: (String) -> Unit,
    onOpenUnitManage: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    var storeName by remember { mutableStateOf(prefs.getString("store_name", "Rental PS") ?: "Rental PS") }
    var storeNameText by remember { mutableStateOf("") }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val units by dao.unitsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val parsed = try {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText()
                    if (text == null) null else parseBackup(text)
                } catch (e: Exception) {
                    null
                }
                if (parsed != null) {
                    pendingRestore = parsed
                } else {
                    notify("Berkas backup tidak valid")
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header toko: identitas yang tampil di struk.
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            storeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Kasir Rental PlayStation",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            storeNameText = storeName
                            showStoreDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Ubah Nama") }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(top = 16.dp)) {
            ListItem(
                headlineContent = { Text("Atur Unit & Tarif") },
                leadingContent = {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            units.size.toString() + " unit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable { onOpenUnitManage() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ListItem(
                headlineContent = { Text("Backup Data") },
                supportingContent = { Text("Simpan semua data ke satu berkas") },
                leadingContent = {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        val data = collectBackup(dao, System.currentTimeMillis())
                        shareTextFile(
                            context = context,
                            fileName = backupFileName(data.exportedAtMillis),
                            content = backupToJson(data),
                            mimeType = "application/json",
                            chooserTitle = "Bagikan Backup Kasir"
                        )
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ListItem(
                headlineContent = { Text("Restore Data") },
                supportingContent = { Text("Pulihkan data dari berkas backup") },
                leadingContent = {
                    Icon(
                        Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    restoreLauncher.launch(arrayOf("application/json"))
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ListItem(
                headlineContent = { Text("Mode Gelap") },
                leadingContent = {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = { Switch(checked = dark, onCheckedChange = onDarkChange) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ListItem(
                headlineContent = { Text("Tentang Kasir") },
                leadingContent = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Versi 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable { showAbout = true }
            )
        }
    }
    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            title = { Text("Nama Toko") },
            text = {
                OutlinedTextField(
                    value = storeNameText,
                    onValueChange = { storeNameText = it },
                    label = { Text("Nama toko di struk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = storeNameText.isNotBlank(),
                    onClick = {
                        storeName = storeNameText.trim()
                        prefs.edit().putString("store_name", storeName).apply()
                        notify("Nama toko disimpan")
                        showStoreDialog = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showStoreDialog = false }) { Text("Batal") }
            }
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Tentang Kasir") },
            text = {
                Text(
                    "Kasir rental PlayStation versi 1.0.0.\n\n" +
                        "Fitur: billing per unit dengan paket berharga manual, " +
                        "timer akurat dari waktu mulai, makanan menyatu di tagihan sesi, " +
                        "laporan pendapatan, " +
                        "serta struk yang bisa dibagikan. Semua data tersimpan lokal di HP."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Tutup") }
            }
        )
    }
    pendingRestore?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore Data?") },
            text = {
                Text(
                    "Backup dari " + formatDateTime(data.exportedAtMillis) + " berisi " +
                        data.units.size + " unit, " + data.sessions.size + " sesi, dan " +
                        data.snackSales.size + " penjualan makanan.\n\n" +
                        "Semua data yang sekarang ada akan diganti dengan isi backup ini."
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        applyBackup(DatabaseProvider.get(context), data)
                        pendingRestore = null
                        notify("Data berhasil dipulihkan")
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Batal") }
            }
        )
    }
}

// ---------- Kelola Unit & Tarif ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitManageScreen(dao: KasirDao, notify: (String) -> Unit, onBack: () -> Unit) {
    val units by dao.unitsFlow().collectAsState(initial = emptyList())
    val active by dao.activeSessionsFlow().collectAsState(initial = emptyList())
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UnitEntity?>(null) }
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atur Unit & Tarif", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showForm = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(units, key = { it.id }) { unit ->
                val inUse = active.any { it.unitId == unit.id }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                unit.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                unit.consoleType +
                                    if (inUse) " - sedang dipakai" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            editing = unit
                            showForm = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
    if (showForm) {
        UnitFormDialog(
            initial = editing,
            dao = dao,
            notify = notify,
            inUse = editing != null && active.any { it.unitId == editing?.id },
            onDismiss = { showForm = false }
        )
    }
}

@Composable
private fun UnitFormDialog(
    initial: UnitEntity?,
    dao: KasirDao,
    notify: (String) -> Unit,
    inUse: Boolean,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var consoleType by remember { mutableStateOf(initial?.consoleType ?: "PS4") }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Tarif tidak lagi diketik: otomatis dari jenis konsol yang dipilih.
    val rate = hourlyRateFor(consoleType)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Unit" else "Edit Unit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama unit (mis. TV 07)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Jenis konsol",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CONSOLE_TYPES.forEach { type ->
                        val typeSelected = consoleType == type
                        Card(
                            onClick = { consoleType = type },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (typeSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (typeSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    type,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (typeSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
                if (initial != null) {
                    if (inUse) {
                        Text(
                            "Unit sedang dipakai - tidak bisa dihapus sekarang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else if (confirmDelete) {
                        Text(
                            "Yakin hapus unit ini? Riwayat sesi lama tetap tersimpan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Row(modifier = Modifier.padding(top = 6.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    dao.deleteUnit(initial)
                                    notify("Unit dihapus")
                                    onDismiss()
                                }
                            }) {
                                Text("Ya, Hapus", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
                        }
                    } else {
                        TextButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(
                                "Hapus unit",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && rate > 0,
                onClick = {
                    scope.launch {
                        if (initial == null) {
                            dao.insertUnit(
                                UnitEntity(
                                    name = name.trim(),
                                    consoleType = consoleType,
                                    hourlyRate = rate
                                )
                            )
                            notify("Unit ditambahkan")
                        } else {
                            dao.updateUnit(
                                initial.copy(
                                    name = name.trim(),
                                    consoleType = consoleType,
                                    hourlyRate = rate
                                )
                            )
                            notify("Unit diperbarui")
                        }
                        onDismiss()
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
