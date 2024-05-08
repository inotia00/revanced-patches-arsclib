package app.revanced.patches.reddit.layout.screenshotpopup.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object ScreenshotTakenBannerFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/", "I"),
    customFingerprint = custom@{ methodDef, classDef ->
        if (!classDef.type.endsWith("\$ScreenshotTakenBannerKt\$lambda-1\$1;"))
            return@custom false

        methodDef.name == "invoke"
    }
)