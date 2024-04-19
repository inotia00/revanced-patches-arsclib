package app.revanced.patches.youtube.utils.flyoutpanel

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patches.youtube.utils.flyoutpanel.fingerprints.PlaybackRateBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField

@Patch(description = "Hooks YouTube to open the playback speed flyout panel in the integration.")
object PlaybackSpeedFlyoutPanelHookPatch : BytecodePatch(
    setOf(PlaybackRateBottomSheetClassFingerprint)
) {
    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        PlaybackRateBottomSheetClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val playbackRateBottomSheetClass = definingClass
                val playbackRateBottomSheetBuilderMethodName =
                    it.mutableClass.methods.find { method -> method.parameters.isEmpty() && method.returnType == "V" }
                        ?.name
                        ?: throw PatchException("Could not find PlaybackRateBottomSheetBuilderMethod")

                // set playback rate bottom sheet class
                addInstruction(
                    0,
                    "sput-object p0, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass"
                )

                val videoUtilsMutableClass = context.findClass(
                    INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR
                )!!.mutableClass
                videoUtilsMutableClass.methods.single { method ->
                    method.name == "showPlaybackSpeedFlyoutMenu"
                }.apply {
                    // add playback rate bottom sheet class
                    videoUtilsMutableClass.staticFields.add(
                        ImmutableField(
                            definingClass,
                            "playbackRateBottomSheetClass",
                            playbackRateBottomSheetClass,
                            AccessFlags.PUBLIC or AccessFlags.STATIC,
                            null,
                            annotations,
                            null
                        ).toMutable()
                    )

                    // call playback rate bottom sheet method
                    addInstructionsWithLabels(
                        0, """
                            sget-object v0, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass
                            if-eqz v0, :ignore
                            invoke-virtual {v0}, $playbackRateBottomSheetClass->$playbackRateBottomSheetBuilderMethodName()V
                            :ignore
                            return-void
                            """
                    )
                }
            }
        }
    }
}
