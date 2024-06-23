package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.FrequentUpdatesSheetScreenFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.RedditAlertDialogsFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Remove subreddit dialog")
@Description("Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class SubRedditDialogPatch : BytecodePatch(
    listOf(
        FrequentUpdatesSheetScreenFingerprint,
        RedditAlertDialogsFingerprint
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/RemoveSubRedditDialogPatch;"
    }

    override fun execute(context: BytecodeContext) {

        FrequentUpdatesSheetScreenFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val cancelButtonViewIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val cancelButtonViewRegister =
                    getInstruction<OneRegisterInstruction>(cancelButtonViewIndex).registerA

                addInstruction(
                    cancelButtonViewIndex + 1,
                    "invoke-static {v$cancelButtonViewRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->dismissDialog(Landroid/view/View;)V"
                )
            }
        }

        RedditAlertDialogsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val backgroundTintIndex = RedditAlertDialogsFingerprint.indexOfSetBackgroundTintListInstruction(this)
                val insertIndex =
                    getTargetIndexWithMethodReferenceNameOrThrow(backgroundTintIndex, "setTextAppearance")
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/TextView;)V"
                )
            }
        }

        updateSettingsStatus("enableSubRedditDialog")

    }
}
