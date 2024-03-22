package app.revanced.patches.music.flyoutpanel.replace

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.flyoutpanel.replace.fingerprints.TouchOutsideFingerprint
import app.revanced.patches.music.flyoutpanel.shared.FlyoutPanelMenuItemPatch
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT
import app.revanced.patches.music.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceName
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Replace report",
    description = "Adds an option to replace \"Report\" with \"Playback speed\" in the flyout menu.",
    dependencies = [
        FlyoutPanelMenuItemPatch::class,
        OverrideSpeedHookPatch::class,
        ReplaceReportResourcePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object ReplaceReportPatch : BytecodePatch(
    setOf(TouchOutsideFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        FlyoutPanelMenuItemPatch.replaceComponents()

        TouchOutsideFingerprint.result?.let {
            it.mutableMethod.apply {
                val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName("setOnClickListener")
                val setOnClickListenerRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "sput-object v$setOnClickListenerRegister, $FLYOUT->touchOutSideView:Landroid/view/View;"
                )
            }
        } ?: throw TouchOutsideFingerprint.exception

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
