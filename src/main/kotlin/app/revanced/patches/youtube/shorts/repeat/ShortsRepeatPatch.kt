package app.revanced.patches.youtube.shorts.repeat

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.repeat.fingerprints.ReelEnumConstructorFingerprint
import app.revanced.patches.youtube.shorts.repeat.fingerprints.ReelEnumStaticFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.containsReferenceInstructionIndex
import app.revanced.util.copyXmlNode
import app.revanced.util.exception
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Change shorts repeat state",
    description = "Adds an options for whether shorts should repeat, autoplay, or stop.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39"
            ]
        )
    ]
)
@Suppress("unused")
object ShortsRepeatPatch : BytecodePatch(
    setOf(ReelEnumConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ReelEnumConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                ReelEnumStaticFingerprint.resolve(context, it.mutableClass)

                arrayOf(
                    "REEL_LOOP_BEHAVIOR_END_SCREEN" to "endScreen",
                    "REEL_LOOP_BEHAVIOR_REPEAT" to "repeat",
                    "REEL_LOOP_BEHAVIOR_SINGLE_PLAY" to "singlePlay"
                ).map { (enumName, fieldName) ->
                    injectEnum(enumName, fieldName)
                }

                val endScreenStringIndex = getStringInstructionIndex("REEL_LOOP_BEHAVIOR_END_SCREEN")
                val endScreenReferenceIndex = getTargetIndex(endScreenStringIndex, Opcode.SPUT_OBJECT)
                val endScreenReference = getInstruction<ReferenceInstruction>(endScreenReferenceIndex).reference.toString()

                val enumMethodName = ReelEnumStaticFingerprint.result?.mutableMethod?.name
                    ?: throw ReelEnumStaticFingerprint.exception

                val enumMethodCall = "$definingClass->$enumMethodName(I)$definingClass"

                context.injectHook(endScreenReference, enumMethodCall)
            }
        } ?: throw ReelEnumConstructorFingerprint.exception

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/shorts/host", "values/arrays.xml", "resources")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: CHANGE_SHORTS_REPEAT_STATE"
            )
        )

        SettingsPatch.updatePatchStatus("Change shorts repeat state")
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
            "sput-object v$insertRegister, $SHORTS->$fieldName:Ljava/lang/Enum;"
        )
    }

    private fun BytecodeContext.injectHook(
        endScreenReference: String,
        enumMethodCall: String
    ) {
        classes.forEach { classDef ->
            classDef.methods.filter { method ->
                method.parameters.size == 1
                        && method.parameters[0].startsWith("L")
                        && method.returnType == "V"
                        && method.containsReferenceInstructionIndex(endScreenReference)
            }.forEach { targetMethod ->
                proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(targetMethod)
                    .apply {
                        for ((index, instruction) in implementation!!.instructions.withIndex()) {
                            if (instruction.opcode != Opcode.INVOKE_STATIC)
                                continue
                            if ((instruction as ReferenceInstruction).reference.toString() != enumMethodCall)
                                continue

                            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                            addInstructions(
                                index + 2, """
                                    invoke-static {v$register}, $SHORTS->changeShortsRepeatState(Ljava/lang/Enum;)Ljava/lang/Enum;
                                    move-result-object v$register
                                    """
                            )
                    }
                }
            }
        }
    }
}
