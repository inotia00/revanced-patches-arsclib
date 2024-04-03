package app.revanced.patches.youtube.player.suggestedvideooverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints.CoreContainerBuilderFingerprint
import app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints.MiniPlayerPlayButtonFingerprint
import app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints.TouchAreaOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object SuggestedVideoOverlayPatch : BaseBytecodePatch(
    name = "Hide suggested video overlay",
    description = "Adds an option to hide the suggested video overlay at the end of videos.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CoreContainerBuilderFingerprint,
        TouchAreaOnClickListenerFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        CoreContainerBuilderFingerprint.resultOrThrow().let { parentResult ->
            parentResult.mutableMethod.apply {
                val addOnClickEventListenerIndex = parentResult.scanResult.patternScanResult!!.endIndex - 1
                val viewRegister = getInstruction<FiveRegisterInstruction>(addOnClickEventListenerIndex).registerC

                addInstruction(
                    addOnClickEventListenerIndex + 1,
                    "invoke-static {v$viewRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedVideoOverlay(Landroid/widget/ImageView;)V"
                )
            }

            // Resolves fingerprints
            MiniPlayerPlayButtonFingerprint.resolve(context, parentResult.classDef)

            MiniPlayerPlayButtonFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedVideoOverlay()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                }
            }
        }

        TouchAreaOnClickListenerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertMethod = it.mutableClass.methods.find { method -> method.parameters == listOf("Landroid/view/View${'$'}OnClickListener;") }

                insertMethod?.apply {
                    val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName("setOnClickListener")
                    val setOnClickListenerRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                    addInstruction(
                        setOnClickListenerIndex + 1,
                        "invoke-static {v$setOnClickListenerRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedVideoOverlayAutoPlay(Landroid/view/View;)V"
                    )
                } ?: throw PatchException("Failed to find setOnClickListener method")
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: PLAYER_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_SUGGESTED_VIDEO_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Hide suggested video overlay")

    }
}