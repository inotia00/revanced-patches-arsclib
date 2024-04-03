package app.revanced.patches.youtube.misc.splashanimation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.splashanimation.fingerprints.WatchWhileActivityWithOutFlagsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch.mainActivityMutableClass
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DarkSplashAnimation
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object NewSplashAnimationPatch : BaseBytecodePatch(
    name = "Enable new splash animation",
    description = "Adds an option to enable a new type of splash animation.",
    dependencies = setOf(
        MainActivityResolvePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SplashAnimationPatch;"

    override fun execute(context: BytecodeContext) {
        WatchWhileActivityWithOutFlagsFingerprint.resolve(context, mainActivityMutableClass)

        /**
         * YouTube v18.28.xx~
         */
        WatchWhileActivityWithOutFlagsFingerprint.result?.let {
            it.mutableMethod.apply {
                var startIndex = getWideLiteralInstructionIndex(DarkSplashAnimation) - 1
                val endIndex = startIndex - 30

                for (index in startIndex downTo endIndex) {
                    if (getInstruction(index).opcode != Opcode.IF_EQZ)
                        continue

                    startIndex = index - 8

                    arrayOf(
                        index,
                        index - 8
                    ).forEach { insertIndex -> injectCall(insertIndex) }

                    break
                }

                for (index in startIndex downTo endIndex) {
                    if (getInstruction(index).opcode != Opcode.IF_NE)
                        continue

                    injectCall(index)

                    break
                }
            }
        } ?: throw WatchWhileActivityWithOutFlagsFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_NEW_SPLASH_ANIMATION"
            )
        )

        SettingsPatch.updatePatchStatus("Enable new splash animation")

    }

    private fun MutableMethod.injectCall(
        index: Int
    ) {
        if (getInstruction(index).opcode == Opcode.IF_NE)
            injectInt(index)
        else
            injectBoolean(index)
    }

    private fun MutableMethod.injectBoolean(index: Int) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->enableNewSplashAnimationBoolean(Z)Z
                    move-result v$register
                    """
        )
    }

    private fun MutableMethod.injectInt(index: Int) {
        val register = getInstruction<TwoRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->enableNewSplashAnimationInt(I)I
                    move-result v$register
                    """
        )
    }
}
