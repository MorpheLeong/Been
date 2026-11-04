package com.bugchecking.app

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PassportDesignTokenTest {
    @Test
    fun `uses minimum non-zero system corner radius`() {
        assertEquals(
            144,
            uniformScreenCornerRadiusPx(
                apiLevel = 37,
                cornerRadiiPx = listOf(160, 144, 160, 152),
                fallbackPx = 56,
            ),
        )
    }

    @Test
    fun `square display keeps square corners`() {
        assertEquals(
            0,
            uniformScreenCornerRadiusPx(
                apiLevel = 37,
                cornerRadiiPx = listOf(0, 0, 0, 0),
                fallbackPx = 56,
            ),
        )
    }

    @Test
    fun `pre android twelve uses fallback`() {
        assertEquals(
            56,
            uniformScreenCornerRadiusPx(
                apiLevel = 30,
                cornerRadiiPx = listOf(160, 160, 160, 160),
                fallbackPx = 56,
            ),
        )
    }

    @Test
    fun `semantic shapes separate shells fields and buttons`() {
        val shapes = PassportShapeTokens(screenCornerRadius = 45.7.dp)

        assertEquals(45.7.dp, shapes.cardCornerRadius)
        assertEquals(16.dp, shapes.fieldCornerRadius)
        assertEquals(24.dp, shapes.buttonCornerRadius)
        assertEquals(29.7.dp, shapes.floatingNavigationCornerRadius)
        assertEquals(shapes.full, shapes.navigationSelectionShape)
    }

    @Test
    fun `card content keeps at least twenty four dp from curved edges`() {
        assertEquals(24.dp, PassportContentTokens(screenCornerRadius = 0.dp).cardHorizontalPadding)
        assertEquals(24.dp, PassportContentTokens(screenCornerRadius = 45.7.dp).cardHorizontalPadding)
        assertEquals(32.dp, PassportContentTokens(screenCornerRadius = 64.dp).cardHorizontalPadding)
        assertEquals(24.dp, PassportContentTokens(screenCornerRadius = 64.dp).cardVerticalPadding)
        assertEquals(16.dp, PassportContentTokens(screenCornerRadius = 64.dp).compactCardVerticalPadding)
    }

    @Test
    fun `compact navigation geometry remains inside sixty four dp shell`() {
        val components = PassportComponentTokens()

        assertEquals(64.dp, components.compactNavigationHeight)
        assertEquals(16.dp, components.floatingHorizontalInset)
        assertEquals(4.dp, components.compactNavigationContentPadding)
        assertEquals(56.dp, components.compactNavigationItemHeight)
        assertEquals(24.dp, components.compactNavigationIconSize)
        assertEquals(2.dp, components.compactNavigationLabelGap)
        assertEquals(4.dp, components.floatingControlShadowElevation)
        assertEquals(4.dp, components.floatingNavigationShadowElevation)
        assertEquals(6.dp, components.floatingFabDefaultElevation)
        assertEquals(8.dp, components.floatingFabPressedElevation)
        assertEquals(1.dp, PassportBorderTokens().floating)
    }

    @Test
    fun `compact navigation keeps exactly the three primary labels`() {
        assertEquals(listOf("轨迹", "清单", "二维码"), primaryNavigationTabs.map(Tab::label))
    }

    @Test
    fun `pager offset maps continuously to compact navigation position`() {
        assertEquals(0f, primaryNavigationPosition(0, 0f))
        assertEquals(0.4f, primaryNavigationPosition(0, 0.4f))
        assertEquals(0.6f, primaryNavigationPosition(1, -0.4f))
        assertEquals(2f, primaryNavigationPosition(2, 0.4f))
    }

    @Test
    fun `navigation label emphasis crossfades between adjacent items`() {
        assertEquals(0.25f, primaryNavigationSelectionFraction(0.75f, 0))
        assertEquals(0.75f, primaryNavigationSelectionFraction(0.75f, 1))
        assertEquals(0f, primaryNavigationSelectionFraction(0.75f, 2))
    }

    @Test
    fun `floating navigation uses bounded concentric radius`() {
        assertEquals(16.dp, concentricCornerRadius(0.dp, 16.dp, 64.dp))
        assertEquals(16.dp, concentricCornerRadius(24.dp, 16.dp, 64.dp))
        assertEquals(29.7.dp, concentricCornerRadius(45.7.dp, 16.dp, 64.dp))
        assertEquals(32.dp, concentricCornerRadius(64.dp, 16.dp, 64.dp))
    }

    @Test
    fun `floating rim protects dark mode without dirtying light mode`() {
        assertEquals(0f, floatingControlBorderAlpha(darkTheme = false))
        assertEquals(0.28f, floatingControlBorderAlpha(darkTheme = true))
    }
}
