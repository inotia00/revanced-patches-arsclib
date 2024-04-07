package app.revanced.patches.music.flyoutpanel.component

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.flyoutpanel.component.fingerprints.EndButtonsContainerFingerprint
import app.revanced.patches.music.flyoutpanel.component.fingerprints.SleepTimerFingerprint
import app.revanced.patches.music.flyoutpanel.shared.FlyoutPanelMenuItemPatch
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.EndButtonsContainer
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object FlyoutPanelPatch : BaseBytecodePatch(
    name = "Hide flyout panel",
    description = "Adds options to hide flyout panel components.",
    dependencies = setOf(
        FlyoutPanelMenuItemPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        EndButtonsContainerFingerprint,
        SleepTimerFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerFlyoutPanelsFilter;"

    override fun execute(context: BytecodeContext) {
        FlyoutPanelMenuItemPatch.hideComponents()

        EndButtonsContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = getWideLiteralInstructionIndex(EndButtonsContainer)
                val targetIndex = getTargetIndex(startIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FLYOUT_CLASS_DESCRIPTOR->hideLikeDislikeContainer(Landroid/view/View;)V"
                )
            }
        }

        /**
         * Forces sleep timer menu to be enabled.
         * This method may be desperate in the future.
         */
        SleepTimerFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        if (SettingsPatch.upward0636) {
            LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

            SettingsPatch.addSwitchPreference(
                CategoryType.FLYOUT,
                "revanced_hide_flyout_panel_3_column_component",
                "false"
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_add_to_queue",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_captions",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_delete_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_dismiss_queue",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_download",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_edit_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_album",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_artist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_episode",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_podcast",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_help",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_like_dislike",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_play_next",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_quality",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_remove_from_library",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_remove_from_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_report",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_save_episode_for_later",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_save_to_library",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_save_to_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_share",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_shuffle_play",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_sleep_timer",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_start_radio",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_stats_for_nerds",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_subscribe",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_view_song_credit",
            "false",
            false
        )
    }
}
