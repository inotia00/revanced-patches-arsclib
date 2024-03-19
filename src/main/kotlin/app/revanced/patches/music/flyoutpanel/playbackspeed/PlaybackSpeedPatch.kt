package app.revanced.patches.music.flyoutpanel.playbackspeed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.flyoutpanel.playbackspeed.fingerprints.TouchOutsideFingerprint
import app.revanced.patches.music.utils.flyoutbutton.FlyoutButtonContainerPatch
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT
import app.revanced.patches.music.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Enable playback speed",
    description = "Adds an option to add a playback speed button to the flyout panel.",
    dependencies = [
        FlyoutButtonContainerPatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
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
object PlaybackSpeedPatch : BytecodePatch(
    setOf(TouchOutsideFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TouchOutsideFingerprint.result?.let {
            it.mutableMethod.apply {
                val setOnClickListenerIndex =
                    implementation!!.instructions.indexOfFirst { instruction ->
                        ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "setOnClickListener"
                    }
                val setOnClickListenerRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "sput-object v$setOnClickListenerRegister, $FLYOUT->touchOutSideView:Landroid/view/View;"
                )
            }
        } ?: throw TouchOutsideFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_enable_flyout_panel_playback_speed",
            "false"
        )

    }
}
