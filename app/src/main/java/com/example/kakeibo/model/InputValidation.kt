package com.example.kakeibo.model

const val MAX_AMOUNT_YEN = 9_999_999_999L
const val MAX_MEMO_LENGTH = 200
const val MAX_CATEGORY_NAME_LENGTH = 20

/** UI入力用。貼り付けを含め、保存時に数字だけであることと範囲を検証する。 */
fun sanitizeAmountText(value: String): String = value.filter(Char::isDigit)

/** 金額文字列を検証時と同じ前後空白除去ルールで数値化する。 */
fun parseAmountText(value: String): Long? = value.trim().toLongOrNull()

fun validateAmountText(value: String): String? {
    val normalized = value.trim()
    val amount = normalized.toLongOrNull()
    return when {
        normalized.isEmpty() -> "金額を入力してください"
        normalized.any { !it.isDigit() } -> "数字のみで入力してください"
        amount == null -> "金額が大きすぎます"
        amount <= 0 -> "1円以上で入力してください"
        amount > MAX_AMOUNT_YEN -> "金額が大きすぎます"
        else -> null
    }
}

fun normalizeMemo(value: String?): String? {
    val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    require(cleaned.length <= MAX_MEMO_LENGTH) { "メモは${MAX_MEMO_LENGTH}文字以内で入力してください" }
    return cleaned
}

fun validateCategoryName(value: String): String? {
    val cleaned = value.trim()
    return when {
        cleaned.isEmpty() -> "カテゴリ名を入力してください"
        cleaned.length > MAX_CATEGORY_NAME_LENGTH -> "カテゴリ名は${MAX_CATEGORY_NAME_LENGTH}文字以内で入力してください"
        else -> null
    }
}
