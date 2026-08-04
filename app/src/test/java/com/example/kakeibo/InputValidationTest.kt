package com.example.kakeibo

import com.example.kakeibo.model.MAX_AMOUNT_YEN
import com.example.kakeibo.model.MAX_CATEGORY_NAME_LENGTH
import com.example.kakeibo.model.MAX_MEMO_LENGTH
import com.example.kakeibo.model.normalizeMemo
import com.example.kakeibo.model.parseAmountText
import com.example.kakeibo.model.sanitizeAmountText
import com.example.kakeibo.model.validateAmountText
import com.example.kakeibo.model.validateCategoryName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {
    @Test
    fun amountAcceptsBoundaryValues() {
        assertNull(validateAmountText("1"))
        assertNull(validateAmountText(MAX_AMOUNT_YEN.toString()))
        assertEquals(1L, sanitizeAmountText("0001").toLong())
    }

    @Test
    fun amountWithWhitespaceIsValidatedAndParsedUsingTheSameValue() {
        assertNull(validateAmountText("  1000  "))
        assertEquals(1_000L, parseAmountText("  1000  "))
    }

    @Test
    fun amountRejectsEmptyZeroAndOverflow() {
        assertEquals("金額を入力してください", validateAmountText(""))
        assertEquals("金額を入力してください", validateAmountText("   "))
        assertEquals("1円以上で入力してください", validateAmountText("0"))
        assertEquals("数字のみで入力してください", validateAmountText("-1"))
        assertEquals("数字のみで入力してください", validateAmountText("1.5"))
        assertEquals("数字のみで入力してください", validateAmountText("¥1,000"))
        assertEquals("金額が大きすぎます", validateAmountText("10000000000"))
        assertEquals("金額が大きすぎます", validateAmountText(Long.MAX_VALUE.toString() + "0"))
    }

    @Test
    fun pastedAmountRemovesNonDigits() {
        assertEquals("12345", sanitizeAmountText("¥12,345円"))
        assertEquals("12", sanitizeAmountText("1a2"))
        assertEquals("", sanitizeAmountText("abc-+."))
    }

    @Test
    fun memoIsTrimmedAndEmptyMemoBecomesNull() {
        assertNull(normalizeMemo(null))
        assertNull(normalizeMemo(" \n\t "))
        assertEquals("買い物", normalizeMemo("  買い物  "))
        assertEquals(MAX_MEMO_LENGTH, normalizeMemo("あ".repeat(MAX_MEMO_LENGTH))!!.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun memoOverLimitIsRejected() {
        normalizeMemo("あ".repeat(MAX_MEMO_LENGTH + 1))
    }

    @Test
    fun categoryNameValidatesTrimmedLength() {
        assertEquals("カテゴリ名を入力してください", validateCategoryName("  "))
        assertNull(validateCategoryName(" 食費 "))
        assertNull(validateCategoryName("あ".repeat(MAX_CATEGORY_NAME_LENGTH)))
        assertTrue(validateCategoryName("あ".repeat(MAX_CATEGORY_NAME_LENGTH + 1))!!.contains("20文字以内"))
    }
}
