package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object RequestChecksumFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    strings = listOf("X.509", "user", "S"),
)