package app.revanced.patches.music.flyoutpanel.replace

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.flyoutpanel.replace.fingerprints.TouchOutsideFingerprint
import app.revanced.patches.music.flyoutpanel.shared.FlyoutPanelMenuItemPatch
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object ReplaceReportPatch : BaseBytecodePatch(
    name = "Replace report",
    description = "Adds an option to replace \"Report\" with \"Playback speed\" in the flyout menu.",
    dependencies = setOf(
        FlyoutPanelMenuItemPatch::class,
        OverrideSpeedHookPatch::class,
        ReplaceReportResourcePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TouchOutsideFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        FlyoutPanelMenuItemPatch.replaceComponents()

        TouchOutsideFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName("setOnClickListener")
                val setOnClickListenerRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "sput-object v$setOnClickListenerRegister, $FLYOUT_CLASS_DESCRIPTOR->touchOutSideView:Landroid/view/View;"
                )
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_report",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_report_only_player",
            "true",
            "revanced_replace_flyout_panel_report"
        )

    }
}
