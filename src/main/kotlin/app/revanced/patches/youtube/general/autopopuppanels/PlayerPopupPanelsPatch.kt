package app.revanced.patches.youtube.general.autopopuppanels

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.autopopuppanels.fingerprints.EngagementPanelControllerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object PlayerPopupPanelsPatch : BaseBytecodePatch(
    name = "Hide auto player popup panels",
    description = "Adds an option to hide panels (such as live chat) from opening automatically.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(EngagementPanelControllerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        EngagementPanelControllerFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideAutoPlayerPopupPanels()Z
                        move-result v0
                        if-eqz v0, :shown
                        # The type of the fourth parameter is boolean.
                        if-eqz p4, :shown
                        const/4 v0, 0x0
                        return-object v0
                        """, ExternalLabel("shown", getInstruction(0))
                )
            }
        } ?: throw EngagementPanelControllerFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_AUTO_PLAYER_POPUP_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide auto player popup panels")

    }
}
