package app.revanced.patches.music.actionbar.component

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.actionbar.component.fingerprints.ActionBarComponentFingerprint
import app.revanced.patches.music.actionbar.component.fingerprints.LikeDislikeContainerFingerprint
import app.revanced.patches.music.actionbar.component.fingerprints.LikeDislikeContainerVisibilityFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACTIONBAR_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.LikeDislikeContainer
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import kotlin.math.min

@Suppress("unused")
object ActionBarComponentPatch : BaseBytecodePatch(
    name = "Hide action bar component",
    description = "Adds options to hide action bar components and replace the offline download button with an external download button.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ActionBarComponentFingerprint,
        LikeDislikeContainerFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        ActionBarComponentFingerprint.result?.let {
            it.mutableMethod.apply {

                // hook download button
                val addViewIndex = getTargetIndexWithMethodReferenceName("addView")
                val addViewRegister = getInstruction<FiveRegisterInstruction>(addViewIndex).registerD

                addInstruction(
                    addViewIndex + 1,
                    "invoke-static {v$addViewRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->hookDownloadButton(Landroid/view/View;)V"
                )

                // hide action button label
                val noLabelIndex = indexOfFirstInstruction {
                    val reference = (this as? ReferenceInstruction)?.reference.toString()
                    opcode == Opcode.INVOKE_DIRECT
                            && reference.endsWith("<init>(Landroid/content/Context;)V")
                            && !reference.contains("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;")
                } - 2
                val replaceIndex = indexOfFirstInstruction {
                    val reference = (this as? ReferenceInstruction)?.reference.toString()
                    opcode == Opcode.INVOKE_DIRECT
                            && reference.endsWith("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;-><init>(Landroid/content/Context;)V")
                } - 2
                val replaceInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                val replaceReference = getInstruction<ReferenceInstruction>(replaceIndex).reference

                addInstructionsWithLabels(
                    replaceIndex + 1, """
                        invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionBarLabel()Z
                        move-result v${replaceInstruction.registerA}
                        if-nez v${replaceInstruction.registerA}, :hidden
                        iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                        """, ExternalLabel("hidden", getInstruction(noLabelIndex))
                )
                removeInstruction(replaceIndex)

                // hide action button
                val hasNextIndex = getTargetIndexWithMethodReferenceName("hasNext")
                val freeRegister = min(implementation!!.registerCount - parameters.size - 2, 15)

                val spannedIndex = getTargetIndexWithReference(")Landroid/text/Spanned;")
                val spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
                val spannedReference = getInstruction<ReferenceInstruction>(spannedIndex).reference

                addInstructionsWithLabels(
                    spannedIndex + 1, """
                        invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionButton()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hidden
                        invoke-static {v$spannedRegister}, $spannedReference
                        """, ExternalLabel("hidden", getInstruction(hasNextIndex))
                )
                removeInstruction(spannedIndex)

                // set action button identifier
                val buttonTypeDownloadIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val buttonTypeDownloadRegister = getInstruction<OneRegisterInstruction>(buttonTypeDownloadIndex).registerA

                val buttonTypeIndex = it.scanResult.patternScanResult!!.endIndex - 1
                val buttonTypeRegister = getInstruction<OneRegisterInstruction>(buttonTypeIndex).registerA

                addInstruction(
                    buttonTypeIndex + 2,
                    "invoke-static {v$buttonTypeRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonType(Ljava/lang/Object;)V"
                )

                addInstruction(
                    buttonTypeDownloadIndex,
                    "invoke-static {v$buttonTypeDownloadRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonTypeDownload(I)V"
                )
            }
        } ?: throw ActionBarComponentFingerprint.exception

        LikeDislikeContainerFingerprint.result?.let { parentResult ->
            // Resolves fingerprints
            LikeDislikeContainerVisibilityFingerprint.resolve(context, parentResult.classDef)

            /**
             * Added in YouTube Music v6.35.xx~
             */
            LikeDislikeContainerVisibilityFingerprint.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->hideLikeDislikeButton(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            } // Don't throw exception

            parentResult.mutableMethod.apply {
                val insertIndex = getWideLiteralInstructionIndex(LikeDislikeContainer) + 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$insertRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->hideLikeDislikeButton(Landroid/view/View;)V"
                )
            }
        } ?: throw LikeDislikeContainerFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_add_to_playlist",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_comment",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_download",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_label",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_like_dislike",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_radio",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_share",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hook_action_button_download",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.ACTION_BAR,
            "revanced_external_downloader_package_name",
            "revanced_hook_action_button_download"
        )

    }
}