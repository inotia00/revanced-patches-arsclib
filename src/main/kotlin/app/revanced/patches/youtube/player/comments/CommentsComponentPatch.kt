package app.revanced.patches.youtube.player.comments

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.comments.fingerprints.ShortsLiveStreamEmojiPickerOnClickListenerFingerprint
import app.revanced.patches.youtube.player.comments.fingerprints.ShortsLiveStreamEmojiPickerOpacityFingerprint
import app.revanced.patches.youtube.player.comments.fingerprints.ShortsLiveStreamThanksFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object CommentsComponentPatch : BaseBytecodePatch(
    name = "Hide comments component",
    description = "Adds options to hide components related to comments.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ShortsLiveStreamEmojiPickerOnClickListenerFingerprint,
        ShortsLiveStreamEmojiPickerOpacityFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CommentsFilter;"
    override fun execute(context: BytecodeContext) {

        // region patch for emoji picker button in shorts

        ShortsLiveStreamEmojiPickerOpacityFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister= getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeEmojiPickerOpacity(Landroid/widget/ImageView;)V"
                )
            }
        }

        val shortsLiveStreamEmojiPickerOnClickListenerResult =
            ShortsLiveStreamEmojiPickerOnClickListenerFingerprint.resultOrThrow()

        shortsLiveStreamEmojiPickerOnClickListenerResult.let {
            it.mutableMethod.apply {
                val emojiPickerEndpointIndex = getWideLiteralInstructionIndex(126326492)
                val emojiPickerOnClickListenerIndex = getTargetIndex(emojiPickerEndpointIndex, Opcode.INVOKE_DIRECT)
                val emojiPickerOnClickListenerMethod = getWalkerMethod(context, emojiPickerOnClickListenerIndex)

                emojiPickerOnClickListenerMethod.apply {
                    val insertIndex = getTargetIndex(Opcode.IF_EQZ)
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->disableEmojiPickerOnClickListener(Ljava/lang/Object;)Ljava/lang/Object;
                            move-result-object v$insertRegister
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for thanks button in shorts

        ShortsLiveStreamThanksFingerprint.resolve(context, shortsLiveStreamEmojiPickerOnClickListenerResult.classDef)
        ShortsLiveStreamThanksFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                addInstructions(
                    insertIndex,"""
                        invoke-static { v${insertInstruction.registerC}, v${insertInstruction.registerD} }, $PLAYER_CLASS_DESCRIPTOR->hideThanksButton(Landroid/view/View;I)I
                        move-result v${insertInstruction.registerD}
                        """
                )
            }
        }

        // endregion

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_COMMENTS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
