package app.revanced.patches.music.general.redirection

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.redirection.fingerprints.DislikeButtonOnClickListenerFingerprint
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable dislike redirection",
    description = "Adds an option to disable redirection to the next track when clicking dislike button.",
    dependencies = [SettingsPatch::class],
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
object DislikeRedirectionPatch : BytecodePatch(
    setOf(DislikeButtonOnClickListenerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        DislikeButtonOnClickListenerFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val jumpIndex = it.scanResult.patternScanResult!!.endIndex + 1
                val freeRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA

                addInstructionsWithLabels(
                    startIndex + 1, """
                        invoke-static {}, $GENERAL->disableDislikeRedirection()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :disable
                        """, ExternalLabel("disable", getInstruction(jumpIndex))
                )
            }
        } ?: throw DislikeButtonOnClickListenerFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

    }
}
