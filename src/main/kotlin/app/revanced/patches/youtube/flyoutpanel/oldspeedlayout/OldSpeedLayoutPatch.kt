package app.revanced.patches.youtube.flyoutpanel.oldspeedlayout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints.CustomPlaybackSpeedIntegrationsFingerprint
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints.PlaybackRateBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField

@Patch(
    dependencies = [
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class
    ]
)
object OldSpeedLayoutPatch : BytecodePatch(
    setOf(
        CustomPlaybackSpeedIntegrationsFingerprint,
        PlaybackRateBottomSheetClassFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/CustomPlaybackSpeedPatch;"

    private lateinit var playbackRateBottomSheetClass: String
    private lateinit var playbackRateBottomSheetBuilderMethod: String

    override fun execute(context: BytecodeContext) {

        /**
         * Input 'playbackRateBottomSheetClass' in FlyoutPanelPatch.
         */
        PlaybackRateBottomSheetClassFingerprint.result?.let {
            it.mutableMethod.apply {
                playbackRateBottomSheetClass = definingClass
                playbackRateBottomSheetBuilderMethod =
                    it.mutableClass.methods.find { method -> method.parameters.isEmpty() && method.returnType == "V" }
                        ?.name
                        ?: throw PatchException("Could not find PlaybackRateBottomSheetBuilderMethod")

                addInstruction(
                    0,
                    "sput-object p0, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass"
                )
            }
        } ?: throw PlaybackRateBottomSheetClassFingerprint.exception

        /**
         * Create a static field in the patch
         * Add a call the Playback Speed Bottom Sheet Fragment method
         */
        CustomPlaybackSpeedIntegrationsFingerprint.result?.let {
            it.mutableMethod.apply {
                // Create a static field 'playbackRateBottomSheetClass' in FlyoutPanelPatch.
                it.mutableClass.staticFields.add(
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

                removeInstruction(1)
                removeInstruction(0)

                addInstructionsWithLabels(
                    0, """
                        sget-object v0, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$playbackRateBottomSheetClass
                        if-nez v0, :not_null
                        return-void
                        :not_null
                        invoke-virtual {v0}, $playbackRateBottomSheetClass->$playbackRateBottomSheetBuilderMethod()V
                        """
                )
            }
        } ?: throw CustomPlaybackSpeedIntegrationsFingerprint.exception

        BottomSheetRecyclerViewPatch.injectCall("$INTEGRATIONS_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

    }
}
