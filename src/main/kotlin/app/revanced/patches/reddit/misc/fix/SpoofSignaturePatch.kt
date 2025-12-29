package app.revanced.patches.reddit.misc.fix

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.misc.fix.fingerprints.ApplicationFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.integrations.IntegrationsPatch
import app.revanced.util.resultOrThrow

@Patch
@Name("Spoof signature")
@Description("Spoofs the signature of the app.")
@DependsOn([IntegrationsPatch::class])
@RedditCompatibility
@RequiresIntegrations
@Suppress("unused")
class SpoofSignaturePatch : BytecodePatch(
    listOf(ApplicationFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/SpoofSignaturePatch;"
    }

    override fun execute(context: BytecodeContext) {
        ApplicationFingerprint.resultOrThrow().mutableClass.setSuperClass(INTEGRATIONS_CLASS_DESCRIPTOR)
    }
}
