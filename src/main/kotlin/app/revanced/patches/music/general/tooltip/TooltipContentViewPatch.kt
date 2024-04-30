package app.revanced.patches.music.general.tooltip

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patches.music.general.tooltip.fingerprints.TooltipContentViewFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object TooltipContentViewPatch : BaseBytecodePatch(
    name = "Hide tooltip content",
    description = "Hides the tooltip box that appears when opening the app for the first time.",
    dependencies = setOf(SharedResourceIdPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TooltipContentViewFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TooltipContentViewFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "return-void"
        )

    }
}
