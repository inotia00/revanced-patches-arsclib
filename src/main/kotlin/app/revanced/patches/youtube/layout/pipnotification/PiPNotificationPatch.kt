package app.revanced.patches.youtube.layout.pipnotification

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.youtube.layout.pipnotification.fingerprints.PiPNotificationFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object PiPNotificationPatch : BaseBytecodePatch(
    name = "Disable pip notification",
    description = "Disable pip notification when you first launch pip mode.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PiPNotificationFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PiPNotificationFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val checkCastCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? ReferenceInstruction)?.reference.toString() == "Lcom/google/apps/tiktok/account/AccountId;"
                    }

                if (checkCastCalls.size != 3)
                    throw PatchException("Couldn't find target Index")

                arrayOf(
                    checkCastCalls.elementAt(1).index,
                    checkCastCalls.elementAt(0).index
                ).forEach { index ->
                    addInstruction(
                        index + 1,
                        "return-void"
                    )
                }
            }
        }

        SettingsPatch.updatePatchStatus(this)
    }
}