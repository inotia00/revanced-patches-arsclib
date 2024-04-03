package app.revanced.patches.youtube.player.collapsebutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.player.collapsebutton.fingerprints.LiveChatFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c

@Suppress("unused")
object CollapseButtonPatch : BaseBytecodePatch(
    name = "Hide collapse button",
    description = "Adds an option to hide the collapse button in the video player.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(LiveChatFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        LiveChatFingerprint.resultOrThrow().let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            val instructions = it.mutableMethod.getInstruction(endIndex)
            val imageButtonClass = context
                .findClass((instructions as BuilderInstruction21c).reference.toString())!!
                .mutableClass

            for (method in imageButtonClass.methods) {
                imageButtonClass.findMutableMethodOf(method).apply {
                    var jumpInstruction = true

                    implementation!!.instructions.forEachIndexed { index, instructions ->
                        if (instructions.opcode == Opcode.INVOKE_VIRTUAL) {
                            val definedInstruction = (instructions as? BuilderInstruction35c)

                            if (definedInstruction?.reference.toString() ==
                                "Landroid/view/View;->setVisibility(I)V"
                            ) {

                                jumpInstruction = !jumpInstruction
                                if (jumpInstruction) return@forEachIndexed

                                val firstRegister = definedInstruction?.registerC
                                val secondRegister = definedInstruction?.registerD

                                addInstructions(
                                    index, """
                                        invoke-static {v$firstRegister, v$secondRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerButton(Landroid/view/View;I)I
                                        move-result v$secondRegister
                                        """
                                )
                            }
                        }
                    }
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_COLLAPSE_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide collapse button")

    }
}
