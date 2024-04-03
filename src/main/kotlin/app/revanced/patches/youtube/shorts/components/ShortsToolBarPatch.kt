package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.components.fingerprints.ToolBarBannerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow

@Patch(dependencies = [ToolBarHookPatch::class])
object ShortsToolBarPatch : BytecodePatch(
    setOf(ToolBarBannerFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ToolBarBannerFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)

            walkerMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsToolBarBanner()Z
                        move-result v0
                        if-nez v0, :hide
                        """,
                    ExternalLabel("hide", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        }

        ToolBarHookPatch.injectCall("$SHORTS_CLASS_DESCRIPTOR->hideShortsToolBarButton")
    }
}
