package app.revanced.patches.reddit.layout.navigation

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenHandlerFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenHandlerFingerprint.indexOfGetItemsInstruction
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenSetupBottomNavigationFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.ComposeBottomNavScreenFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2024_26_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.iface.reference.TypeReference

@Patch
@Name("Hide navigation buttons")
@Description("Adds options to hide buttons in the navigation bar.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class NavigationButtonsPatch : BytecodePatch(
    listOf(
        BottomNavScreenHandlerFingerprint,
        BottomNavScreenFingerprint,
        BottomNavScreenSetupBottomNavigationFingerprint,
        ComposeBottomNavScreenFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/NavigationButtonsPatch;"

        private fun indexOfButtonsArrayInstruction(methodDef: Method) =
            methodDef.indexOfFirstInstruction {
                opcode == Opcode.FILLED_NEW_ARRAY &&
                        getReference<TypeReference>()?.type?.startsWith("[Lcom/reddit/widget/bottomnav/") == true
            }
    }

    override fun execute(context: BytecodeContext) {

        if (!is_2024_26_or_greater) {
            // Legacy method.
            BottomNavScreenHandlerFingerprint.resultOrThrow().mutableMethod.apply {
                val targetIndex = indexOfGetItemsInstruction(this) + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$targetRegister
                        """
                )
            }
        } else if (!is_2025_40_or_greater) {
            val fingerprints = if (is_2025_06_or_greater) {
                listOf(
                    BottomNavScreenSetupBottomNavigationFingerprint,
                    ComposeBottomNavScreenFingerprint
                )
            } else {
                listOf(
                    BottomNavScreenSetupBottomNavigationFingerprint
                )
            }

            fingerprints.forEach { fingerprint ->
                fingerprint.resultOrThrow().mutableMethod.apply {
                    val arrayIndex = indexOfButtonsArrayInstruction(this)
                    val arrayRegister =
                        getInstruction<OneRegisterInstruction>(arrayIndex + 1).registerA

                    addInstructions(
                        arrayIndex + 2, """
                            invoke-static {v$arrayRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideNavigationButtons([Ljava/lang/Object;)[Ljava/lang/Object;
                            move-result-object v$arrayRegister
                            """
                    )
                }
            }
        } else {
            BottomNavScreenFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    implementation!!.instructions
                        .withIndex()
                        .filter { (_, instruction) ->
                            val reference =
                                (instruction as? ReferenceInstruction)?.reference
                            instruction.opcode == Opcode.INVOKE_INTERFACE &&
                                    reference is MethodReference &&
                                    reference.toString() == "Ljava/util/List;->add(Ljava/lang/Object;)Z"
                        }
                        .map { (index, _) -> index }
                        .reversed()
                        .forEach { index ->
                            val instruction =
                                getInstruction<FiveRegisterInstruction>(index)

                            val listRegister = instruction.registerC
                            val objectRegister = instruction.registerD

                            replaceInstruction(
                                index,
                                "invoke-static { v$listRegister, v$objectRegister }, " +
                                        "$INTEGRATIONS_CLASS_DESCRIPTOR->" +
                                        "hideNavigationButtons(Ljava/util/List;Ljava/lang/Object;)V"
                            )
                        }

                    implementation!!.instructions
                        .withIndex()
                        .filter { (_, instruction) ->
                            val reference =
                                (instruction as? ReferenceInstruction)?.reference
                            instruction.opcode == Opcode.INVOKE_DIRECT &&
                                    reference is MethodReference &&
                                    reference.definingClass.startsWith("Lcom/reddit/widget/bottomnav/") &&
                                    reference.name == "<init>" &&
                                    reference.parameterTypes.firstOrNull() == "Ljava/lang/String;"
                        }
                        .map { (index, _) -> index }
                        .reversed()
                        .forEach { index ->
                            val instruction =
                                getInstruction<FiveRegisterInstruction>(index)

                            val objectRegister = instruction.registerC
                            val labelRegister = instruction.registerD

                            addInstruction(
                                index + 1,
                                "invoke-static { v$objectRegister, v$labelRegister }, " +
                                        "$INTEGRATIONS_CLASS_DESCRIPTOR->" +
                                        "setNavigationMap(Ljava/lang/Object;Ljava/lang/String;)V"
                            )
                        }

                    addInstruction(
                        0,
                        "invoke-static/range { p1 .. p1 }, " +
                                "$INTEGRATIONS_CLASS_DESCRIPTOR->setResources(Landroid/content/res/Resources;)V"
                    )
            }
        }

        updateSettingsStatus("enableNavigationButtons")

    }
}
