package app.revanced.patches.shared.spoofsignature.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.patches.shared.spoofsignature.fingerprints.SpoofSignatureFingerprint.INTEGRATIONS_CLASS_DESCRIPTOR

internal object SpoofSignatureFingerprint : MethodFingerprint(
    customFingerprint = { _, classDef ->
        classDef.type == INTEGRATIONS_CLASS_DESCRIPTOR
    },
) {
    const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/SpoofSignaturePatch;"

    const val GET_ORIGINAL_PACKAGE_NAME_METHOD_NAME =
        "getOriginalPackageName"
}
