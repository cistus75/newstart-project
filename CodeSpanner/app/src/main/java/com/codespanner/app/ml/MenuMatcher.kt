package com.codespanner.app.ml

import com.codespanner.app.model.MenuDatabase
import com.codespanner.app.model.MenuItem

object MenuMatcher {

    // OCR로 읽은 이름을 DB 메뉴명과 매칭. 유사도 0.55 이상이면 반환, 미달이면 null
    fun match(ocrName: String): MenuItem? {
        val query = ocrName.replace(" ", "")
        if (query.isEmpty()) return null

        var bestScore = 0.0
        var bestItem: MenuItem? = null

        for (item in MenuDatabase.all) {
            val score = similarity(query, item.name)
            if (score > bestScore) {
                bestScore = score
                bestItem = item
            }
        }

        return if (bestScore >= 0.55) bestItem else null
    }

    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        // query(a)가 메뉴명(b)을 포함하는 경우만 높은 점수 — 반대 방향은 허용하지 않음
        // (예: "에이드" ⊂ "유자에이드" 같은 짧은 부분문자열 오매칭 방지)
        if (a.contains(b)) {
            return b.length.toDouble() / a.length * 0.95
        }
        val dist = levenshtein(a, b)
        return 1.0 - dist.toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
            }
        }
        return dp[a.length][b.length]
    }
}
