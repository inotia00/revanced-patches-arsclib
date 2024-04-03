package app.revanced.patches.music.account.component

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.account.component.fingerprints.MenuEntryFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACCOUNT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object MenuComponentPatch : BaseBytecodePatch(
    name = "Hide account menu",
    description = "Adds the ability to hide account menu elements using a custom filter.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(MenuEntryFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MenuEntryFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val textIndex = getTargetIndexWithMethodReferenceName("setText")
                val viewIndex = getTargetIndexWithMethodReferenceName("addView")

                val textRegister = getInstruction<FiveRegisterInstruction>(textIndex).registerD
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister, v$viewRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideAccountMenu(Ljava/lang/CharSequence;Landroid/view/View;)V"
                )
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_filter_strings",
            "revanced_hide_account_menu"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_empty_component",
            "false",
            "revanced_hide_account_menu"
        )
    }
}
