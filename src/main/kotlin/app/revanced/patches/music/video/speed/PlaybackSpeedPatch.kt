package app.revanced.patches.music.video.speed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetFingerprint
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetParentFingerprint
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object PlaybackSpeedPatch : BaseBytecodePatch(
    name = "Remember playback speed",
    description = "Adds an option to remember the last playback speed selected.",
    dependencies = setOf(
        SettingsPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PlaybackSpeedBottomSheetParentFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"

    override fun execute(context: BytecodeContext) {

        PlaybackSpeedBottomSheetFingerprint.resolve(
            context,
            PlaybackSpeedBottomSheetParentFingerprint.resultOrThrow().classDef
        )
        PlaybackSpeedBottomSheetFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(targetIndex).registerD

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        VideoInformationPatch.playbackSpeedResult.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val speedRegister =
                    getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                addInstructions(
                    startIndex + 2, """
                        invoke-static {v$speedRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$speedRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_enable_save_playback_speed",
            "true"
        )
    }
}