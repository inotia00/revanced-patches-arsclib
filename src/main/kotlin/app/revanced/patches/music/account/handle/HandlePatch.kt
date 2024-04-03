package app.revanced.patches.music.account.handle

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.music.account.handle.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.music.account.handle.fingerprints.NamesInactiveAccountThumbnailSizeFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACCOUNT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object HandlePatch : BaseBytecodePatch(
    name = "Hide handle",
    description = "Adds an option to hide the handle in the account menu.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountSwitcherAccessibilityLabelFingerprint,
        NamesInactiveAccountThumbnailSizeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hide handle in account menu
         */
        AccountSwitcherAccessibilityLabelFingerprint.result?.let { result ->
            result.mutableMethod.apply {

                val textColorIndex = getTargetIndexWithMethodReferenceName("setTextColor")
                val setVisibilityIndex = getTargetIndexWithMethodReferenceName(textColorIndex, "setVisibility")
                val textViewInstruction = getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                replaceInstruction(
                    setVisibilityIndex,
                    "invoke-static {v${textViewInstruction.registerC}, v${textViewInstruction.registerD}}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Landroid/widget/TextView;I)V"
                )
            }
        } ?: throw AccountSwitcherAccessibilityLabelFingerprint.exception

        /**
         * Hide handle in account switcher
         */
        NamesInactiveAccountThumbnailSizeFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw NamesInactiveAccountThumbnailSizeFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_handle",
            "true"
        )

    }
}
