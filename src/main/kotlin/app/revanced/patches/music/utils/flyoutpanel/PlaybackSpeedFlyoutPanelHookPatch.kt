package app.revanced.patches.music.utils.flyoutpanel

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patches.music.utils.flyoutpanel.fingerprints.PlaybackSpeedOnClickListenerFingerprint
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(description = "Hooks YouTube Music to open the playback speed flyout panel in the integration.")
object PlaybackSpeedFlyoutPanelHookPatch : BytecodePatch(
    setOf(PlaybackSpeedOnClickListenerFingerprint)
) {
    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        PlaybackSpeedOnClickListenerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = getWideLiteralInstructionIndex(147448)
                val iGetObjectIndex = getTargetIndex(startIndex, Opcode.IGET_OBJECT)
                val invokeInterfaceIndex = getTargetIndex(startIndex, Opcode.INVOKE_INTERFACE)
                val invokeVirtualIndex = getTargetIndex(startIndex, Opcode.INVOKE_VIRTUAL)

                val iGetObjectReference = getInstruction<ReferenceInstruction>(iGetObjectIndex).reference
                val playbackRateBottomSheetClass = (iGetObjectReference as FieldReference).type
                val invokeInterfaceReference = getInstruction<ReferenceInstruction>(invokeInterfaceIndex).reference
                val invokeVirtualReference = getInstruction<ReferenceInstruction>(invokeVirtualIndex).reference

                it.mutableClass.methods.first { method ->
                    MethodUtil.isConstructor(method)
                }.apply {
                    val iPutIndex =
                        indexOfFirstInstruction {
                            opcode == Opcode.IPUT_OBJECT &&
                                    (this as? ReferenceInstruction)?.reference == iGetObjectReference
                        }
                    val iPutRegister = getInstruction<TwoRegisterInstruction>(iPutIndex).registerA

                    addInstruction(
                        iPutIndex,
                        "sput-object v$iPutRegister, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass"
                    )
                }

                val videoUtilsMutableClass = context.findClass(
                    INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR
                )!!.mutableClass
                videoUtilsMutableClass.methods.single { method ->
                    method.name == "showPlaybackSpeedFlyoutMenu"
                }.apply {
                    // add playback rate bottom sheet class
                    videoUtilsMutableClass.staticFields.add(
                        ImmutableField(
                            definingClass,
                            "playbackRateBottomSheetClass",
                            playbackRateBottomSheetClass,
                            AccessFlags.PUBLIC or AccessFlags.STATIC,
                            null,
                            annotations,
                            null
                        ).toMutable()
                    )

                    // call playback rate bottom sheet method
                    addInstructionsWithLabels(
                        0, """
                            sget-object v0, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass
                            if-eqz v0, :ignore
                            invoke-interface {v0}, $invokeInterfaceReference
                            move-result-object v0
                            check-cast v0, ${(invokeVirtualReference as MethodReference).definingClass}
                            invoke-virtual {v0}, $invokeVirtualReference
                            :ignore
                            return-void
                            """
                    )
                }
            }
        }
    }
}
