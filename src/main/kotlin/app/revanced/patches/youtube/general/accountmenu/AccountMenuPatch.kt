package app.revanced.patches.youtube.general.accountmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountListFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountListParentFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountMenuFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountMenuParentFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountMenuPatchFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.SetViewGroupMarginFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object AccountMenuPatch : BaseBytecodePatch(
    name = "Hide account menu",
    description = "Adds the ability to hide account menu elements using a custom filter in the account menu and You tab.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountListParentFingerprint,
        AccountMenuParentFingerprint,
        AccountMenuPatchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        AccountListParentFingerprint.resultOrThrow().let { parentResult ->
            AccountListFingerprint.resolve(context, parentResult.classDef)

            AccountListFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 3
                    val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                    addInstruction(
                        targetIndex,
                        "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                                "$GENERAL_CLASS_DESCRIPTOR->hideAccountList(Landroid/view/View;Ljava/lang/CharSequence;)V"
                    )
                }
            }
        }

        AccountMenuParentFingerprint.resultOrThrow().let { parentResult ->
            AccountMenuFingerprint.resolve(context, parentResult.classDef)
            SetViewGroupMarginFingerprint.resolve(context, parentResult.classDef)

            AccountMenuFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 2
                    val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                    addInstruction(
                        targetIndex,
                        "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                                "$GENERAL_CLASS_DESCRIPTOR->hideAccountMenu(Landroid/view/View;Ljava/lang/CharSequence;)V"
                    )
                }
            }

            SetViewGroupMarginFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val setViewGroupMarginIndex = it.scanResult.patternScanResult!!.startIndex
                    val setViewGroupMarginReference =
                        getInstruction<ReferenceInstruction>(setViewGroupMarginIndex).reference

                    AccountMenuPatchFingerprint.resultOrThrow().mutableMethod.addInstructions(
                        0, """
                            const/4 v0, 0x0
                            invoke-static {p0, v0, v0}, $setViewGroupMarginReference
                            """
                    )
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_ACCOUNT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus("Hide account menu")

    }
}