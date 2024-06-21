package app.revanced.patches.shared.spoofsignature.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.shared.spoofsignature.fingerprints.CertificateFingerprint.GET_PACKAGE_NAME_METHOD_REFERENCE
import app.revanced.util.fingerprint.ReferenceFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object CertificateFingerprint : ReferenceFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("X.509", "user", "S"),
    reference = { GET_PACKAGE_NAME_METHOD_REFERENCE }
) {
    const val GET_PACKAGE_NAME_METHOD_REFERENCE =
        "Landroid/content/Context;->getPackageName()Ljava/lang/String;"
}
