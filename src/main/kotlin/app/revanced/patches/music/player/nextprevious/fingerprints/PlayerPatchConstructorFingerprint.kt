package app.revanced.patches.music.player.nextprevious.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER

object PlayerPatchConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == PLAYER
                && methodDef.name == "<init>"
    }
)