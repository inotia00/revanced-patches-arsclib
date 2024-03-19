package app.revanced.patches.music.player.colormatchplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.fingerprints.SwitchToggleColorFingerprint
import app.revanced.patches.music.utils.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ColorGrey
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch(
    name = "Enable color match player",
    description = "Adds an option to match the color of the miniplayer to the fullscreen player.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
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
object ColorMatchPlayerPatch : BytecodePatch(
    setOf(MiniPlayerConstructorFingerprint)
) {
    private lateinit var invokeVirtualReference: Reference
    private lateinit var iGetReference: Reference
    private lateinit var iPutReference: Reference
    private lateinit var methodParameter: List<MethodParameter>

    override fun execute(context: BytecodeContext) {

        MiniPlayerConstructorFingerprint.result?.let { parentResult ->
            // Resolves fingerprints
            SwitchToggleColorFingerprint.resolve(context, parentResult.classDef)

            SwitchToggleColorFingerprint.result?.let {
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
                                invoke-static {}, $PLAYER->enableColorMatchPlayer()Z
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
            } ?: throw SwitchToggleColorFingerprint.exception
        } ?: throw MiniPlayerConstructorFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_color_match_player",
            "true"
        )

    }
}