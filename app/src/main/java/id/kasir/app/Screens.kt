package id.kasir.app

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.kasir.app.data.KasirDao
import id.kasir.app.data.MemberEntity
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.SnackEntity
import id.kasir.app.data.SnackSaleEntity
import id.kasir.app.data.TopUpEntity
import id.kasir.app.data.UnitEntity
import kotlinx.coroutines.launch

// ---------- Struk ----------

fun receiptText(
    storeName: String,
    session: SessionEntity,
    snacks: List<SessionSnackEntity>
): String {
    val end = session.endEpoch ?: session.startEpoch
    val billed = billableMinutes(session, end)
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
    session.memberName?.let { sb.appendLine("Member  : $it") }
    sb.appendLine("Mulai   : " + formatDateTime(session.startEpoch))
    sb.appendLine("Selesai : " + formatDateTime(end))
    sb.appendLine("Mode    : " + modeLabel + " (ditagih " + formatMinutes(billed) + ")")
    sb.appendLine("------------------------------")
    sb.appendLine("Sewa    : " + formatRupiah(session.rentalCost))
    if (session.balanceMinutesUsed > 0) {
        val balanceValue = session.hourlyRate * session.balanceMinutesUsed / 60
        sb.appendLine(
            "Saldo waktu " + formatMinutes(session.balanceMinutesUsed) +
                ": -" + formatRupiah(balanceValue)
        )
    }
    if (session.discountAmount > 0) {
        sb.appendLine("Diskon member: -" + formatRupiah(session.discountAmount))
    }
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

@Composable
fun ReceiptDialog(dao: KasirDao, session: SessionEntity, onDismiss: () -> Unit) {
    val snacks by dao.sessionSnacksFlow(session.id).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val storeName = prefs.getString("store_name", "Rental PS") ?: "Rental PS"
    val text = receiptText(storeName, session, snacks)
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
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan struk"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("Bagikan", modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

// ---------- Tab Jajan ----------

@Composable
fun JajanTab(dao: KasirDao, notify: (String) -> Unit) {
    val snacks by dao.snacksFlow().collectAsState(initial = emptyList())
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SnackEntity?>(null) }
    var selling by remember { mutableStateOf<SnackEntity?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Menu jajan & stok",
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
                "Menu masih kosong. Tambahkan jajan pertama lewat tombol Tambah.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snacks, key = { it.id }) { snack ->
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
                                snack.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatRupiah(snack.price) + " - stok " + snack.stock,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            editing = snack
                            showForm = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                        FilledTonalButton(
                            enabled = snack.stock > 0,
                            onClick = { selling = snack }
                        ) { Text("Jual") }
                    }
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
    selling?.let { snack ->
        SellSnackDialog(
            snack = snack,
            dao = dao,
            notify = notify,
            onDismiss = { selling = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellSnackDialog(
    snack: SnackEntity,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember { mutableIntStateOf(1) }
    var method by remember { mutableStateOf(PAY_TUNAI) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jual - " + snack.name) },
        text = {
            Column {
                Text(
                    formatRupiah(snack.price) + " - stok " + snack.stock,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("Jumlah", modifier = Modifier.weight(1f))
                    IconButton(
                        enabled = qty > 1,
                        onClick = { qty -= 1 }
                    ) { Icon(Icons.Default.Remove, contentDescription = null) }
                    Text(qty.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        enabled = qty < snack.stock,
                        onClick = { qty += 1 }
                    ) { Icon(Icons.Default.Add, contentDescription = null) }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
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
                Text(
                    "Total: " + formatRupiah(snack.price * qty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    dao.insertSnackSale(
                        SnackSaleEntity(
                            snackId = snack.id,
                            snackName = snack.name,
                            price = snack.price,
                            qty = qty,
                            total = snack.price * qty,
                            epoch = System.currentTimeMillis(),
                            paymentMethod = method
                        )
                    )
                    dao.decreaseStock(snack.id, qty)
                    notify("Terjual: " + snack.name + " x" + qty)
                    onDismiss()
                }
            }) { Text("Bayar") }
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
        title = { Text(if (initial == null) "Tambah Jajan" else "Edit Jajan") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama jajan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { c -> c.isDigit() } },
                    label = { Text("Harga (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { v -> stockText = v.filter { c -> c.isDigit() } },
                    label = { Text("Stok") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && price > 0,
                onClick = {
                    scope.launch {
                        if (initial == null) {
                            dao.insertSnack(SnackEntity(name = name.trim(), price = price, stock = stock))
                            notify("Jajan ditambahkan")
                        } else {
                            dao.updateSnack(initial.copy(name = name.trim(), price = price, stock = stock))
                            notify("Jajan diperbarui")
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

// ---------- Tab Member ----------

@Composable
fun MemberTab(dao: KasirDao, notify: (String) -> Unit) {
    val members by dao.membersFlow().collectAsState(initial = emptyList())
    var showAddForm by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daftar member",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { showAddForm = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Tambah", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (members.isEmpty()) {
            Text(
                "Belum ada member. Member dapat diskon sewa dan bisa menabung saldo waktu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(members, key = { it.id }) { member ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = member.id }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (member.phone.isNotBlank()) {
                            Text(
                                member.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Diskon " + member.discountPercent + "% - Saldo waktu: " +
                                formatMinutes(member.timeBalanceMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
    if (showAddForm) {
        MemberFormDialog(
            initial = null,
            dao = dao,
            notify = notify,
            onDismiss = { showAddForm = false }
        )
    }
    selectedId?.let { mid ->
        val member = members.firstOrNull { it.id == mid }
        if (member != null) {
            MemberDetailDialog(
                member = member,
                dao = dao,
                notify = notify,
                onDismiss = { selectedId = null }
            )
        }
    }
}

@Composable
private fun MemberFormDialog(
    initial: MemberEntity?,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var discountText by remember { mutableStateOf(initial?.discountPercent?.toString() ?: "10") }
    val scope = rememberCoroutineScope()
    val discount = (discountText.toIntOrNull() ?: 0).coerceIn(0, 100)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Member" else "Edit Member") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama member") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { v -> discountText = v.filter { c -> c.isDigit() } },
                    label = { Text("Diskon sewa (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (initial == null) {
                            dao.insertMember(
                                MemberEntity(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    discountPercent = discount
                                )
                            )
                            notify("Member ditambahkan")
                        } else {
                            dao.updateMember(
                                initial.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    discountPercent = discount
                                )
                            )
                            notify("Member diperbarui")
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

@Composable
private fun MemberDetailDialog(
    member: MemberEntity,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var showTopUp by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.name) },
        text = {
            Column {
                if (member.phone.isNotBlank()) {
                    Text("HP: " + member.phone, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Diskon sewa: " + member.discountPercent + "%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Saldo waktu: " + formatMinutes(member.timeBalanceMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = { showTopUp = true }) { Text("Isi Saldo") }
                    FilledTonalButton(onClick = { showEdit = true }) { Text("Edit") }
                }
                if (confirmDelete) {
                    Text(
                        "Yakin hapus member ini? Saldo waktu yang tersisa ikut hilang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row(modifier = Modifier.padding(top = 6.dp)) {
                        TextButton(onClick = {
                            scope.launch {
                                dao.deleteMember(member)
                                notify("Member dihapus")
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
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Hapus member", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
    if (showEdit) {
        MemberFormDialog(
            initial = member,
            dao = dao,
            notify = notify,
            onDismiss = { showEdit = false }
        )
    }
    if (showTopUp) {
        TopUpDialog(
            member = member,
            dao = dao,
            notify = notify,
            onDismiss = { showTopUp = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopUpDialog(
    member: MemberEntity,
    dao: KasirDao,
    notify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var minutesText by remember { mutableStateOf("60") }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PAY_TUNAI) }
    val scope = rememberCoroutineScope()
    val minutes = minutesText.toIntOrNull() ?: 0
    val amount = amountText.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Isi Saldo Waktu") },
        text = {
            Column {
                Text(
                    member.name + " - saldo sekarang " + formatMinutes(member.timeBalanceMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { v -> minutesText = v.filter { c -> c.isDigit() } },
                    label = { Text("Tambah waktu (menit)") },
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
                    listOf(60, 120, 300).forEach { m ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutesText = m.toString() },
                            label = { Text(formatMinutes(m)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v -> amountText = v.filter { c -> c.isDigit() } },
                    label = { Text("Harga bayar (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
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
            }
        },
        confirmButton = {
            Button(
                enabled = minutes > 0 && amount > 0,
                onClick = {
                    scope.launch {
                        dao.insertTopUp(
                            TopUpEntity(
                                memberId = member.id,
                                memberName = member.name,
                                minutes = minutes,
                                amount = amount,
                                epoch = System.currentTimeMillis(),
                                paymentMethod = method
                            )
                        )
                        dao.addBalance(member.id, minutes)
                        notify("Saldo " + member.name + " bertambah " + formatMinutes(minutes))
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

// ---------- Tab Laporan ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanTab(dao: KasirDao, notify: (String) -> Unit) {
    val finished by dao.finishedSessionsFlow().collectAsState(initial = emptyList())
    val sales by dao.snackSalesFlow().collectAsState(initial = emptyList())
    val topUps by dao.topUpsFlow().collectAsState(initial = emptyList())
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
    val periodTopUps = topUps.filter { it.epoch >= from }
    val sessionTotal = periodSessions.sumOf { it.total }
    val salesTotal = periodSales.sumOf { it.total }
    val topUpTotal = periodTopUps.sumOf { it.amount }
    val grandTotal = sessionTotal + salesTotal + topUpTotal
    val byUnit = periodSessions.groupBy { it.unitName }
        .map { (name, list) ->
            Triple(
                name,
                list.size,
                list.sumOf { elapsedMinutes(it, it.endEpoch ?: nowMs) }
            ) to list.sumOf { it.total }
        }
        .sortedByDescending { it.first.second }

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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Pendapatan", style = MaterialTheme.typography.labelLarge)
                Text(
                    formatRupiah(grandTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "Sewa & sesi: " + formatRupiah(sessionTotal) + " (" +
                        periodSessions.size + " sesi)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    "Jajan langsung: " + formatRupiah(salesTotal) + " (" +
                        periodSales.size + " penjualan)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "Top up saldo: " + formatRupiah(topUpTotal) + " (" +
                        periodTopUps.size + " kali)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (byUnit.isNotEmpty()) {
                item {
                    Text(
                        "Unit Terlaris",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                                formatDateTime(session.endEpoch ?: session.startEpoch) +
                                    (session.memberName?.let { " - $it" } ?: ""),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        ListItem(
            headlineContent = { Text("Atur Unit & Tarif") },
            supportingContent = { Text("Tambah, ubah, dan hapus unit beserta tarif per jam") },
            leadingContent = { Icon(Icons.Default.Tv, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable { onOpenUnitManage() }
        )
        ListItem(
            headlineContent = { Text("Nama Toko") },
            supportingContent = { Text(storeName) },
            leadingContent = { Icon(Icons.Default.Storefront, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable {
                storeNameText = storeName
                showStoreDialog = true
            }
        )
        ListItem(
            headlineContent = { Text("Mode Gelap") },
            supportingContent = { Text(if (dark) "Aktif" else "Nonaktif") },
            leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
            trailingContent = { Switch(checked = dark, onCheckedChange = onDarkChange) }
        )
        ListItem(
            headlineContent = { Text("Tentang Kasir") },
            supportingContent = { Text("Versi 1.0.0 - kasir rental PlayStation") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable { showAbout = true }
        )
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
                        "Fitur: billing per unit dengan mode Paket dan Open Billing, " +
                        "timer akurat dari waktu mulai, jajan menyatu di tagihan sesi, " +
                        "member dengan diskon dan saldo waktu, laporan pendapatan, " +
                        "serta struk yang bisa dibagikan. Semua data tersimpan lokal di HP."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Tutup") }
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
                                unit.consoleType + " - " + formatRupiah(unit.hourlyRate) + " / jam" +
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
    var rateText by remember { mutableStateOf(initial?.hourlyRate?.toString() ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rate = rateText.toLongOrNull() ?: 0L
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
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CONSOLE_TYPES.forEach { type ->
                        FilterChip(
                            selected = consoleType == type,
                            onClick = { consoleType = type },
                            label = { Text(type) }
                        )
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { v -> rateText = v.filter { c -> c.isDigit() } },
                    label = { Text("Tarif per jam (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
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
