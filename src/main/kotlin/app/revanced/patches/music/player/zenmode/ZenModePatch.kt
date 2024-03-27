package app.revanced.patches.music.player.zenmode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.player.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.utils.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.utils.fingerprints.SwitchToggleColorFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable zen mode",
    description = "Adds an option to change the player background to light grey to reduce eye strain.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object ZenModePatch : BytecodePatch(
    setOf(MiniPlayerConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MiniPlayerConstructorFingerprint.result?.let { parentResult ->
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
                            invoke-static {v$targetRegister}, $PLAYER->enableZenMode(I)I
                            move-result v$targetRegister
                            """
                    )
                }
            }

            SwitchToggleColorFingerprint.result?.let {
                val invokeDirectIndex = it.mutableMethod.getTargetIndex(0, Opcode.INVOKE_DIRECT)
                val targetMethod = context.toMethodWalker(it.method)
                    .nextMethod(invokeDirectIndex, true)
                    .getMethod() as MutableMethod

                targetMethod.apply {
                    addInstructions(
                        0, """
                            invoke-static {p1}, $PLAYER->enableZenMode(I)I
                            move-result p1
                            invoke-static {p2}, $PLAYER->enableZenMode(I)I
                            move-result p2
                            """
                    )
                }
            } ?: throw SwitchToggleColorFingerprint.exception
        } ?: throw MiniPlayerConstructorFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode_podcast",
            "false",
            "revanced_enable_zen_mode"
        )

    }
}