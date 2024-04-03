package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsABConfigFingerprint
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object ResumingShortsOnStartupPatch : BaseBytecodePatch(
    name = "Disable resuming shorts on startup",
    description = "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        UserWasInShortsABConfigFingerprint,
        UserWasInShortsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        UserWasInShortsABConfigFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)

            // This method will only be called for the user being A/B tested.
            // Presumably a method that processes the ProtoDataStore value (boolean) for the 'user_was_in_shorts' key.
            walkerMethod.apply {
                val insertIndex = getTargetIndex(Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                        move-result v$insertRegister
                        if-eqz v$insertRegister, :show
                        const/4 v$insertRegister, 0x0
                        return v$insertRegister
                        """, ExternalLabel("show", getInstruction(insertIndex))
                )
            }
        }

        UserWasInShortsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = getStringInstructionIndex("Failed to read user_was_in_shorts proto after successful warmup")
                val exceptionIndex = getTargetIndexReversed(startIndex, Opcode.RETURN_VOID) - 1
                val targetIndex = getTargetIndexReversed(exceptionIndex, Opcode.RETURN_VOID) + 1
                if (getInstruction(targetIndex).opcode != Opcode.IGET_OBJECT)
                    throw PatchException("Failed to find insert index")

                val replaceReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val replaceInstruction = getInstruction<TwoRegisterInstruction>(targetIndex)

                addInstructionsWithLabels(
                    targetIndex + 1,
                    """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                        move-result v${replaceInstruction.registerA}
                        if-eqz v${replaceInstruction.registerA}, :show
                        return-void
                        :show
                        iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                        """
                )
                removeInstruction(targetIndex)
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: DISABLE_RESUMING_SHORTS_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("Disable resuming shorts on startup")

    }
}
