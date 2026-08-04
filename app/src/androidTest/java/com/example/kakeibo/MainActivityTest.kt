package com.example.kakeibo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import java.time.YearMonth
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenIsShownOnLaunch() {
        composeRule.onNodeWithText("ホーム").assertIsDisplayed()
        composeRule.onNodeWithText("今月の予算").assertIsDisplayed()
        composeRule.onNodeWithText("＋ 記録").assertIsDisplayed()
    }

    @Test
    fun emptyAmountShowsValidationError() {
        composeRule.onNodeWithText("＋ 記録").performClick()
        composeRule.onNodeWithText("保存").performClick()

        composeRule.onNodeWithText("金額を入力してください").assertIsDisplayed()
        composeRule.onNodeWithText("カテゴリを選択してください").assertIsDisplayed()
    }

    @Test
    fun zeroAmountShowsMinimumError() {
        composeRule.onNodeWithText("＋ 記録").performClick()
        composeRule.onNodeWithText("金額（円）").performTextInput("0")
        composeRule.onNodeWithText("保存").performClick()

        composeRule.onNodeWithText("1円以上で入力してください").assertIsDisplayed()
    }

    @Test
    fun amountOverMaximumShowsOverflowError() {
        composeRule.onNodeWithText("＋ 記録").performClick()
        composeRule.onNodeWithText("金額（円）").performTextInput("10000000000")
        composeRule.onNodeWithText("保存").performClick()

        composeRule.onNodeWithText("金額が大きすぎます").assertIsDisplayed()
    }

    @Test
    fun amountWithWhitespaceIsSavedAfterTrimming() {
        composeRule.onNodeWithText("＋ 記録").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("食費").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("金額（円）").performTextInput(" 1000 ")
        composeRule.onNodeWithText("食費").performClick()
        composeRule.onNodeWithText("保存").performClick()

        assertTrue(composeRule.onAllNodesWithText("1,000円").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun budgetWithWhitespaceIsSavedAfterTrimming() {
        composeRule.onNodeWithText("今月の予算").assertIsDisplayed()
        composeRule.onNodeWithTag("budget_action").performClick()
        composeRule.onNodeWithText("予算（円）").performTextReplacement(" 5000 ")
        composeRule.onNodeWithText("保存").performClick()

        assertTrue(composeRule.onAllNodesWithText("5,000円", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun budgetScreenKeepsSelectedPreviousMonth() {
        val previousMonth = YearMonth.now().minusMonths(1)

        composeRule.onNodeWithText("‹").performClick()
        composeRule.onNodeWithTag("budget_action").performClick()

        composeRule.onNodeWithText("${previousMonth.year}年${previousMonth.monthValue}月").assertIsDisplayed()
    }

    @Test
    fun rapidSaveDoesNotCreateDuplicateTransaction() {
        composeRule.onNodeWithText("＋ 記録").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("食費").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("金額（円）").performTextInput("7654321")
        composeRule.onNodeWithText("食費").performClick()
        composeRule.onNodeWithText("保存").performTouchInput {
            click()
            click()
        }

        composeRule.onAllNodesWithText("7,654,321円").assertCountEquals(2)
        composeRule.onAllNodesWithText("15,308,642円").assertCountEquals(0)
    }
}
