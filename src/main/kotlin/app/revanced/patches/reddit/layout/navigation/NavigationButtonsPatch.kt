package app.revanced.patches.reddit.layout.navigation

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Hide navigation buttons")
@Description("Adds options to hide buttons in the navigation bar.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class NavigationButtonsPatch : BytecodePatch(
    listOf(BottomNavScreenFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/NavigationButtonsPatch;->hideNavigationButtons(Landroid/view/ViewGroup;)V"
    }

    override fun execute(context: BytecodeContext) {

        BottomNavScreenFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(startIndex).registerC

                addInstruction(
                    startIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        }

        updateSettingsStatus("enableNavigationButtons")

    }
}
