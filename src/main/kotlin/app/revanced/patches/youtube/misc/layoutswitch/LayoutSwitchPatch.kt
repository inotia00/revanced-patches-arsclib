package app.revanced.patches.youtube.misc.layoutswitch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.GetFormFactorFingerprint
import app.revanced.patches.youtube.utils.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LayoutSwitchPatch : BaseBytecodePatch(
    name = "Layout switch",
    description = "Adds an option to trick dpi to use tablet or phone layout.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        GetFormFactorFingerprint,
        LayoutSwitchFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/LayoutOverridePatch;"
    override fun execute(context: BytecodeContext) {

        // tablet layout
        GetFormFactorFingerprint.result?.let {
            it.mutableMethod.apply {
                val jumpIndex = getTargetIndexReversed(Opcode.SGET_OBJECT)

                addInstructionsWithLabels(
                    0, """
                        invoke-static { }, $INTEGRATIONS_CLASS_DESCRIPTOR->enableTabletLayout()Z
                        move-result v0 # Free register
                        if-nez v0, :is_large_form_factor
                        """,
                    ExternalLabel(
                        "is_large_form_factor",
                        getInstruction(jumpIndex)
                    )
                )
            }
        } ?: throw GetFormFactorFingerprint.exception

        // phone layout
        LayoutSwitchFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    4, """
                        invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR->getLayoutOverride(I)I
                        move-result p0
                        """
                )
            }
        } ?: throw LayoutSwitchFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: LAYOUT_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Layout switch")

    }

    private fun MutableMethod.injectEnum(
        enumName: String,
        fieldName: String
    ) {
        val stringIndex = getStringInstructionIndex(enumName)
        val insertIndex = getTargetIndex(stringIndex, Opcode.SPUT_OBJECT)
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstruction(
            insertIndex + 1,
            "sput-object v$insertRegister, $INTEGRATIONS_CLASS_DESCRIPTOR->$fieldName:Ljava/lang/Enum;"
        )
    }
}
