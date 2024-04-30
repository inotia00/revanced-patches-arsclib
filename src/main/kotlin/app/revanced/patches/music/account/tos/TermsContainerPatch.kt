package app.revanced.patches.music.account.tos

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.account.tos.fingerprints.TermsOfServiceFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.ACCOUNT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object TermsContainerPatch : BaseBytecodePatch(
    name = "Hide terms container",
    description = "Adds an option to hide the terms of service container in the account menu.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TermsOfServiceFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TermsOfServiceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithReference("/PrivacyTosFooter;->setVisibility(I)V")
                val visibilityRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$visibilityRegister, 0x0"
                )
                addInstructions(
                    insertIndex, """
                        invoke-static {}, $ACCOUNT_CLASS_DESCRIPTOR->hideTermsContainer()I
                        move-result v$visibilityRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_terms_container",
            "false"
        )

    }
}
