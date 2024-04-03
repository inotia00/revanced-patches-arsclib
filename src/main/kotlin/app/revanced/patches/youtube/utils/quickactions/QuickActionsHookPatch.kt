package app.revanced.patches.youtube.utils.quickactions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.quickactions.fingerprints.QuickActionsElementFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.QuickActionsElementContainer
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object QuickActionsHookPatch : BytecodePatch(
    setOf(QuickActionsElementFingerprint)
) {
    private lateinit var insertMethod: MutableMethod
    private var insertIndex: Int = 0
    private var insertRegister: Int = 0
    override fun execute(context: BytecodeContext) {

        QuickActionsElementFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                insertMethod = this
                val containerCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? WideLiteralInstruction)?.wideLiteral == QuickActionsElementContainer
                    }
                val constIndex = containerCalls.elementAt(containerCalls.size - 1).index
                insertRegister =
                    getInstruction<OneRegisterInstruction>(constIndex + 2).registerA

                addInstruction(
                    constIndex + 3,
                    "invoke-static {v$insertRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->hideQuickActions(Landroid/view/View;)V"
                )
                insertIndex = constIndex + 5
            }
        }
    }

    internal fun injectQuickActionMargin() {
        insertMethod.addInstruction(
            insertIndex,
            "invoke-static {v$insertRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->setQuickActionMargin(Landroid/widget/FrameLayout;)V"
        )
    }
}
