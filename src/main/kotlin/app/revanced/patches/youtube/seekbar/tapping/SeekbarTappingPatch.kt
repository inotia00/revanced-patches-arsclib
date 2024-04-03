package app.revanced.patches.youtube.seekbar.tapping

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.tapping.fingerprints.SeekbarTappingFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SEEKBAR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodReference
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n

@Suppress("unused")
object SeekbarTappingPatch : BaseBytecodePatch(
    name = "Enable seekbar tapping",
    description = "Adds an option to enable tap-to-seek on the seekbar of the video player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(SeekbarTappingFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        SeekbarTappingFingerprint.result?.let {
            it.mutableMethod.apply {
                val tapSeekIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val tapSeekReference = getInstruction<BuilderInstruction35c>(tapSeekIndex).reference
                val tapSeekClass =
                    context
                        .findClass(((tapSeekReference) as DexBackedMethodReference).definingClass)!!
                        .mutableClass
                val tapSeekMethods = mutableMapOf<String, MutableMethod>()

                for (method in tapSeekClass.methods) {
                    if (method.implementation == null)
                        continue

                    val instructions = method.implementation!!.instructions
                    // here we make sure we actually find the method because it has more than 7 instructions
                    if (instructions.count() != 10)
                        continue

                    // we know that the 7th instruction has the opcode CONST_4
                    val instruction = instructions.elementAt(6)
                    if (instruction.opcode != Opcode.CONST_4)
                        continue

                    // the literal for this instruction has to be either 1 or 2
                    val literal = (instruction as Instruction11n).narrowLiteral

                    // method founds
                    if (literal == 1)
                        tapSeekMethods["P"] = method
                    else if (literal == 2)
                        tapSeekMethods["O"] = method
                }

                val pMethod = tapSeekMethods["P"]
                    ?: throw PatchException("tapSeekMethod not found")
                val oMethod = tapSeekMethods["O"]
                    ?: throw PatchException("tapSeekMethod not found")

                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SEEKBAR_CLASS_DESCRIPTOR->enableSeekbarTapping()Z
                        move-result v0
                        if-eqz v0, :disabled
                        invoke-virtual { p0, v2 }, ${oMethod.definingClass}->${oMethod.name}(I)V
                        invoke-virtual { p0, v2 }, ${pMethod.definingClass}->${pMethod.name}(I)V
                        """, ExternalLabel("disabled", getInstruction(insertIndex))
                )
            }
        } ?: throw SeekbarTappingFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_SEEKBAR_TAPPING"
            )
        )

        SettingsPatch.updatePatchStatus("Enable seekbar tapping")

    }

    private lateinit var TappingLabel: String
}