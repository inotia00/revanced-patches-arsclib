package app.revanced.patches.music.player.repeat

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.player.repeat.fingerprints.RepeatTrackFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object RememberRepeatPatch : BaseBytecodePatch(
    name = "Remember repeat state",
    description = "Adds an option to remember the state of the repeat toggle.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(RepeatTrackFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        RepeatTrackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->rememberRepeatState(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_remember_repeat_state",
            "true"
        )
    }
}
