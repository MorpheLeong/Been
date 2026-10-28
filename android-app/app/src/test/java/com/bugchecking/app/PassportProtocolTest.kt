package com.bugchecking.app

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassportProtocolTest {
    @Test
    fun uncheckInNeverRecordsTrack() {
        assertFalse(PassportProtocol.commandRecordsTrack(PassportProtocol.UNCHECK_IN))
        assertTrue(PassportProtocol.commandRecordsTrack(PassportProtocol.CHECK_IN))
        assertTrue(PassportProtocol.commandRecordsTrack(1))
    }

    @Test fun parsesTrackAndDestinationCommands() {
        val track = byteArrayOf(3, 1, 0x34, 0x12)
        assertEquals(PassportProtocol.CheckIn(0x1234, 1, "", "轨迹记录"), PassportProtocol.parseCommand(track))

        val destination = directCommand(0x2468, PassportProtocol.CHECK_IN, "dest-1", "博物馆")
        assertEquals(
            PassportProtocol.CheckIn(0x2468, PassportProtocol.CHECK_IN, "dest-1", "博物馆"),
            PassportProtocol.parseCommand(destination),
        )
    }

    @Test fun createsAttSafeResultAndSnapshotFrames() {
        val result = PassportProtocol.checkInResult(
            0x1234, true, timestampMillis = 1_800_000_100_000L, offsetMinutes = 480,
        )
        assertEquals(11, result.size)
        assertArrayEquals(byteArrayOf(3, 0x81.toByte(), 0, 0x34, 0x12), result.copyOfRange(0, 5))
        assertEquals(1_800_000_100, ByteBuffer.wrap(result, 5, 4)
            .order(ByteOrder.LITTLE_ENDIAN).int)

        val payload = ByteArray(37) { it.toByte() }
        val frames = PassportProtocol.snapshotFrames(7, payload, 14)
        assertTrue(frames.all { it.size <= 20 })
        assertEquals(PassportProtocol.SNAPSHOT_BEGIN, frames.first()[1].toInt() and 0xff)
        assertEquals(PassportProtocol.SNAPSHOT_COMMIT, frames.last()[1].toInt() and 0xff)
        assertEquals(listOf(0, 14, 28), frames.drop(1).dropLast(1).map {
            ByteBuffer.wrap(it, 4, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
        })

        val largePayload = ByteArray(500) { it.toByte() }
        val mtu247Frames = PassportProtocol.snapshotFrames(8, largePayload, 238)
        assertTrue(mtu247Frames.all { it.size <= 244 })
        assertEquals(listOf(0, 238, 476), mtu247Frames.drop(1).dropLast(1).map {
            ByteBuffer.wrap(it, 4, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
        })
    }

    @Test fun createsFragmentedTrackNameBatchesAndSanitizesUtf8() {
        val track = track(
            id = 1,
            requestId = "device-4660-destination",
            name = "  上海\n图书馆-${"界".repeat(20)}  ",
            time = 1_800_000_100_999L,
        )
        val frames = PassportProtocol.trackNameFrames(9, listOf(track), 14)
        assertTrue(frames.all { it.size <= 20 })
        assertEquals(PassportProtocol.TRACK_NAMES_BEGIN, frames.first()[1].toInt() and 0xff)
        assertEquals(PassportProtocol.TRACK_NAMES_COMMIT, frames.last()[1].toInt() and 0xff)
        val payload = frames.drop(1).dropLast(1).flatMap { frame ->
            frame.copyOfRange(6, frame.size).toList()
        }.toByteArray()
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, buffer.get().toInt() and 0xff)
        assertEquals(0x1234, buffer.short.toInt() and 0xffff)
        assertEquals(1_800_000_100, buffer.int)
        val nameBytes = ByteArray(buffer.get().toInt() and 0xff).also(buffer::get)
        val name = nameBytes.toString(Charsets.UTF_8)
        assertTrue(name.startsWith("上海 图书馆-"))
        assertTrue(nameBytes.size <= 47)

        assertTrue(PassportProtocol.trackNameFrames(
            10, listOf(track.copy(requestId = "manual-id")), 238,
        ).isEmpty())
    }

    @Test fun trackNameBatchIsLimitedToLatestThirtyTwoInputs() {
        val tracks = (1L..35L).map { id ->
            track(id, "device-$id-", "地点$id", 1_800_000_000_000L + id * 1000)
        }
        val frames = PassportProtocol.trackNameFrames(11, tracks, 238)
        val payload = frames.drop(1).dropLast(1).flatMap {
            it.copyOfRange(6, it.size).toList()
        }.toByteArray()
        assertEquals(32, payload[0].toInt() and 0xff)
    }

    @Test fun createsExplicitTimeSyncFrame() {
        assertArrayEquals(
            byteArrayOf(3, 0x14, 0x00, 0x5A, 0xC5.toByte(), 0x6A, 0x20, 0xFE.toByte()),
            PassportProtocol.timeSyncFrame(1_791_318_528_000L, -480),
        )
    }

    @Test fun createsAvatarFramesWithOffsetsAndCrc() {
        val pixels = ByteArray(AvatarImage.DEVICE_BYTES) { (it * 13).toByte() }
        val frames = PassportProtocol.avatarFrames(pixels, 16)
        assertEquals(11, frames.first().size)
        assertEquals(PassportProtocol.AVATAR_BEGIN, frames.first()[1].toInt() and 0xff)
        assertEquals(AvatarImage.DEVICE_SIZE, frames.first()[3].toInt() and 0xff)
        assertEquals(AvatarImage.DEVICE_SIZE, frames.first()[4].toInt() and 0xff)
        val header = ByteBuffer.wrap(frames.first()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(AvatarImage.DEVICE_BYTES, header.getShort(5).toInt() and 0xffff)
        assertEquals(CRC32().apply { update(pixels) }.value.toInt(), header.getInt(7))
        assertTrue(frames.drop(1).dropLast(1).all { it.size <= 20 })
        assertEquals(listOf(0, 16, 32), frames.drop(1).dropLast(1).take(3).map {
            ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).getShort(2).toInt() and 0xffff
        })
        assertEquals(PassportProtocol.AVATAR_COMMIT, frames.last()[1].toInt() and 0xff)
        assertArrayEquals(
            byteArrayOf(PassportProtocol.VERSION.toByte(), PassportProtocol.AVATAR_CLEAR.toByte()),
            PassportProtocol.avatarFrames(null).single(),
        )
    }

    @Test fun convertsRgb888ToDeviceRgb565() {
        assertEquals(0xf800, AvatarImage.rgb888ToRgb565(255, 0, 0))
        assertEquals(0x07e0, AvatarImage.rgb888ToRgb565(0, 255, 0))
        assertEquals(0x001f, AvatarImage.rgb888ToRgb565(0, 0, 255))
        assertEquals(0xffff, AvatarImage.rgb888ToRgb565(255, 255, 255))
    }

    @Test fun reassemblesFragmentsAndRejectsMalformedSequences() {
        val command = directCommand(55, PassportProtocol.CHECK_IN, "id-1", "公园")
        val payload = command.copyOfRange(4, command.size)
        val assembler = PassportCommandAssembler(timeoutMs = 100)
        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 0, 5), 1_000))
        assertArrayEquals(command, assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 5, payload.size), 1_050))

        assertNull(assembler.accept(ByteArray(8) { 0 }.also { it[0] = 3; it[1] = 0x42 }, 2_000))
        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 0, 5), 3_000))
        assertNull(assembler.accept(fragment(55, PassportProtocol.UNCHECK_IN, payload, 5, payload.size), 3_010))
        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 5, payload.size), 3_020))

        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 0, 5), 4_000))
        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 5, payload.size), 4_101))

        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 0, 5), 5_000))
        assertNull(assembler.accept(fragment(55, PassportProtocol.CHECK_IN, payload, 0, 5), 5_010))
        assertArrayEquals(command, assembler.accept(
            fragment(55, PassportProtocol.CHECK_IN, payload, 5, payload.size), 5_020,
        ))
    }

    private fun directCommand(request: Int, opcode: Int, id: String, name: String): ByteArray {
        val idBytes = id.toByteArray()
        val nameBytes = name.toByteArray()
        return ByteBuffer.allocate(6 + idBytes.size + nameBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .put(3).put(opcode.toByte()).putShort(request.toShort())
            .put(idBytes.size.toByte()).put(idBytes).put(nameBytes.size.toByte()).put(nameBytes).array()
    }

    private fun track(
        id: Long,
        requestId: String,
        name: String,
        time: Long,
    ) = TrackPoint(
        id = id,
        requestId = requestId,
        destinationId = null,
        name = name,
        lat = 31.2,
        lon = 121.4,
        time = time,
        accuracy = 5f,
    )

    private fun fragment(request: Int, opcode: Int, payload: ByteArray, from: Int, to: Int): ByteArray =
        ByteBuffer.allocate(9 + to - from).order(ByteOrder.LITTLE_ENDIAN)
            .put(3).put(0x42).put(opcode.toByte()).putShort(request.toShort())
            .putShort(payload.size.toShort()).putShort(from.toShort()).put(payload, from, to - from).array()
}
