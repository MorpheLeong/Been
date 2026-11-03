package com.bugchecking.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PassportColorSchemeTest {
    @Test
    fun fixedPaletteKeepsInteractionGreenAndJourneyRed() {
        val light = passportColorScheme(darkTheme = false)
        val dark = passportColorScheme(darkTheme = true)

        assertEquals(Color(0xFF53FF9A), light.primary)
        assertEquals(Color(0xFFC3004F), light.tertiary)
        assertEquals(Color(0xFF53FF9A), dark.primary)
        assertEquals(Color(0xFFFF5A86), dark.tertiary)
        assertEquals(light.primary, light.primaryContainer)
        assertEquals(light.primary, light.secondary)
        assertEquals(light.primary, light.secondaryContainer)
        assertEquals(light.primary, dark.primary)
        assertEquals(Color(0xFF53FF9A), PassportNeonGreen)
        assertEquals(Color(0xFF00351D), PassportOnNeonGreen)
        assertEquals(Color(0xFF007A49), PassportDisconnectedGreen)
    }

    @Test
    fun disconnectedDeviceGreenRemainsVisibleOnBothCanvases() {
        listOf(false, true).forEach { darkTheme ->
            val surface = passportColorScheme(darkTheme).surface
            assertTrue(
                "Expected disconnected device icon to remain visible on $surface",
                contrastRatio(PassportDisconnectedGreen, surface) >= 3.0,
            )
        }
    }

    @Test
    fun deviceActionUsesDeepGreenOnlyWhileDisconnected() {
        assertEquals(PassportDisconnectedGreen, deviceActionTint(DeviceActionState.DISCONNECTED))
        DeviceActionState.entries.filterNot { it == DeviceActionState.DISCONNECTED }.forEach { state ->
            assertEquals("Expected neon device tint for $state", PassportNeonGreen, deviceActionTint(state))
        }
    }

    @Test
    fun favoriteTiersUseThreeDistinctCandyRedRolesInBothThemes() {
        listOf(false, true).forEach { darkTheme ->
            val palettes = (1..3).map { favoriteTierPalette(darkTheme, it) }
            assertEquals(3, palettes.map { it.container }.distinct().size)
            assertEquals(3, palettes.map { it.pulse }.distinct().size)
            assertEquals(3, palettes.map { it.accent }.distinct().size)
            assertTrue(palettes.none { it.container == PassportNeonGreen })
            assertTrue(palettes.none { it.pulse == PassportNeonGreen })
        }
    }

    @Test
    fun semanticForegroundPairsMeetNormalTextContrast() {
        listOf(false, true).forEach { darkTheme ->
            val scheme = passportColorScheme(darkTheme)
            val pairs = listOf(
                scheme.onPrimary to scheme.primary,
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondary to scheme.secondary,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiary to scheme.tertiary,
                scheme.onTertiaryContainer to scheme.tertiaryContainer,
                scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.surface,
                scheme.onError to scheme.error,
                scheme.onErrorContainer to scheme.errorContainer,
                scheme.inverseOnSurface to scheme.inverseSurface,
            )

            pairs.forEach { (foreground, background) ->
                assertTrue(
                    "Expected >= 4.5 contrast, got ${contrastRatio(foreground, background)} " +
                        "for $foreground on $background (darkTheme=$darkTheme)",
                    contrastRatio(foreground, background) >= 4.5,
                )
            }
        }
    }

    @Test
    fun outlinesAndJourneyAccentsRemainVisibleOnBaseSurface() {
        listOf(false, true).forEach { darkTheme ->
            val scheme = passportColorScheme(darkTheme)
            listOf(scheme.outline, scheme.tertiary).forEach { color ->
                assertTrue(
                    "Expected >= 3.0 contrast, got ${contrastRatio(color, scheme.surface)} " +
                        "for $color on ${scheme.surface} (darkTheme=$darkTheme)",
                    contrastRatio(color, scheme.surface) >= 3.0,
                )
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linearize(color.red.toDouble()) +
            0.7152 * linearize(color.green.toDouble()) +
            0.0722 * linearize(color.blue.toDouble())

    private fun linearize(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
