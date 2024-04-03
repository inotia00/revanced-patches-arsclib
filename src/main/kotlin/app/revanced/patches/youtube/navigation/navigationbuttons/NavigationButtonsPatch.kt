package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarButtonViewFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ImageOnlyTab
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcode.MOVE_RESULT_OBJECT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object NavigationButtonsPatch : BaseBytecodePatch(
    name = "Hide navigation buttons",
    description = "Adds options to hide and change navigation buttons (such as the Shorts button).",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AutoMotiveFingerprint,
        PivotBarCreateButtonViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        PivotBarCreateButtonViewFingerprint.resultOrThrow().let { parentResult ->

            /**
             * Home, Shorts, Subscriptions Button
             */
            with(
                arrayOf(
                    PivotBarEnumFingerprint,
                    PivotBarButtonViewFingerprint
                ).onEach {
                    it.resolve(
                        context,
                        parentResult.mutableMethod,
                        parentResult.mutableClass
                    )
                }.map {
                    it.resultOrThrow().scanResult.patternScanResult!!
                }
            ) {
                val enumScanResult = this[0]
                val buttonViewResult = this[1]

                val enumHookInsertIndex = enumScanResult.startIndex + 2
                val buttonHookInsertIndex = buttonViewResult.endIndex

                mapOf(
                    BUTTON_HOOK to buttonHookInsertIndex,
                    ENUM_HOOK to enumHookInsertIndex
                ).forEach { (hook, insertIndex) ->
                    parentResult.mutableMethod.injectHook(hook, insertIndex)
                }
            }

            /**
             * Create Button
             */
            parentResult.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(ImageOnlyTab)
                val insertIndex = getTargetIndex(constIndex, Opcode.INVOKE_VIRTUAL) + 2
                injectHook(CREATE_BUTTON_HOOK, insertIndex)
            }

        }

        /**
         * Switch create button with notifications button
         */
        AutoMotiveFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("Android Automotive") - 1
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $NAVIGATION_CLASS_DESCRIPTOR->switchCreateNotification(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide navigation buttons")

    }

    private const val REGISTER_TEMPLATE_REPLACEMENT: String = "REGISTER_INDEX"

    private const val ENUM_HOOK =
        "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $NAVIGATION_CLASS_DESCRIPTOR" +
                "->" +
                "lastPivotTab:Ljava/lang/Enum;"

    private const val BUTTON_HOOK =
        "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION_CLASS_DESCRIPTOR" +
                "->" +
                "hideNavigationButton(Landroid/view/View;)V"

    private const val CREATE_BUTTON_HOOK =
        "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION_CLASS_DESCRIPTOR" +
                "->" +
                "hideCreateButton(Landroid/view/View;)V"

    /**
     * Injects an instruction into insertIndex of the hook.
     * @param hook The hook to insert.
     * @param insertIndex The index to insert the instruction at.
     * [MOVE_RESULT_OBJECT] has to be the previous instruction before [insertIndex].
     */
    private fun MutableMethod.injectHook(hook: String, insertIndex: Int) {
        val injectTarget = this

        // Register to pass to the hook
        val registerIndex = insertIndex - 1 // MOVE_RESULT_OBJECT is always the previous instruction
        val register = injectTarget.getInstruction<OneRegisterInstruction>(registerIndex).registerA

        injectTarget.addInstruction(
            insertIndex,
            hook.replace("REGISTER_INDEX", register.toString()),
        )
    }
}