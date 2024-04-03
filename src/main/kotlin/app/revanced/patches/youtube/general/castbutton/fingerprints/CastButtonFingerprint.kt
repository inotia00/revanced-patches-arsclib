package app.revanced.patches.youtube.general.castbutton.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object CastButtonFingerprint : MethodFingerprint(
    parameters = listOf("I"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/MediaRouteButton;")
                && methodDef.name == "setVisibility"
    }
)