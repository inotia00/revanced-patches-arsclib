package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.screenshotpopup.fingerprints.ScreenshotTakenBannerFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("Disable screenshot popup")
@Description("Adds an option to disable the popup that appears when taking a screenshot.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class ScreenshotPopupPatch : BytecodePatch(
    listOf(ScreenshotTakenBannerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        fun indexOfShowBannerInstruction(methodDef: Method) =
            methodDef.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET_OBJECT &&
                        reference?.name?.contains("shouldShowBanner") == true &&
                        reference.definingClass.startsWith("Lcom/reddit/sharing/screenshot/")
            }

        fun indexOfSetValueInstruction(methodDef: Method) =
            methodDef.indexOfFirstInstruction {
                getReference<MethodReference>()?.name == "setValue"
            }

        fun indexOfBooleanInstruction(methodDef: Method, startIndex: Int = 0) =
            methodDef.indexOfFirstInstruction(startIndex) {
                val reference = getReference<FieldReference>()
                opcode == Opcode.SGET_OBJECT &&
                        reference?.definingClass == "Ljava/lang/Boolean;" &&
                        reference.type == "Ljava/lang/Boolean;"
            }

        val isScreenShotMethod: Method.() -> Boolean = {
            definingClass.startsWith("Lcom/reddit/sharing/screenshot/") &&
                    name == "invokeSuspend" &&
                    indexOfShowBannerInstruction(this) >= 0 &&
                    indexOfBooleanInstruction(this) >= 0 &&
                    indexOfSetValueInstruction(this) >= 0
        }

        var hookCount = 0

        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.isScreenShotMethod()) {
                    context.classes.proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .apply {
                            val showBannerIndex = indexOfShowBannerInstruction(this)
                            val booleanIndex = indexOfBooleanInstruction(this, showBannerIndex)
                            val booleanRegister =
                                getInstruction<OneRegisterInstruction>(booleanIndex).registerA

                            addInstructions(
                                booleanIndex + 1, """
                                    invoke-static {v$booleanRegister}, $PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup(Ljava/lang/Boolean;)Ljava/lang/Boolean;
                                    move-result-object v$booleanRegister
                                    """
                            )
                            hookCount++
                        }
                }
            }
        }

        if (hookCount == 0) {
            throw PatchException("Failed to find hook method")
        }

        updateSettingsStatus("enableScreenshotPopup")

    }
}
