package app.revanced.patches.music.misc.premium

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.misc.premium.fingerprints.AccountMenuFooterFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.HideGetPremiumFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.MembershipSettingsFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.MembershipSettingsParentFingerprint
import app.revanced.patches.music.navigation.component.NavigationBarComponentPatch
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PrivacyTosFooter
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object GetPremiumPatch : BaseBytecodePatch(
    name = "Hide get premium",
    description = "Hides the \"Get Music Premium\" label from the account menu and settings.",
    dependencies = setOf(
        NavigationBarComponentPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountMenuFooterFingerprint,
        HideGetPremiumFingerprint,
        MembershipSettingsParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // Hides get premium button at the bottom of the account switching menu
        HideGetPremiumFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        }

        // Hides get premium button at the top of the account switching menu
        AccountMenuFooterFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(PrivacyTosFooter)
                val walkerIndex = getTargetIndex(constIndex + 2, Opcode.INVOKE_VIRTUAL)
                val viewIndex = getTargetIndex(constIndex, Opcode.IGET_OBJECT)
                val viewReference = getInstruction<ReferenceInstruction>(viewIndex).reference.toString()

                val walkerMethod = getWalkerMethod(context, walkerIndex)
                walkerMethod.apply {
                    val insertIndex = getTargetIndexWithReference(viewReference)
                    val nullCheckIndex = getTargetIndex(insertIndex - 1, Opcode.IF_NEZ)
                    val nullCheckRegister = getInstruction<OneRegisterInstruction>(nullCheckIndex).registerA

                    addInstruction(
                        nullCheckIndex,
                        "const/4 v$nullCheckRegister, 0x0"
                    )
                }
            }
        }

        // Hides premium membership menu in settings
        MembershipSettingsParentFingerprint.resultOrThrow().classDef.let { classDef ->
            MembershipSettingsFingerprint.resolve(context, classDef)
            MembershipSettingsFingerprint.resultOrThrow().mutableMethod.addInstructions(
                0, """
                    const/4 v0, 0x0
                    return-object v0
                    """
            )
        }

    }
}
