package app.revanced.patches.reddit.layout.trendingtoday

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.trendingtoday.fingerprints.SearchTypeaheadListDefaultPresentationConstructorFingerprint
import app.revanced.patches.reddit.layout.trendingtoday.fingerprints.SearchTypeaheadListDefaultPresentationToStringFingerprint
import app.revanced.patches.reddit.layout.trendingtoday.fingerprints.TrendingTodayItemFingerprint
import app.revanced.patches.reddit.layout.trendingtoday.fingerprints.TrendingTodayItemLegacyFingerprint
import app.revanced.patches.reddit.layout.trendingtoday.fingerprints.TrendingTodayTitleFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_13_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.getInstruction
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide Trending Today shelf")
@Description("Adds an option to hide the Trending Today shelf from search suggestions.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class TrendingTodayShelfPatch : BytecodePatch(
    listOf(
        SearchTypeaheadListDefaultPresentationToStringFingerprint,
        TrendingTodayItemFingerprint,
        TrendingTodayItemLegacyFingerprint,
        TrendingTodayTitleFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/TrendingTodayShelfPatch;"

        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            INTEGRATIONS_CLASS_DESCRIPTOR +
                    "->" +
                    "hideTrendingTodayShelf()Z"
    }

    override fun execute(context: BytecodeContext) {

        // region patch for hide trending today title.

        TrendingTodayTitleFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("trending_today_title")
                val relativeIndex =
                    indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.AND_INT_LIT8)
                val insertIndex = indexOfFirstInstructionReversedOrThrow(
                    relativeIndex + 1,
                    Opcode.MOVE_OBJECT_FROM16
                )
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpOpcode = if (returnType == "V") Opcode.RETURN_VOID else Opcode.SGET_OBJECT
                var jumpIndex = indexOfFirstInstructionReversedOrThrow(jumpOpcode)
                if (jumpOpcode == Opcode.SGET_OBJECT && getInstruction(jumpIndex + 1).opcode != Opcode.RETURN_OBJECT) {
                    jumpIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
                }

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, listOf(ExternalLabel("hidden", getInstruction(jumpIndex)))
                )
            }
        }

        if (is_2025_13_or_greater) {
            SearchTypeaheadListDefaultPresentationConstructorFingerprint.alsoResolve(
                context, SearchTypeaheadListDefaultPresentationToStringFingerprint
            ).mutableMethod.addInstructions(
                1, """
                    invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->removeTrendingLabel(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    """
            )
        }

        // endregion

        // region patch for hide trending today contents.

        val trendingTodayItems = if (is_2025_40_or_greater) {
            listOf(
                TrendingTodayItemFingerprint,
                TrendingTodayItemLegacyFingerprint
            )
        } else {
            listOf(TrendingTodayItemLegacyFingerprint)
        }

        trendingTodayItems.forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    addInstructions(
                        0, """
                            invoke-static {}, $INTEGRATIONS_METHOD_DESCRIPTOR
                            move-result v0
                            if-eqz v0, :ignore
                            return-void
                            """, listOf(ExternalLabel("ignore", getInstruction(0)))
                    )
                }
            }
        }

        // endregion

        updateSettingsStatus("enableTrendingTodayShelf")

    }
}
