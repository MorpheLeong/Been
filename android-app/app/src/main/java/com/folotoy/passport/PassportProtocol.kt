package com.folotoy.passport

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.zip.CRC32

internal object PassportProtocol {
    val service: UUID = UUID.fromString("01504941-6E8C-214A-10A9-415000000001")
    val command: UUID = UUID.fromString("02504941-6E8C-214A-10A9-415000000001")
    val result: UUID = UUID.fromString("03504941-6E8C-214A-10A9-415000000001")
    val snapshot: UUID = UUID.fromString("04504941-6E8C-214A-10A9-415000000001")
    const val VERSION = 3
    const val CHECK_IN = 2
    const val UNCHECK_IN = 3
    const val CHECK_IN_RESULT = 0x81
    const val SNAPSHOT_BEGIN = 0x10
    const val SNAPSHOT_CHUNK = 0x11
    const val SNAPSHOT_COMMIT = 0x12
    const val SCREEN_TIMEOUT = 0x13
    const val TIME_SYNC = 0x14
    const val CHECK_IN_FRAGMENT = 0x42

    data class CheckIn(val request: Int, val opcode: Int, val destinationId: String, val name: String)
    /** Independent setting command: never changes the existing v3 snapshot format. */
    fun screenTimeoutFrame(seconds: Int): ByteArray {
        require(seconds in 15..600)
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            .put(VERSION.toByte()).put(SCREEN_TIMEOUT.toByte()).putShort(seconds.toShort()).array()
    }
    fun timeSyncFrame(nowMillis: Long = System.currentTimeMillis(), offsetMinutes: Int =
        java.time.OffsetDateTime.now().offset.totalSeconds / 60): ByteArray {
        require(nowMillis >= 1_577_836_800_000L)
        require(offsetMinutes in -840..840)
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .put(VERSION.toByte()).put(TIME_SYNC.toByte())
            .putInt((nowMillis / 1000L).toInt()).putShort(offsetMinutes.toShort()).array()
    }
    fun parseCommand(bytes: ByteArray): CheckIn? = runCatching {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(b.remaining() >= 4)
        val version = b.get().toInt() and 0xff
        val opcode = b.get().toInt() and 0xff
        require(version == VERSION && (opcode == CHECK_IN || opcode == UNCHECK_IN || opcode == 1))
        val request = b.short.toInt() and 0xffff
        if (opcode == 1) return@runCatching CheckIn(request, opcode, "", "轨迹记录")
        require(b.remaining() >= 2)
        val idLen = b.get().toInt() and 0xff
        require(idLen in 1..36 && b.remaining() >= idLen + 1)
        val idBytes = ByteArray(idLen); b.get(idBytes)
        val id = idBytes.toString(Charsets.UTF_8)
        val nameLen = b.get().toInt() and 0xff
        require(nameLen in 1..48 && b.remaining() == nameLen)
        val nameBytes = ByteArray(nameLen); b.get(nameBytes)
        CheckIn(request, opcode, id, nameBytes.toString(Charsets.UTF_8))
    }.getOrNull()

    fun checkInResult(request: Int, success: Boolean, code: Int = 0): ByteArray {
        val b = ByteBuffer.allocate(if (success) 11 else 6).order(ByteOrder.LITTLE_ENDIAN)
            .put(VERSION.toByte()).put(CHECK_IN_RESULT.toByte()).put(if (success) 0 else 1)
            .putShort(request.toShort())
        if (success) b.putInt((System.currentTimeMillis() / 1000L).toInt()).putShort(java.time.OffsetDateTime.now().offset.totalSeconds.div(60).toShort())
        else b.put(code.toByte())
        return b.array()
    }

    /** Starred codes claim slots first; upcoming itinerary codes fill remaining slots. */
    fun snapshotPayload(db: PassportDb): ByteArray {
        val qrs = db.qrs(); val destinations = db.destinations().take(64)
        val starred = qrs.filter { it.starred }.take(3)
        val itineraryCodes = destinations.mapNotNull { d -> qrs.firstOrNull { it.id == d.qrId } }
        val active = itineraryCodes.filter { q -> destinations.firstOrNull { it.qrId == q.id }?.done == false }
        val completed = itineraryCodes.filter { q -> destinations.firstOrNull { it.qrId == q.id }?.done == true }
        val selected = (starred + active + completed)
            .distinctBy { it.id }.take(10)
        val out = ByteArrayOutputStream()
        val visibleDestinations = destinations
        out.write(visibleDestinations.size)
        visibleDestinations.forEach { destination ->
            writeString(out, destination.id, 36); writeString(out, destination.name, 48)
            out.write(if (destination.done) 1 else 0)
        }
        out.write(selected.size)
        selected.forEach { qr ->
            val linked = destinations.firstOrNull { it.qrId == qr.id }
            writeString(out, qr.id, 36); writeString(out, qr.name, 48); writeString(out, qr.payload, 192)
            out.write(if (qr.starred) 1 else 0)
            writeString(out, linked?.id.orEmpty(), 36)
        }
        val profile = db.profile()
        writeString(out, profile.nickname, 48)
        writeString(out, profile.bio, 96)
        val accounts = profile.socialIds.take(4)
        out.write(accounts.size)
        accounts.forEach { writeString(out, it, 48) }
        out.write(db.soundVolume())
        return out.toByteArray()
    }

    /** MTU-aware frames; caller writes them in order and only reports a sync after commit. */
    fun snapshotFrames(revision: Int, payload: ByteArray, chunkPayload: Int = 14): List<ByteArray> {
        require(payload.isNotEmpty() && payload.size <= 12000 && chunkPayload in 1..238)
        val out = mutableListOf<ByteArray>()
        val crc = CRC32().apply { update(payload) }.value.toInt()
        out += ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).put(VERSION.toByte()).put(SNAPSHOT_BEGIN.toByte())
            .putShort(revision.toShort()).putShort(payload.size.toShort()).putInt(crc).array()
        var offset = 0
        while (offset < payload.size) {
            val length = minOf(chunkPayload, payload.size - offset)
            out += ByteBuffer.allocate(6 + length).order(ByteOrder.LITTLE_ENDIAN).put(VERSION.toByte()).put(SNAPSHOT_CHUNK.toByte())
                .putShort(revision.toShort()).putShort(offset.toShort()).put(payload, offset, length).array()
            offset += length
        }
        out += ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put(VERSION.toByte()).put(SNAPSHOT_COMMIT.toByte())
            .putShort(revision.toShort()).array()
        return out
    }

    private fun writeString(out: ByteArrayOutputStream, value: String, max: Int) {
        val data = value.toByteArray(Charsets.UTF_8); require(data.size <= max)
        out.write(data.size); out.write(data)
    }
}

/** Reassembles ATT-sized check-in commands and discards stale or inconsistent fragments. */
internal class PassportCommandAssembler(private val timeoutMs: Long = 10_000L) {
    private var request = -1
    private var opcode = -1
    private var expectedBytes = 0
    private var updatedAt = 0L
    private val payload = ByteArrayOutputStream()

    fun accept(frame: ByteArray, nowMs: Long): ByteArray? {
        if (frame.size < 2 || (frame[0].toInt() and 0xff) != PassportProtocol.VERSION ||
            (frame[1].toInt() and 0xff) != PassportProtocol.CHECK_IN_FRAGMENT) {
            reset()
            return frame
        }
        if (frame.size < 9) { reset(); return null }
        val buffer = ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).apply { position(2) }
        val nextOpcode = buffer.get().toInt() and 0xff
        val nextRequest = buffer.short.toInt() and 0xffff
        val declared = buffer.short.toInt() and 0xffff
        val offset = buffer.short.toInt() and 0xffff
        val expired = request != -1 && nowMs - updatedAt > timeoutMs
        if (expired) reset()
        if (offset == 0 && request != -1) reset()
        val validHeader = nextOpcode == PassportProtocol.CHECK_IN || nextOpcode == PassportProtocol.UNCHECK_IN
        val sameMessage = request == -1 ||
            (request == nextRequest && opcode == nextOpcode && expectedBytes == declared)
        if (!validHeader || declared !in 1..100 || !sameMessage || offset != payload.size() ||
            payload.size() + buffer.remaining() > declared) {
            reset()
            return null
        }
        if (request == -1) {
            request = nextRequest
            opcode = nextOpcode
            expectedBytes = declared
        }
        val part = ByteArray(buffer.remaining())
        buffer.get(part)
        payload.write(part)
        updatedAt = nowMs
        if (payload.size() != expectedBytes) return null
        val complete = ByteBuffer.allocate(4 + payload.size()).order(ByteOrder.LITTLE_ENDIAN)
            .put(PassportProtocol.VERSION.toByte()).put(opcode.toByte())
            .putShort(request.toShort()).put(payload.toByteArray()).array()
        reset()
        return complete
    }

    fun reset() {
        request = -1
        opcode = -1
        expectedBytes = 0
        updatedAt = 0L
        payload.reset()
    }
}
