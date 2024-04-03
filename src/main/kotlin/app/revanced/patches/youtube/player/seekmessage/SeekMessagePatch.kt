package app.revanced.patches.youtube.player.seekmessage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduContainerFingerprint
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduUndoOverlayFingerprint
import app.revanced.patches.youtube.utils.controlsoverlay.ControlsOverlayConfigPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
object SeekMessagePatch : BaseBytecodePatch(
    name = "Hide seek message",
    description = "Adds an option to hide the 'Slide left or right to seek' or 'Release to cancel' message container in the video player.",
    dependencies = setOf(
        ControlsOverlayConfigPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        SeekEduContainerFingerprint,
        SeekEduUndoOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        SeekEduContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        }

        /**
         * Added in YouTube v18.29.xx~
         */
        SeekEduUndoOverlayFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val seekUndoCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? WideLiteralInstruction)?.wideLiteral == SeekUndoEduOverlayStub
                    }
                val insertIndex = seekUndoCalls.elementAt(seekUndoCalls.size - 1).index
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                val onClickListenerIndex = getTargetIndexWithMethodReferenceName(insertIndex, "setOnClickListener")
                val constComponent = getConstComponent(insertIndex, onClickListenerIndex - 1)

                addInstructionsWithLabels(
                    insertIndex, constComponent + """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekUndoMessage()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :default
                        """, ExternalLabel("default", getInstruction(onClickListenerIndex + 1))
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_SEEK_MESSAGE"
            )
        )

        SettingsPatch.updatePatchStatus("Hide seek message")

    }

    private fun MutableMethod.getConstComponent(
        startIndex: Int,
        endIndex: Int
    ): String {
        val constRegister =
            getInstruction<FiveRegisterInstruction>(endIndex).registerE

        for (index in endIndex downTo startIndex) {
            val instruction = getInstruction(index)
            if (instruction !is WideLiteralInstruction)
                continue

            if ((instruction as OneRegisterInstruction).registerA != constRegister)
                continue

            val constValue = (instruction as WideLiteralInstruction).wideLiteral.toInt()

            return "const/16 v$constRegister, $constValue"
        }
        return ""
    }
}
