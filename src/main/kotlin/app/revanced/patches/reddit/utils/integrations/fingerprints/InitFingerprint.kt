package app.revanced.patches.reddit.utils.integrations.fingerprints

import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint

@Suppress("DEPRECATION")
object InitFingerprint : IntegrationsFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/FrontpageApplication;") &&
                methodDef.name == "onCreate"
    }
)