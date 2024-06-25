package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object AcknowledgementsLabelBuilderFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroidx/preference/Preference;"),
    strings = listOf("onboardingAnalytics"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.startsWith("Lcom/reddit/screen/settings/preferences/")
    }
)