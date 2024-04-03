package app.revanced.patches.youtube.fullscreen.autoplaypreview

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavPreviewStub
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavToggle
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object AutoplayPreviewPatch : BaseBytecodePatch(
    name = "Hide autoplay preview",
    description = "Adds an option to hide the autoplay preview container when in fullscreen.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(LayoutConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        LayoutConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val dummyRegister =
                    getInstruction<OneRegisterInstruction>(getStringInstructionIndex("1.0x")).registerA
                val insertIndex = getWideLiteralInstructionIndex(AutoNavPreviewStub)
                val jumpIndex = getWideLiteralInstructionIndex(AutoNavToggle) - 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $FULLSCREEN_CLASS_DESCRIPTOR->hideAutoPlayPreview()Z
                        move-result v$dummyRegister
                        if-nez v$dummyRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } ?: throw LayoutConstructorFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("Hide autoplay preview")

    }
}

