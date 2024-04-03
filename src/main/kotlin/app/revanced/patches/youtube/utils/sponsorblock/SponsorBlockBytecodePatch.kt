package app.revanced.patches.youtube.utils.sponsorblock

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.SeekbarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.playercontrols.PlayerControlsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InsetOverlayViewLayout
import app.revanced.patches.youtube.utils.sponsorblock.fingerprints.RectangleFieldInvalidatorFingerprint
import app.revanced.patches.youtube.utils.sponsorblock.fingerprints.SegmentPlaybackControllerFingerprint
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.patches.youtube.utils.videoid.withoutshorts.VideoIdWithoutShortsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithFieldReferenceTypeReversed
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getTargetIndexWithMethodReferenceNameReversed
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(
    dependencies = [
        OverrideSpeedHookPatch::class,
        PlayerControlsPatch::class,
        SharedResourceIdPatch::class,
        VideoIdPatch::class,
        VideoIdWithoutShortsPatch::class
    ]
)
object SponsorBlockBytecodePatch : BytecodePatch(
    setOf(
        SeekbarFingerprint,
        SegmentPlaybackControllerFingerprint,
        TotalTimeFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_SPONSOR_BLOCK_PATH =
        "$INTEGRATIONS_PATH/sponsorblock"

    private const val INTEGRATIONS_SPONSOR_BLOCK_UI_PATH =
        "$INTEGRATIONS_SPONSOR_BLOCK_PATH/ui"

    private const val INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_SPONSOR_BLOCK_PATH/SegmentPlaybackController;"

    override fun execute(context: BytecodeContext) {

        VideoIdPatch.apply {
            // Hook the video time method
            videoTimeHook(
                INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
            // Initialize SponsorBlock
            onCreateHook(
                INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
                "initialize"
            )
        }

        val seekBarClass = SeekbarFingerprint.resultOrThrow().mutableClass
        SeekbarOnDrawFingerprint.resolve(context, seekBarClass)
        RectangleFieldInvalidatorFingerprint.resolve(context, seekBarClass)

        SeekbarOnDrawFingerprint.resultOrThrow().mutableMethod.apply {
            // Get left and right of seekbar rectangle
            val moveObjectIndex = getTargetIndex(Opcode.MOVE_OBJECT_FROM16)

            addInstruction(
                moveObjectIndex + 1,
                "invoke-static/range {p0 .. p0}, " +
                        "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )

            // Set seekbar thickness
            val roundIndex = getTargetIndexWithMethodReferenceName("round") + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA

            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = getTargetIndexWithMethodReferenceNameReversed("drawCircle")
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }

        // Voting & Shield button
        arrayOf("CreateSegmentButtonController;", "VotingButtonController;").forEach { className ->
            PlayerControlsPatch.initializeSB("$INTEGRATIONS_SPONSOR_BLOCK_UI_PATH/$className")
            PlayerControlsPatch.injectVisibility("$INTEGRATIONS_SPONSOR_BLOCK_UI_PATH/$className")
        }

        // Append timestamp
        TotalTimeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("getString") + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        // Initialize the SponsorBlock view
        YouTubeControlsOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(InsetOverlayViewLayout)
                val checkCastIndex = getTargetIndex(targetIndex, Opcode.CHECK_CAST)
                val targetRegister = getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_SPONSOR_BLOCK_UI_PATH/SponsorBlockViewController;->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        }

        // Replace strings
        RectangleFieldInvalidatorFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val invalidateIndex = getTargetIndexWithMethodReferenceNameReversed("invalidate")
                val rectangleIndex = getTargetIndexWithFieldReferenceTypeReversed(invalidateIndex + 1, "Landroid/graphics/Rect;")
                val rectangleFieldName = (getInstruction<ReferenceInstruction>(rectangleIndex).reference as FieldReference).name

                SegmentPlaybackControllerFingerprint.resultOrThrow().let {
                    it.mutableMethod.apply {
                        val replaceIndex = it.scanResult.patternScanResult!!.startIndex
                        val replaceRegister =
                            getInstruction<OneRegisterInstruction>(replaceIndex).registerA

                        replaceInstruction(
                            replaceIndex,
                            "const-string v$replaceRegister, \"$rectangleFieldName\""
                        )
                    }
                }
            }
        }

        // Inject VideoIdPatch
        VideoIdWithoutShortsPatch.injectCall("$INTEGRATIONS_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")
    }
}