package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.overlaybuttons.fingerprints.OfflineVideoEndpointFingerprint
import app.revanced.patches.youtube.player.overlaybuttons.fingerprints.PiPPlaybackFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(dependencies = [MainActivityResolvePatch::class])
object OverlayButtonsBytecodePatch : BytecodePatch(
    setOf(
        OfflineVideoEndpointFingerprint,
        PiPPlaybackFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {
        OfflineVideoEndpointFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static/range {p3 .. p3}, $INTEGRATIONS_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    """, ExternalLabel("show_native_downloader", getInstruction(0))
            )
        }

        PiPPlaybackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->getExternalDownloaderLaunchedState(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        context.updatePatchStatus("$UTILS_PATH/PatchStatus;", "OverlayButtons")

    }
}
