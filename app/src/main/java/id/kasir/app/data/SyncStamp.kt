package id.kasir.app.data

import java.util.UUID

// Penanda sinkron (Mode Online): setiap tulisan lokal harus membawa
// remoteId (identitas awan), updatedAt (cap sunting terakhir) dan
// kotor = 1 (menunggu naik). Pola yang sama seperti KasBulanan dan
// Apotek Lestari, disesuaikan dengan lapisan data Kasir yang memanggil
// DAO langsung dari layar: sisipan baru memakai baruSync(), suntingan
// memakai sentuhSync(), dan hapus adalah query UPDATE kuburan di DAO.

fun nowSync(): Long = System.currentTimeMillis()

fun genRemoteId(awalan: String): String = awalan + UUID.randomUUID().toString()

fun UnitEntity.baruSync(): UnitEntity =
    copy(remoteId = genRemoteId("unit-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun UnitEntity.sentuhSync(): UnitEntity = copy(updatedAt = nowSync(), kotor = 1)

fun SnackEntity.baruSync(): SnackEntity =
    copy(remoteId = genRemoteId("snack-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun SnackEntity.sentuhSync(): SnackEntity = copy(updatedAt = nowSync(), kotor = 1)

fun SessionEntity.baruSync(): SessionEntity =
    copy(remoteId = genRemoteId("sesi-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun SessionEntity.sentuhSync(): SessionEntity = copy(updatedAt = nowSync(), kotor = 1)

fun SessionSnackEntity.baruSync(): SessionSnackEntity =
    copy(remoteId = genRemoteId("sesisnack-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun SessionSnackEntity.sentuhSync(): SessionSnackEntity = copy(updatedAt = nowSync(), kotor = 1)

fun SnackSaleEntity.baruSync(): SnackSaleEntity =
    copy(remoteId = genRemoteId("jualsnack-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun SnackSaleEntity.sentuhSync(): SnackSaleEntity = copy(updatedAt = nowSync(), kotor = 1)

fun ReservationEntity.baruSync(): ReservationEntity =
    copy(remoteId = genRemoteId("reservasi-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun ReservationEntity.sentuhSync(): ReservationEntity = copy(updatedAt = nowSync(), kotor = 1)

fun ExpenseEntity.baruSync(): ExpenseEntity =
    copy(remoteId = genRemoteId("pengeluaran-"), updatedAt = nowSync(), terhapus = 0, kotor = 1)

fun ExpenseEntity.sentuhSync(): ExpenseEntity = copy(updatedAt = nowSync(), kotor = 1)
