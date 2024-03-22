package app.revanced.patches.music.flyoutpanel.shared

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.flyoutpanel.shared.fingerprints.MenuItemFingerprint
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import kotlin.properties.Delegates

object FlyoutPanelMenuItemPatch : BytecodePatch(
    setOf(MenuItemFingerprint)
) {
    private lateinit var menuItemMethod: MutableMethod
    private var freeRegister by Delegates.notNull<Int>()
    private var textViewRegister by Delegates.notNull<Int>()
    private var imageViewRegister by Delegates.notNull<Int>()
    private var instructionAdded = false

    override fun execute(context: BytecodeContext) {
        MenuItemFingerprint.result?.let {
            it.mutableMethod.apply {
                val freeIndex = getTargetIndex(Opcode.OR_INT_LIT16)
                val textViewIndex = it.scanResult.patternScanResult!!.startIndex
                val imageViewIndex = it.scanResult.patternScanResult!!.endIndex

                freeRegister =
                    getInstruction<TwoRegisterInstruction>(freeIndex).registerA
                textViewRegister =
                    getInstruction<OneRegisterInstruction>(textViewIndex).registerA
                imageViewRegister =
                    getInstruction<OneRegisterInstruction>(imageViewIndex).registerA

                menuItemMethod = this
            }
        } ?: throw MenuItemFingerprint.exception
    }

    private fun MutableMethod.getEnumIndex() = indexOfFirstInstruction {
        opcode == Opcode.INVOKE_STATIC
                && (this as? ReferenceInstruction)?.reference.toString().contains("(I)L")
    } + 1

    internal fun hideComponents() {
        menuItemMethod.apply {
            val enumIndex = getEnumIndex()
            val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

            addInstructionsWithLabels(
                enumIndex + 1, """
                    invoke-static {v$enumRegister}, $FLYOUT->hideComponents(Ljava/lang/Enum;)Z
                    move-result v$freeRegister
                    if-nez v$freeRegister, :hide
                    """, ExternalLabel("hide", getInstruction(implementation!!.instructions.size - 1))
            )
        }
    }

    internal fun replaceComponents() {
        if (!instructionAdded) {
            menuItemMethod.apply {
                val enumIndex = getEnumIndex()
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                addInstruction(
                    enumIndex + 1,
                    "invoke-static {v$enumRegister, v$textViewRegister, v$imageViewRegister}, $FLYOUT->replaceComponents(Ljava/lang/Enum;Landroid/widget/TextView;Landroid/widget/ImageView;)V"
                )
            }
            instructionAdded = true
        }
    }
}
