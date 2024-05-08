package app.revanced.patches.reddit.ad.comments

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.ad.comments.fingerprints.CommentAdsFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.util.getInstruction
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow

class CommentAdsPatch : BytecodePatch(
    setOf(CommentAdsFingerprint)
) {
    companion object {
        private const val INTEGRATION_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/GeneralAdsPatch;->hideCommentAds()Z"
    }

    override fun execute(context: BytecodeContext) {
        CommentAdsFingerprint.resultOrThrow().apply {
            val walkerMethod = getWalkerMethod(context, scanResult.patternScanResult!!.startIndex)
            walkerMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $INTEGRATION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :show
                        new-instance v0, Ljava/lang/Object;
                        invoke-direct {v0}, Ljava/lang/Object;-><init>()V
                        return-object v0
                        """, listOf(ExternalLabel("show", getInstruction(0)))
                )
            }
        }

    }
}
