package app.revanced.patches.music.utils.overridespeed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedFingerprint
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedOnClickListenerFingerprint
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedParentFingerprint
import app.revanced.patches.music.utils.overridespeed.fingerprints.PlaybackSpeedPatchFingerprint
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.util.MethodUtil

object OverrideSpeedHookPatch : BytecodePatch(
    setOf(
        PlaybackSpeedOnClickListenerFingerprint,
        PlaybackSpeedPatchFingerprint,
        PlaybackSpeedParentFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"

    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    private lateinit var iGetObjectReference: Reference
    private lateinit var invokeInterfaceReference: Reference
    private lateinit var invokeVirtualReference: Reference
    private lateinit var objectClass: String

    override fun execute(context: BytecodeContext) {

        PlaybackSpeedOnClickListenerFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = getWideLiteralInstructionIndex(147448)
                val iGetObjectIndex = getTargetIndex(startIndex, Opcode.IGET_OBJECT)
                val invokeInterfaceIndex = getTargetIndex(startIndex, Opcode.INVOKE_INTERFACE)
                val invokeVirtualIndex = getTargetIndex(startIndex, Opcode.INVOKE_VIRTUAL)

                iGetObjectReference = getInstruction<ReferenceInstruction>(iGetObjectIndex).reference
                objectClass = (iGetObjectReference as FieldReference).type
                invokeInterfaceReference = getInstruction<ReferenceInstruction>(invokeInterfaceIndex).reference
                invokeVirtualReference = getInstruction<ReferenceInstruction>(invokeVirtualIndex).reference
            }

            it.mutableClass.methods.first { method ->
                MethodUtil.isConstructor(method)
            }.apply {
                val iPutIndex =
                    implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.IPUT_OBJECT &&
                                (instruction as? ReferenceInstruction)?.reference == iGetObjectReference
                    }
                val iPutRegister = getInstruction<TwoRegisterInstruction>(iPutIndex).registerA

                addInstruction(
                    iPutIndex,
                    "sput-object v$iPutRegister, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->objectClass:$objectClass"
                )
            }
        } ?: throw PlaybackSpeedOnClickListenerFingerprint.exception

        PlaybackSpeedParentFingerprint.result?.let { parentResult ->
            PlaybackSpeedFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val endIndex = it.scanResult.patternScanResult!!.endIndex

                    val speedRegister =
                        getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                    val speedMethod = context
                        .toMethodWalker(this)
                        .nextMethod(endIndex, true)
                        .getMethod() as MutableMethod

                    speedMethod.addInstruction(
                        speedMethod.implementation!!.instructions.size - 1,
                        "sput p1, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->currentSpeed:F"
                    )

                    addInstructions(
                        startIndex + 2, """
                            invoke-static {}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed()F
                            move-result v$speedRegister
                            """
                    )
                }

            } ?: throw PlaybackSpeedFingerprint.exception
        } ?: throw PlaybackSpeedParentFingerprint.exception

        PlaybackSpeedPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "objectClass",
                        objectClass,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )

                addInstructionsWithLabels(
                    0, """
                        sget-object v0, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->objectClass:$objectClass
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
        } ?: throw PlaybackSpeedPatchFingerprint.exception
    }
}