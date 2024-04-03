package app.revanced.patches.music.player.colormatchplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.utils.fingerprints.SwitchToggleColorFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ColorGrey
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
object ColorMatchPlayerPatch : BaseBytecodePatch(
    name = "Enable color match player",
    description = "Adds an option to match the color of the miniplayer to the fullscreen player.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(MiniPlayerConstructorFingerprint)
) {
    private lateinit var invokeVirtualReference: Reference
    private lateinit var iGetReference: Reference
    private lateinit var iPutReference: Reference
    private lateinit var methodParameter: List<MethodParameter>

    override fun execute(context: BytecodeContext) {

        MiniPlayerConstructorFingerprint.resultOrThrow().let { parentResult ->
            // Resolves fingerprints
            SwitchToggleColorFingerprint.resolve(context, parentResult.classDef)

            SwitchToggleColorFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    methodParameter = parameters

                    val relativeIndex = it.scanResult.patternScanResult!!.endIndex + 1
                    val invokeVirtualIndex = getTargetIndex(relativeIndex, Opcode.INVOKE_VIRTUAL)
                    val iGetIndex = getTargetIndex(relativeIndex, Opcode.IGET)

                    invokeVirtualReference = getInstruction<ReferenceInstruction>(invokeVirtualIndex).reference
                    iGetReference = getInstruction<ReferenceInstruction>(iGetIndex).reference
                }

                parentResult.mutableMethod.apply {
                    val colorGreyIndex = getWideLiteralInstructionIndex(ColorGrey)
                    val iPutIndex = getTargetIndex(colorGreyIndex, Opcode.IPUT)

                    iPutReference = getInstruction<ReferenceInstruction>(iPutIndex).reference
                }

                parentResult.mutableClass.methods.filter { method ->
                    method.accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL
                            && method.parameters == methodParameter
                            && method.returnType == "V"
                }.forEach { mutableMethod ->
                    mutableMethod.apply {
                        val freeRegister = implementation!!.registerCount - parameters.size - 3

                        val invokeDirectIndex = getTargetIndexReversed(implementation!!.instructions.size - 1, Opcode.INVOKE_DIRECT)
                        val invokeDirectReference = getInstruction<ReferenceInstruction>(invokeDirectIndex).reference

                        addInstructionsWithLabels(
                            invokeDirectIndex + 1, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableColorMatchPlayer()Z
                                move-result v$freeRegister
                                if-eqz v$freeRegister, :off
                                invoke-virtual {p1}, $invokeVirtualReference
                                move-result-object v$freeRegister
                                check-cast v$freeRegister, ${(iGetReference as FieldReference).definingClass}
                                iget v$freeRegister, v$freeRegister, $iGetReference
                                iput v$freeRegister, p0, $iPutReference
                                :off
                                invoke-direct {p0}, $invokeDirectReference
                                """
                        )
                        removeInstruction(invokeDirectIndex)
                    }
                }
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_color_match_player",
            "true"
        )

    }
}