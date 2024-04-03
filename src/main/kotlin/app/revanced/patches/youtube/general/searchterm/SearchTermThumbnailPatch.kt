package app.revanced.patches.youtube.general.searchterm

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.searchterm.fingerprints.CreateSearchSuggestionsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getTargetIndexWithReferenceReversed
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object SearchTermThumbnailPatch : BaseBytecodePatch(
    name = "Hide search term thumbnail",
    description = "Adds an option to hide thumbnails in the search term history.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(CreateSearchSuggestionsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        CreateSearchSuggestionsFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val relativeIndex = getWideLiteralInstructionIndex(40)
                val replaceIndex = getTargetIndexWithReferenceReversed(
                    relativeIndex,
                    "Landroid/widget/ImageView;->setVisibility(I)V"
                ) - 1

                val jumpIndex = getTargetIndexWithReference(
                    relativeIndex,
                    "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                ) + 4

                val replaceIndexInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                val replaceIndexReference =
                    getInstruction<ReferenceInstruction>(replaceIndex).reference

                addInstructionsWithLabels(
                    replaceIndex + 1, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                        move-result v${replaceIndexInstruction.registerA}
                        if-nez v${replaceIndexInstruction.registerA}, :hidden
                        iget-object v${replaceIndexInstruction.registerA}, v${replaceIndexInstruction.registerB}, $replaceIndexReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(replaceIndex)
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_SEARCH_TERM_THUMBNAIL"
            )
        )

        SettingsPatch.updatePatchStatus("Hide search term thumbnail")

    }
}
