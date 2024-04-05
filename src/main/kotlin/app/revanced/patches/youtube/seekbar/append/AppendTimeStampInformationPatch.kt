package app.revanced.patches.youtube.seekbar.append

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SEEKBAR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.overridequality.OverrideQualityHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object AppendTimeStampInformationPatch : BaseBytecodePatch(
    name = "Append time stamps information",
    description = "Adds an option to add the current video quality or playback speed in brackets next to the current time.",
    dependencies = setOf(
        OverrideQualityHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TotalTimeFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TotalTimeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val charSequenceIndex = getTargetIndexWithMethodReferenceName("getString") + 1
                val charSequenceRegister = getInstruction<OneRegisterInstruction>(charSequenceIndex).registerA
                val textViewIndex = getTargetIndexWithMethodReferenceName("getText")
                val textViewRegister =
                    getInstruction<FiveRegisterInstruction>(textViewIndex).registerC

                addInstructions(
                    textViewIndex, """
                        invoke-static {v$textViewRegister}, $SEEKBAR_CLASS_DESCRIPTOR->setContainerClickListener(Landroid/view/View;)V
                        invoke-static {v$charSequenceRegister}, $SEEKBAR_CLASS_DESCRIPTOR->appendTimeStampInformation(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$charSequenceRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: APPEND_TIME_STAMP_INFORMATION"
            )
        )

        SettingsPatch.updatePatchStatus("Append time stamps information")

    }
}
