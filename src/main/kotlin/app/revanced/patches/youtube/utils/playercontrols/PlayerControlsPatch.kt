package app.revanced.patches.youtube.utils.playercontrols

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.fingerprints.ThumbnailPreviewConfigFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleToStringFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.QuickSeekVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SeekEDUVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.UserScrubbingFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerControlsPatch : BytecodePatch(
    setOf(
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        FullscreenEngagementSpeedEduVisibleToStringFingerprint,
        PlayerControlsVisibilityModelFingerprint,
        ThumbnailPreviewConfigFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val playerControlsVisibilityModelClass =
            PlayerControlsVisibilityModelFingerprint.resultOrThrow().mutableClass

        val youTubeControlsOverlayClass =
            YouTubeControlsOverlayFingerprint.resultOrThrow().mutableClass

        QuickSeekVisibleFingerprint.resolve(context, playerControlsVisibilityModelClass)
        SeekEDUVisibleFingerprint.resolve(context, playerControlsVisibilityModelClass)
        UserScrubbingFingerprint.resolve(context, playerControlsVisibilityModelClass)

        PlayerControlsVisibilityFingerprint.resolve(context, youTubeControlsOverlayClass)

        quickSeekVisibleMutableMethod = QuickSeekVisibleFingerprint.resultOrThrow().mutableMethod

        seekEDUVisibleMutableMethod = SeekEDUVisibleFingerprint.resultOrThrow().mutableMethod

        userScrubbingMutableMethod = UserScrubbingFingerprint.resultOrThrow().mutableMethod

        playerControlsVisibilityMutableMethod = PlayerControlsVisibilityFingerprint.resultOrThrow().mutableMethod

        controlsLayoutInflateResult = ControlsLayoutInflateFingerprint.resultOrThrow()

        inflateResult = BottomControlsInflateFingerprint.resultOrThrow()

        FullscreenEngagementSpeedEduVisibleToStringFingerprint.resultOrThrow().let {
            FullscreenEngagementSpeedEduVisibleFingerprint.resolve(context, it.classDef)
            fullscreenEngagementSpeedEduVisibleMutableMethod = FullscreenEngagementSpeedEduVisibleFingerprint.resultOrThrow().mutableMethod

            it.mutableMethod.apply {
                fullscreenEngagementViewVisibleReference =
                    findReference(", isFullscreenEngagementViewVisible=")
                speedEDUVisibleReference = findReference(", isSpeedmasterEDUVisible=")
            }
        }

        ThumbnailPreviewConfigFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                bigBoardsVisibilityMutableMethod = this

                addInstruction(
                    0,
                    "const/4 v0, 0x1"
                )
            }
        }
    }

    private lateinit var controlsLayoutInflateResult: MethodFingerprintResult
    private lateinit var inflateResult: MethodFingerprintResult

    private lateinit var bigBoardsVisibilityMutableMethod: MutableMethod
    private lateinit var playerControlsVisibilityMutableMethod: MutableMethod
    private lateinit var quickSeekVisibleMutableMethod: MutableMethod
    private lateinit var seekEDUVisibleMutableMethod: MutableMethod
    private lateinit var userScrubbingMutableMethod: MutableMethod

    private lateinit var fullscreenEngagementSpeedEduVisibleMutableMethod: MutableMethod
    private lateinit var fullscreenEngagementViewVisibleReference: Reference
    private lateinit var speedEDUVisibleReference: Reference

    private fun MutableMethod.findReference(targetString: String): Reference {
        val stringIndex = getStringInstructionIndex(targetString)
        if (stringIndex > 0) {
            val appendIndex = getTargetIndexWithReference(
                stringIndex,
                "Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;"
            )
            if (appendIndex > 0) {
                val booleanRegister = getInstruction<FiveRegisterInstruction>(appendIndex).registerD

                for (index in appendIndex downTo 0) {
                    val opcode = getInstruction(index).opcode
                    if (opcode != Opcode.IGET_BOOLEAN)
                        continue

                    val register = getInstruction<TwoRegisterInstruction>(index).registerA
                    if (register != booleanRegister)
                        continue

                    return getInstruction<ReferenceInstruction>(index).reference
                }
            }
        }

        throw PatchException("Reference not found: $targetString")
    }

    private fun injectBigBoardsVisibilityCall(descriptor: String) {
        bigBoardsVisibilityMutableMethod.addInstruction(
            1,
            "invoke-static {v0}, $descriptor->changeVisibilityNegatedImmediate(Z)V"
        )
    }

    private fun injectFullscreenEngagementSpeedEduViewVisibilityCall(
        reference: Reference,
        descriptor: String
    ) {
        fullscreenEngagementSpeedEduVisibleMutableMethod.apply {
            val index = getTargetIndexWithReference(reference.toString())
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstruction(
                index,
                "invoke-static {v$register}, $descriptor->changeVisibilityNegatedImmediate(Z)V"
            )
        }
    }

    private fun MutableMethod.injectVisibilityCall(
        descriptor: String,
        fieldName: String
    ) {
        addInstruction(
            0,
            "invoke-static {p1}, $descriptor->$fieldName(Z)V"
        )
    }

    private fun MethodFingerprintResult.injectCalls(
        descriptor: String
    ) {
        mutableMethod.apply {
            val endIndex = scanResult.patternScanResult!!.endIndex
            val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

            addInstruction(
                endIndex + 1,
                "invoke-static {v$viewRegister}, $descriptor->initialize(Landroid/view/View;)V"
            )
        }
    }

    internal fun injectVisibility(descriptor: String) {
        playerControlsVisibilityMutableMethod.injectVisibilityCall(
            descriptor,
            "changeVisibility"
        )
        quickSeekVisibleMutableMethod.injectVisibilityCall(
            descriptor,
            "changeVisibilityNegatedImmediate"
        )
        seekEDUVisibleMutableMethod.injectVisibilityCall(
            descriptor,
            "changeVisibilityNegatedImmediate"
        )
        userScrubbingMutableMethod.injectVisibilityCall(
            descriptor,
            "changeVisibilityNegatedImmediate"
        )

        injectBigBoardsVisibilityCall(descriptor)

        injectFullscreenEngagementSpeedEduViewVisibilityCall(
            fullscreenEngagementViewVisibleReference,
            descriptor
        )
        injectFullscreenEngagementSpeedEduViewVisibilityCall(
            speedEDUVisibleReference,
            descriptor
        )
    }

    internal fun initializeSB(descriptor: String) {
        controlsLayoutInflateResult.injectCalls(descriptor)
    }

    internal fun initializeControl(descriptor: String) {
        inflateResult.injectCalls(descriptor)
    }
}