package app.revanced.patches.music.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ColorGrey
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.util.containsWideLiteralInstructionIndex

object MiniPlayerConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("sharedToggleMenuItemMutations"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionIndex(ColorGrey)
                && methodDef.containsWideLiteralInstructionIndex(MiniPlayerPlayPauseReplayButton)
    }
)