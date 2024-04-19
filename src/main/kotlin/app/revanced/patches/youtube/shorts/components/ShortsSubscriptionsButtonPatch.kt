package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsSubscriptionsFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsSubscriptionsTabletFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsSubscriptionsTabletParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPlayerFooter
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPlayerPausedStateButton
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(dependencies = [SettingsPatch::class])
object ShortsSubscriptionsButtonPatch : BytecodePatch(
    setOf(
        ShortsSubscriptionsFingerprint,
        ShortsSubscriptionsTabletParentFingerprint
    )
) {
    private lateinit var subscriptionFieldReference: FieldReference

    override fun execute(context: BytecodeContext) {
        ShortsSubscriptionsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralInstructionIndex(ReelPlayerPausedStateButton) + 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSubscriptionsButton(Landroid/view/View;)V"
                )
            }
        }

        /**
         * Deprecated in YouTube v18.31.xx+
         */
        if (!SettingsPatch.upward1831) {
            ShortsSubscriptionsTabletParentFingerprint.resultOrThrow().let { parentResult ->
                parentResult.mutableMethod.apply {
                    val targetIndex = getWideLiteralInstructionIndex(ReelPlayerFooter) - 1
                    subscriptionFieldReference =
                        (getInstruction<ReferenceInstruction>(targetIndex)).reference as FieldReference
                }

                ShortsSubscriptionsTabletFingerprint.also {
                    it.resolve(
                        context,
                        parentResult.classDef
                    )
                }.resultOrThrow().mutableMethod.apply {
                    implementation!!.instructions.filter { instruction ->
                        val fieldReference =
                            (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        instruction.opcode == Opcode.IGET
                                && fieldReference == subscriptionFieldReference
                    }.forEach { instruction ->
                        val insertIndex = implementation!!.instructions.indexOf(instruction) + 1
                        val register = (instruction as TwoRegisterInstruction).registerA

                        addInstructions(
                            insertIndex, """
                                invoke-static {v$register}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSubscriptionsButton(I)I
                                move-result v$register
                                """
                        )
                    }
                }
            }
        }
    }
}
