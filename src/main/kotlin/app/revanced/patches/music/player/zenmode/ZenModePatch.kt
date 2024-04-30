package app.revanced.patches.music.player.zenmode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.player.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.utils.fingerprints.SwitchToggleColorFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object ZenModePatch : BaseBytecodePatch(
    name = "Enable zen mode",
    description = "Adds an option to change the player background to light grey to reduce eye strain.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(MiniPlayerConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MiniPlayerConstructorFingerprint.resultOrThrow().let { parentResult ->
            // Resolves fingerprints
            SwitchToggleColorFingerprint.resolve(context, parentResult.classDef)
            ZenModeFingerprint.resolve(context, parentResult.classDef)

            // This method is used for old player background
            // Deprecated since YT Music v6.34.51
            ZenModeFingerprint.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA

                    val insertIndex = it.scanResult.patternScanResult!!.endIndex + 1

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result v$targetRegister
                            """
                    )
                }
            }

            SwitchToggleColorFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val invokeDirectIndex = getTargetIndex(Opcode.INVOKE_DIRECT)
                    val walkerMethod = getWalkerMethod(context, invokeDirectIndex)

                    walkerMethod.addInstructions(
                        0, """
                            invoke-static {p1}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result p1
                            invoke-static {p2}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result p2
                            """
                    )
                }
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode_podcast",
            "false",
            "revanced_enable_zen_mode"
        )

    }
}