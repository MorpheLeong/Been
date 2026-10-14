package com.folotoy.passport

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassportProtocolTest {
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
        val result = PassportProtocol.checkInResult(0x1234, true)
        assertEquals(11, result.size)
        assertArrayEquals(byteArrayOf(3, 0x81.toByte(), 0, 0x34, 0x12), result.copyOfRange(0, 5))

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

    @Test fun createsExplicitTimeSyncFrame() {
        assertArrayEquals(
            byteArrayOf(3, 0x14, 0x00, 0x5A, 0xC5.toByte(), 0x6A, 0x20, 0xFE.toByte()),
            PassportProtocol.timeSyncFrame(1_791_318_528_000L, -480),
        )
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

    private fun fragment(request: Int, opcode: Int, payload: ByteArray, from: Int, to: Int): ByteArray =
        ByteBuffer.allocate(9 + to - from).order(ByteOrder.LITTLE_ENDIAN)
            .put(3).put(0x42).put(opcode.toByte()).putShort(request.toShort())
            .putShort(payload.size.toShort()).putShort(from.toShort()).put(payload, from, to - from).array()
}
