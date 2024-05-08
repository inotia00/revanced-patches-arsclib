package app.revanced.patches.reddit.layout.premiumicon

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.premiumicon.fingerprints.PremiumIconFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.util.resultOrThrow

@Patch
@Name("Premium icon")
@Description("Unlocks premium app icons.")
@RedditCompatibility
@Suppress("unused")
class PremiumIconPatch : BytecodePatch(
    listOf(PremiumIconFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PremiumIconFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        }

    }
}
