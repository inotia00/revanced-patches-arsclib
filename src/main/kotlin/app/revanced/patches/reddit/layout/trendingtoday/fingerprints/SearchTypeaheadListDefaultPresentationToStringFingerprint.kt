package app.revanced.patches.reddit.layout.trendingtoday.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object SearchTypeaheadListDefaultPresentationToStringFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("OnSearchTypeaheadListDefaultPresentation(title="),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "toString"
    }
)