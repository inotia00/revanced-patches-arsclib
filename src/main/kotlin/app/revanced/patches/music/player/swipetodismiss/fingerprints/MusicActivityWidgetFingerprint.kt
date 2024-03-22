package app.revanced.patches.music.player.swipetodismiss.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex

object MusicActivityWidgetFingerprint : MethodFingerprint(
    customFingerprint = handler@{ methodDef, _ ->
        if (!methodDef.definingClass.endsWith("/MusicActivity;"))
            return@handler false

        methodDef.containsWideLiteralInstructionIndex(79500)
    }
)
