package app.revanced.patches.youtube.fullscreen.forcefullscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.youtube.fullscreen.forcefullscreen.fingerprints.ClientSettingEndpointFingerprint
import app.revanced.patches.youtube.fullscreen.forcefullscreen.fingerprints.VideoPortraitParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object ForceFullscreenPatch : BaseBytecodePatch(
    name = "Force fullscreen",
    description = "Adds an option to forcefully open videos in fullscreen.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ClientSettingEndpointFingerprint,
        VideoPortraitParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        /**
         * Process that hooks Activity for using {Activity.setRequestedOrientation}.
         */
        ClientSettingEndpointFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val getActivityIndex = getStringInstructionIndex("watch") + 2
                val getActivityReference =
                    getInstruction<ReferenceInstruction>(getActivityIndex).reference
                val classRegister =
                    getInstruction<TwoRegisterInstruction>(getActivityIndex).registerB

                val watchDescriptorMethodIndex =
                    getStringInstructionIndex("start_watch_minimized") - 1
                val watchDescriptorRegister =
                    getInstruction<FiveRegisterInstruction>(watchDescriptorMethodIndex).registerD

                addInstructions(
                    watchDescriptorMethodIndex, """
                        invoke-static {v$watchDescriptorRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->forceFullscreen(Z)Z
                        move-result v$watchDescriptorRegister
                        """
                )

                val insertIndex = getStringInstructionIndex("force_fullscreen")
                val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$classRegister, $getActivityReference
                        check-cast v$freeRegister, Landroid/app/Activity;
                        sput-object v$freeRegister, $FULLSCREEN_CLASS_DESCRIPTOR->watchDescriptorActivity:Landroid/app/Activity;
                        """
                )
            }
        }

        /**
         * Don't rotate the screen in vertical video.
         * Add an instruction to check the vertical video.
         */
        VideoPortraitParentFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex =
                    getStringInstructionIndex("Acquiring NetLatencyActionLogger failed. taskId=")
                val invokeIndex = getTargetIndex(stringIndex, Opcode.INVOKE_INTERFACE)
                val targetIndex = getTargetIndex(invokeIndex, Opcode.CHECK_CAST)
                val targetClass = context
                    .findClass(getInstruction<ReferenceInstruction>(targetIndex).reference.toString())!!
                    .mutableClass

                targetClass.methods.find { method -> method.parameters == listOf("I", "I", "Z") }
                    ?.apply {
                        addInstruction(
                            1,
                            "invoke-static {p1, p2}, $FULLSCREEN_CLASS_DESCRIPTOR->setVideoPortrait(II)V"
                        )
                    } ?: throw PatchException("Could not find targetMethod")
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: FULLSCREEN_EXPERIMENTAL_FLAGS",
                "SETTINGS: FORCE_FULLSCREEN"
            )
        )

        SettingsPatch.updatePatchStatus("Force fullscreen")

    }
}
