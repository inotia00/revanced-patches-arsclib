package app.revanced.patches.music.navigation.component

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.music.navigation.component.fingerprints.TabLayoutTextFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Suppress("DEPRECATION", "SpellCheckingInspection", "unused")
object NavigationBarComponentPatch : BaseBytecodePatch(
    name = "Hide navigation bar component",
    description = "Adds options to hide navigation bar components.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TabLayoutTextFingerprint)
) {
    private const val FLAG = "android:layout_weight"
    private const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"

    override fun execute(context: BytecodeContext) {
        /**
         * Hide navigation labels
         */
        TabLayoutTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(SharedResourceIdPatch.Text1)
                val targetIndex = getTargetIndex(constIndex, Opcode.CHECK_CAST)
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        SettingsPatch.contexts.xmlEditor[RESOURCE_FILE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }

        /**
         * Hide navigation bar & buttons
         */
        TabLayoutTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val enumIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA
                val insertEnumIndex = getTargetIndex(Opcode.AND_INT_LIT8) - 2

                val pivotTabIndex = getTargetIndexWithMethodReferenceName("getVisibility")
                val pivotTabRegister = getInstruction<Instruction35c>(pivotTabIndex).registerC

                addInstruction(
                    pivotTabIndex,
                    "invoke-static {v$pivotTabRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationButton(Landroid/view/View;)V"
                )

                addInstruction(
                    insertEnumIndex,
                    "sput-object v$enumRegister, $NAVIGATION_CLASS_DESCRIPTOR->lastPivotTab:Ljava/lang/Enum;"
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_explore_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_home_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_library_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_bar",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_label",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_samples_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_upgrade_button",
            "true"
        )
    }
}
