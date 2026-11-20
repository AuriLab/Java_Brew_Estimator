package com.example.javabrewestimator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ExampleUnitTest {

    @Test
    fun testCalculateExtractionYieldEspresso() {
        val yield = calculateExtractionYield(
            method = BrewMethod.ESPRESSO,
            roast = RoastLevel.MEDIUM,
            beanWeightGrams = 18f,
            liquidYieldGrams = 36f,
            brewTimeSec = 30f,
            pourOverRatio = 15f,
            drawdownTimeMin = 3.5f,
            waterTempC = 93f,
            grinderRpm = 900f,
            beanAgeDays = 14f,
        )
        // Expected extraction yield should fall within standard 18-22% sweet spot range
        assertTrue("Extraction yield $yield should be between 15% and 25%", (yield in 15f..25f))
    }

    @Test
    fun testCalculateExtractionYieldPourOver() {
        val yield = calculateExtractionYield(
            method = BrewMethod.POUR_OVER,
            roast = RoastLevel.LIGHT,
            beanWeightGrams = 15f,
            liquidYieldGrams = 225f,
            brewTimeSec = 30f,
            pourOverRatio = 15f,
            drawdownTimeMin = 3.5f,
            waterTempC = 93f,
            grinderRpm = 900f,
            beanAgeDays = 14f,
        )
        assertTrue("Pour Over yield $yield should be between 15% and 25%", (yield in 15f..25f))
    }

    @Test
    fun testCalculateBalancedBrewTime() {
        val brewTime = calculateBalancedBrewTime(
            roast = RoastLevel.MEDIUM,
            beanWeightGrams = 18f,
            liquidYieldGrams = 36f,
            waterTempC = 93f,
            grinderRpm = 900f,
            beanAgeDays = 14f,
        )
        assertTrue("Balanced brew time $brewTime should be within valid 15s to 50s range", (brewTime in 15f..50f))
    }

    @Test
    fun testCalculateBalancedPourOverRatio() {
        val ratio = calculateBalancedPourOverRatio(
            roast = RoastLevel.LIGHT,
            drawdownTimeMin = 3.5f,
            waterTempC = 93f,
            grinderRpm = 900f,
            beanAgeDays = 14f,
        )
        assertTrue("Balanced pour over ratio $ratio should be within valid 13:1 to 17:1 range", (ratio in 13f..17f))
    }

    @Test
    fun testGetBackgroundDrawableForTime() {
        assertEquals(R.drawable.morning, getBackgroundDrawableForTime(LocalTime.of(8, 0)))
        assertEquals(R.drawable.afternoon, getBackgroundDrawableForTime(LocalTime.of(14, 0)))
        assertEquals(R.drawable.evening, getBackgroundDrawableForTime(LocalTime.of(20, 0)))
        assertEquals(R.drawable.evening, getBackgroundDrawableForTime(LocalTime.of(0, 30)))
    }
}