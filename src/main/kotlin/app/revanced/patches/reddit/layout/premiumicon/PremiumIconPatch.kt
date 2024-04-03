package app.revanced.patches.reddit.layout.premiumicon

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.reddit.layout.premiumicon.fingerprints.PremiumIconFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object PremiumIconPatch : BaseBytecodePatch(
    name = "Premium icon",
    description = "Unlocks premium app icons.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PremiumIconFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PremiumIconFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: throw PremiumIconFingerprint.exception

    }
}
