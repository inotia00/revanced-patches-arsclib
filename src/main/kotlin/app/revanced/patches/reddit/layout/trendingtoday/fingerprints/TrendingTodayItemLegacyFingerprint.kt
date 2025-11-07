package app.revanced.patches.reddit.layout.trendingtoday.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object TrendingTodayItemLegacyFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("search_trending_item"),
    customFingerprint = { _, classDef ->
        classDef.type.startsWith("Lcom/reddit/typeahead/ui/zerostate/composables")
    }
)