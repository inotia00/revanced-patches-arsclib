package app.revanced.patches.music.utils.overridespeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackSpeedOnClickListenerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = handler@{ methodDef, _ ->
        if (methodDef.definingClass.startsWith("Lcom/"))
            return@handler false

        methodDef.name == "onClick"
                && methodDef.containsWideLiteralInstructionIndex(147448)
    }
)