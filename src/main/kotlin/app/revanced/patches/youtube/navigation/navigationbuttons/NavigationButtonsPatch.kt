package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object NavigationButtonsPatch : BaseBytecodePatch(
    name = "Hide navigation buttons",
    description = "Adds options to hide and change navigation buttons (such as the Shorts button).",
    dependencies = setOf(
        SettingsPatch::class,
        NavigationBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(AutoMotiveFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Switch create button with notifications button
         */
        AutoMotiveFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("Android Automotive") - 1
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $NAVIGATION_CLASS_DESCRIPTOR->switchCreateWithNotificationButton(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // Hook navigation button created, in order to hide them.
        NavigationBarHookPatch.hookNavigationButtonCreated(NAVIGATION_CLASS_DESCRIPTOR)


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide navigation buttons")

    }
}