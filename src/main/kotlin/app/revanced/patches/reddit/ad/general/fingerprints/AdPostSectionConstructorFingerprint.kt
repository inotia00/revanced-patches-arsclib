package app.revanced.patches.reddit.ad.general.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

/**
 * for New ad post
 */
internal object AdPostSectionConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf(
        "adPayload",
        "feed_post_section__"
    ),
    customFingerprint = { methodDef, _ -> methodDef.name == "<init>" },
)