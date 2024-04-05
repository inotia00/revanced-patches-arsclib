package app.revanced.patches.youtube.video.speed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.utils.fingerprints.QualityChangedFromRecyclerViewFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch.speedSelectionInsertMethod
import app.revanced.patches.youtube.video.speed.fingerprints.PlaybackSpeedChangedFromRecyclerViewFingerprint
import app.revanced.patches.youtube.video.speed.fingerprints.PlaybackSpeedInitializeFingerprint
import app.revanced.util.getTargetIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object PlaybackSpeedPatch : BaseBytecodePatch(
    name = "Default playback speed",
    description = "Adds an option to set the default playback speed.",
    dependencies = setOf(
        SettingsPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        QualityChangedFromRecyclerViewFingerprint,
        VideoEndFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"

    override fun execute(context: BytecodeContext) {

        PlaybackSpeedChangedFromRecyclerViewFingerprint.resolve(
            context,
            QualityChangedFromRecyclerViewFingerprint.resultOrThrow().classDef
        )

        val newMethod = PlaybackSpeedChangedFromRecyclerViewFingerprint.resultOrThrow().mutableMethod

        arrayOf(
            newMethod,
            speedSelectionInsertMethod
        ).forEach {
            it.apply {
                val speedSelectionValueInstructionIndex = getTargetIndex(Opcode.IGET)
                val speedSelectionValueRegister =
                    getInstruction<TwoRegisterInstruction>(speedSelectionValueInstructionIndex).registerA

                addInstruction(
                    speedSelectionValueInstructionIndex + 1,
                    "invoke-static {v$speedSelectionValueRegister}, " +
                            "$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
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

        VideoInformationPatch.cpnHook("$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Z)V")

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