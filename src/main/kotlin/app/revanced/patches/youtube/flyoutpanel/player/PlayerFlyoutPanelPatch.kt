package app.revanced.patches.youtube.flyoutpanel.player

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.AdvancedQualityBottomSheetFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.CaptionsBottomSheetFingerprint
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.FLYOUT_PANEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomSheetFooterText
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionViewHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object PlayerFlyoutPanelPatch : BaseBytecodePatch(
    name = "Hide player flyout panel",
    description = "Adds options to hide player flyout panel components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AdvancedQualityBottomSheetFingerprint,
        CaptionsBottomSheetFingerprint,
        QualityMenuViewInflateFingerprint
    )
) {
    private const val PANELS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerFlyoutPanelsFilter;"

    private const val PANELS_FOOTER_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerFlyoutPanelsFooterFilter;"

    override fun execute(context: BytecodeContext) {
        arrayOf(
            AdvancedQualityBottomSheetFingerprint to "hideFooterQuality",
            CaptionsBottomSheetFingerprint to "hideFooterCaptions",
            QualityMenuViewInflateFingerprint to "hideFooterQuality"
        ).map { (fingerprint, name) ->
            fingerprint.literalInstructionViewHook(BottomSheetFooterText, "$FLYOUT_PANEL_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V")
        }

        LithoFilterPatch.addFilter(PANELS_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(PANELS_FOOTER_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: PLAYER_FLYOUT_PANEL_HEADER",
                "SETTINGS: PLAYER_FLYOUT_PANEL_ADDITIONAL_SETTINGS_HEADER",
                "SETTINGS: HIDE_PLAYER_FLYOUT_PANEL"
            )
        )

        SettingsPatch.updatePatchStatus("Hide player flyout panel")

    }
}
