package app.revanced.patches.reddit.layout.sidebar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object HeaderItemUiModelToStringFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "HeaderItemUiModel(uniqueId=",
        ", type="
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "toString"
    }
)