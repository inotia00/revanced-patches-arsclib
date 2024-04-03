package app.revanced.patches.youtube.video.speed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.utils.fingerprints.NewVideoQualityChangedFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videocpn.VideoCpnPatch
import app.revanced.patches.youtube.video.speed.fingerprints.NewPlaybackSpeedChangedFingerprint
import app.revanced.patches.youtube.video.speed.fingerprints.PlaybackSpeedInitializeFingerprint
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object PlaybackSpeedPatch : BaseBytecodePatch(
    name = "Default playback speed",
    description = "Adds an option to set the default playback speed.",
    dependencies = setOf(
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        VideoCpnPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        NewVideoQualityChangedFingerprint,
        VideoEndFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"

    override fun execute(context: BytecodeContext) {

        NewVideoQualityChangedFingerprint.resultOrThrow().let { parentResult ->
            NewPlaybackSpeedChangedFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.resultOrThrow().let { result ->
                arrayOf(result, OverrideSpeedHookPatch.playbackSpeedChangedResult).forEach {
                    it.mutableMethod.apply {
                        val index = it.scanResult.patternScanResult!!.endIndex
                        val register = getInstruction<FiveRegisterInstruction>(index).registerD

                        addInstruction(
                            index,
                            "invoke-static {v$register}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                        )
                    }
                }
            }
        }

        VideoEndFingerprint.resultOrThrow().let { parentResult ->
            PlaybackSpeedInitializeFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeedInShorts(F)F
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        VideoCpnPatch.injectCall("$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: VIDEO_EXPERIMENTAL_FLAGS",
                "SETTINGS: DEFAULT_PLAYBACK_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("Default playback speed")

        context.updatePatchStatus("$UTILS_PATH/PatchStatus;", "DefaultPlaybackSpeed")

    }
}