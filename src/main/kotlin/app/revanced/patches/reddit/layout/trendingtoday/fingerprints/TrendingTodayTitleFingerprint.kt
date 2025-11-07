package app.revanced.patches.reddit.layout.trendingtoday.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

internal object TrendingTodayTitleFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.AND_INT_LIT8),
    strings = listOf("trending_today_title"),
    customFingerprint = { _, classDef ->
        classDef.type.startsWith("Lcom/reddit/") &&
                classDef.type.contains("/composables/")
    }
)