package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.reddit.utils.settings.fingerprints.AcknowledgementsLabelBuilderFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.PreferenceDestinationFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.PreferenceManagerFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.PreferenceManagerFingerprint.indexOfPreferencesPresenterInstruction
import app.revanced.patches.reddit.utils.settings.fingerprints.PreferenceManagerParentFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.RedditInternalFeaturesFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.SettingsStatusLoadFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.WebBrowserActivityOnCreateFingerprint
import app.revanced.patches.shared.settings.fingerprints.SharedSettingFingerprint
import app.revanced.util.alsoResolve
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.methodCall
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@RequiresIntegrations
class SettingsBytecodePatch : BytecodePatch(
    listOf(
        AcknowledgementsLabelBuilderFingerprint,
        OssLicensesMenuActivityOnCreateFingerprint,
        PreferenceDestinationFingerprint,
        PreferenceManagerParentFingerprint,
        RedditInternalFeaturesFingerprint,
        SharedSettingFingerprint,
        SettingsStatusLoadFingerprint,
        WebBrowserActivityOnCreateFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_PATH/settings/ActivityHook;"

        private lateinit var acknowledgementsLabelBuilderMethod: MutableMethod
        private lateinit var settingsStatusLoadMethod: MutableMethod

        internal var is_2024_26_or_greater = false
        internal var is_2024_41_or_greater = false
        internal var is_2025_01_or_greater = false
        internal var is_2025_05_or_greater = false
        internal var is_2025_06_or_greater = false
        internal var is_2025_13_or_greater = false
        internal var is_2025_40_or_greater = false
        internal var is_2025_45_or_greater = false
        internal var is_2025_52_or_greater = false

        internal fun updateSettingsLabel(label: String) =
            acknowledgementsLabelBuilderMethod.apply {
                val predicate: Instruction.() -> Boolean = {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "getString"
                }
                var insertIndex: Int

                if (is_2025_40_or_greater) {
                    val preferencesPresenterIndex =
                        indexOfPreferencesPresenterInstruction(this)

                    val stringIndex =
                        indexOfFirstInstructionReversedOrThrow(preferencesPresenterIndex, predicate)
                    val iconIndex =
                        indexOfFirstInstructionReversedOrThrow(stringIndex - 2, Opcode.CONST)
                    val iconRegister =
                        getInstruction<OneRegisterInstruction>(iconIndex).registerA

                    addInstructions(
                        iconIndex + 1, """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getIcon()I
                            move-result v$iconRegister
                            """
                    )

                    insertIndex =
                        indexOfFirstInstructionReversedOrThrow(preferencesPresenterIndex, predicate) + 2
                } else {
                    val stringIndex =
                        indexOfFirstStringInstructionOrThrow("onboardingAnalytics")
                    insertIndex =
                        indexOfFirstInstructionReversedOrThrow(stringIndex, predicate) + 2
                }

                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "const-string v$insertRegister, \"$label\""
                )
            }

        internal fun updateSettingsStatus(description: String) =
            settingsStatusLoadMethod.addInstruction(
                0,
                "invoke-static {}, $INTEGRATIONS_PATH/settings/SettingsStatus;->$description()V"
            )
    }

    override fun execute(context: BytecodeContext) {

        /**
         * Set version info
         */
        RedditInternalFeaturesFingerprint.resultOrThrow().mutableMethod.apply {
            val versionIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING
                        && (this as? BuilderInstruction21c)?.reference.toString().startsWith("202")
            }

            val versionNumber =
                getInstruction<BuilderInstruction21c>(versionIndex).reference.toString()
                    .replace(".", "").toInt()

            is_2024_26_or_greater = 2024260 <= versionNumber
            is_2024_41_or_greater = 2024410 <= versionNumber
            is_2025_01_or_greater = 2025010 <= versionNumber
            is_2025_05_or_greater = 2025050 <= versionNumber
            is_2025_06_or_greater = 2025060 <= versionNumber
            is_2025_13_or_greater = 2025130 <= versionNumber
            is_2025_40_or_greater = 2025400 <= versionNumber
            is_2025_45_or_greater = 2025450 <= versionNumber
            is_2025_52_or_greater = 2025520 <= versionNumber
        }

        /**
         * Set SharedPrefCategory
         */
        SharedSettingFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
                val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

                replaceInstruction(
                    stringIndex,
                    "const-string v$stringRegister, \"reddit_revanced\""
                )
            }
        }

        if (is_2025_40_or_greater) {
            /**
             * Replace settings label
             */
            acknowledgementsLabelBuilderMethod = PreferenceManagerFingerprint
                .alsoResolve(context, PreferenceManagerParentFingerprint)
                .mutableMethod

            /**
             * Initialize settings activity
             */
            PreferenceDestinationFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 2
                    val targetRegister =
                        getInstruction<FiveRegisterInstruction>(targetIndex).registerC
                    val targetReference =
                        getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference
                    val targetClass = targetReference.definingClass
                    val getActivityReference =
                        context.classes.findClassProxied { classDef ->
                            classDef.type == targetClass
                        }!!.mutableClass.methods.find { methodDef ->
                            methodDef.name == "getActivity"
                        }!!.methodCall()

                    val freeIndex = targetIndex + 1
                    val freeRegister =
                        getInstruction<OneRegisterInstruction>(freeIndex).registerA

                    addInstructions(
                        targetIndex, """
                            invoke-static/range { p1 .. p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->isAcknowledgment(Ljava/lang/Enum;)Z
                            move-result v$freeRegister
                            if-eqz v$freeRegister, :ignore
                            invoke-virtual {v$targetRegister}, $getActivityReference
                            move-result-object v$freeRegister
                            invoke-static {v$freeRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->initializeByIntent(Landroid/content/Context;)Landroid/content/Intent;
                            move-result-object v$freeRegister
                            invoke-virtual {v$targetRegister, v$freeRegister}, $targetClass->startActivity(Landroid/content/Intent;)V
                            return-void
                            :ignore
                            nop
                            """
                    )
                }
            }

            WebBrowserActivityOnCreateFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
                    val freeRegister =
                        getInstruction<OneRegisterInstruction>(stringIndex).registerA

                    val insertIndex = indexOfFirstInstructionOrThrow {
                        getReference<MethodReference>()?.toString() == "Landroid/app/Activity;->getIntent()Landroid/content/Intent;"
                    }

                    addInstructions(
                        insertIndex, """
                            invoke-static/range { p0 .. p0 }, $INTEGRATIONS_CLASS_DESCRIPTOR->hook(Landroid/app/Activity;)Z
                            move-result v$freeRegister
                            if-eqz v$freeRegister, :ignore
                            return-void
                            :ignore
                            nop
                            """
                    )
                }
            }
        } else {
            /**
             * Replace settings label
             */
            acknowledgementsLabelBuilderMethod = AcknowledgementsLabelBuilderFingerprint
                .resultOrThrow()
                .mutableMethod

            /**
             * Initialize settings activity
             */
            OssLicensesMenuActivityOnCreateFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1

                    addInstructions(
                        insertIndex, """
                            invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)V
                            return-void
                            """
                    )
                }
            }
        }

        settingsStatusLoadMethod = SettingsStatusLoadFingerprint.resultOrThrow().mutableMethod
    }
}