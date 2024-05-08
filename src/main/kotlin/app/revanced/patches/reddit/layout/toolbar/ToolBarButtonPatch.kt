package app.revanced.patches.reddit.layout.toolbar

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.reddit.layout.toolbar.fingerprints.HomePagerScreenFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.Companion.ToolBarNavSearchCtaContainer
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

// @Patch
// @Name("Hide toolbar button")
@Description("Adds an option to hide the r/place or Reddit recap button in the toolbar.")
@DependsOn([SettingsPatch::class, SharedResourceIdPatch::class])
@RedditCompatibility
@Suppress("unused")
@Deprecated("This patch is deprecated until Reddit adds a button like r/place or Reddit recap button to the toolbar.")
class ToolBarButtonPatch : BytecodePatch(
    listOf(HomePagerScreenFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/ToolBarButtonPatch;->hideToolBarButton(Landroid/view/View;)V"
    }

    override fun execute(context: BytecodeContext) {

        HomePagerScreenFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex =
                    getWideLiteralInstructionIndex(ToolBarNavSearchCtaContainer) + 3
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        }

        updateSettingsStatus("enableToolBarButton")

    }
}
