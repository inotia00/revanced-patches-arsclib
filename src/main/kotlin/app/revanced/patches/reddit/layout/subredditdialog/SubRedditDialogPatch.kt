package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.FrequentUpdatesHandlerFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.FrequentUpdatesHandlerFingerprint.listOfIsLoggedInInstruction
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.FrequentUpdatesSheetScreenFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint.indexOfHasBeenVisitedInstruction
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint.indexOfIsIncognitoInstruction
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.RedditAlertDialogsFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.RedditAlertDialogsFingerprint.indexOfSetBackgroundTintListInstruction
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2024_41_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_01_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_05_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("Remove subreddit dialog")
@Description("Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class SubRedditDialogPatch : BytecodePatch(
    listOf(
        FrequentUpdatesHandlerFingerprint,
        FrequentUpdatesSheetScreenFingerprint,
        NSFWAlertEmitFingerprint,
        RedditAlertDialogsFingerprint
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/RemoveSubRedditDialogPatch;"
    }

    override fun execute(context: BytecodeContext) {

        if (is_2024_41_or_greater) {
            FrequentUpdatesHandlerFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    listOfIsLoggedInInstruction(this)
                        .forEach { index ->
                            val register =
                                getInstruction<OneRegisterInstruction>(index + 1).registerA

                            addInstructions(
                                index + 2, """
                                    invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->spoofLoggedInStatus(Z)Z
                                    move-result v$register
                                    """
                            )
                        }
                }
        }

        // Not used in latest Reddit client.
        if (!is_2025_05_or_greater) {
            FrequentUpdatesSheetScreenFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
                    val register =
                        getInstruction<OneRegisterInstruction>(index).registerA

                    addInstruction(
                        index,
                        "invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->dismissDialog(Landroid/view/View;)V"
                    )
            }
        }

        if (is_2025_01_or_greater) {
            NSFWAlertEmitFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    val hasBeenVisitedIndex = indexOfHasBeenVisitedInstruction(this)
                    val hasBeenVisitedRegister =
                        getInstruction<OneRegisterInstruction>(hasBeenVisitedIndex + 1).registerA

                    addInstructions(
                        hasBeenVisitedIndex + 2, """
                            invoke-static {v$hasBeenVisitedRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->spoofHasBeenVisitedStatus(Z)Z
                            move-result v$hasBeenVisitedRegister
                            """
                    )

                    val isIncognitoIndex = indexOfIsIncognitoInstruction(this)
                    val nsfwAlertBuilderIndex = indexOfFirstInstructionOrThrow(isIncognitoIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.firstOrNull() == "Z"
                    }
                    val nsfwAlertBuilderReference =
                        getInstruction<ReferenceInstruction>(nsfwAlertBuilderIndex).reference as MethodReference
                    val nsfwAlertBuilderClass =
                        nsfwAlertBuilderReference.definingClass

                    var hookCount = 0

                    context.classes.forEach { classDef ->
                        if (classDef.type == nsfwAlertBuilderClass) {
                            classDef.methods.forEach { method ->
                                val showIndex = method.indexOfFirstInstruction {
                                    opcode == Opcode.INVOKE_VIRTUAL &&
                                            getReference<MethodReference>()?.name == "show"
                                }
                                if (showIndex >= 0) {
                                    context.classes.proxy(classDef)
                                        .mutableClass
                                        .findMutableMethodOf(method)
                                        .apply {
                                            val dialogRegister =
                                                getInstruction<OneRegisterInstruction>(showIndex + 1).registerA

                                            addInstruction(
                                                showIndex + 2,
                                                "invoke-static {v$dialogRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->dismissNSFWDialog(Ljava/lang/Object;)V"
                                            )
                                            hookCount++
                                        }
                                }
                            }
                        }
                    }

                    if (hookCount == 0) {
                        throw PatchException("Failed to find hook method")
                    }
                }
        }

        // Not used in latest Reddit client.
        if (!is_2025_06_or_greater) {
            RedditAlertDialogsFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    val backgroundTintIndex = indexOfSetBackgroundTintListInstruction(this)
                    val insertIndex =
                        indexOfFirstInstructionOrThrow(backgroundTintIndex) {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "setTextAppearance"
                        }
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
