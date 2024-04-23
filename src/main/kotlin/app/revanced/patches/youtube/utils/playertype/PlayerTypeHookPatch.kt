package app.revanced.patches.youtube.utils.playertype

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playertype.fingerprint.ActionBarSearchResultsFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.PlayerTypeFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.VideoStateFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerTypeHookPatch : BytecodePatch(
    setOf(
        ActionBarSearchResultsFingerprint,
        PlayerTypeFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerTypeHookPatch;"

    private const val INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
        "$SHARED_PATH/RootView;"

    override fun execute(context: BytecodeContext) {

        PlayerTypeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {p1}, " +
                            "$INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
                )
            }
        }

        YouTubeControlsOverlayFingerprint.resultOrThrow().let { parentResult ->
            VideoStateFingerprint.also { it.resolve(context, parentResult.classDef)
            }.resultOrThrow().let {
                it.mutableMethod.apply {
                    val endIndex = it.scanResult.patternScanResult!!.endIndex
                    val videoStateFieldName =
                        getInstruction<ReferenceInstruction>(endIndex).reference

                    addInstructions(
                        0, """
                            iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                            invoke-static {v0}, $INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                            """
                    )
                }
            }
        }

        // Hook the search bar.

        // Two different layouts are used at the hooked code.
        // Insert before the first ViewGroup method call after inflating,
        // so this works regardless which layout is used.
        ActionBarSearchResultsFingerprint.resultOrThrow().mutableMethod.apply {
            val instructionIndex = getTargetIndexWithMethodReferenceName("setLayoutDirection")
            val viewRegister = getInstruction<FiveRegisterInstruction>(instructionIndex).registerC

            addInstruction(
                instructionIndex,
                "invoke-static { v$viewRegister }, " +
                        "$INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR->searchBarResultsViewLoaded(Landroid/view/View;)V",
            )
        }


    }
}
