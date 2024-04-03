package app.revanced.patches.music.video.speed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetFingerprint
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetParentFingerprint
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object PlaybackSpeedPatch : BaseBytecodePatch(
    name = "Remember playback speed",
    description = "Adds an option to remember the last playback speed selected.",
    dependencies = setOf(
        OverrideSpeedHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PlaybackSpeedBottomSheetParentFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"

    override fun execute(context: BytecodeContext) {

        PlaybackSpeedBottomSheetParentFingerprint.result?.let { parentResult ->
            PlaybackSpeedBottomSheetFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<FiveRegisterInstruction>(targetIndex).registerD

                    addInstruction(
                        targetIndex,
                        "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                    )
                }
            } ?: throw PlaybackSpeedBottomSheetFingerprint.exception
        } ?: throw PlaybackSpeedBottomSheetParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.VIDEO,
            "revanced_enable_save_playback_speed",
            "true"
        )
    }
}