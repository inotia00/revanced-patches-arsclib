package app.revanced.patches.youtube.utils.playercontrols

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PlayerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsPatchFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerControlsPatch : BytecodePatch(
    setOf(
        PlayerButtonsResourcesFingerprint,
        PlayerControlsPatchFingerprint,
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerControlsPatch;"

    private lateinit var changeVisibilityMethod: MutableMethod
    private lateinit var initializeOverlayButtonsMethod: MutableMethod
    private lateinit var initializeSponsorBlockButtonsMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        // new method
        PlayerButtonsVisibilityFingerprint.resolve(
            context,
            PlayerButtonsResourcesFingerprint.resultOrThrow().mutableClass
        )
        PlayerButtonsVisibilityFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val viewRegister = getInstruction<TwoRegisterInstruction>(viewIndex).registerA

                addInstruction(
                    viewIndex + 1,
                    "invoke-static {p1, p2, v$viewRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->changeVisibility(ZZLandroid/view/View;)V"
                )
            }
        }

        // legacy method
        PlayerControlsVisibilityFingerprint.resolve(
            context,
            YouTubeControlsOverlayFingerprint.resultOrThrow().mutableClass
        )
        PlayerControlsVisibilityFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->changeVisibility(Z)V"
        )

        mapOf(
            BottomControlsInflateFingerprint to "initializeOverlayButtons",
            ControlsLayoutInflateFingerprint to "initializeSponsorBlockButtons"
        ).forEach { (fingerprint, methodName) ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val endIndex = it.scanResult.patternScanResult!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                    addInstruction(
                        endIndex + 1,
                        "invoke-static {v$viewRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                    )
                }
            }
        }

        PlayerControlsPatchFingerprint.resultOrThrow().let {
            changeVisibilityMethod = it.mutableMethod

            initializeOverlayButtonsMethod =
                it.mutableClass.methods.find { method -> method.name == "initializeOverlayButtons" }!!

            initializeSponsorBlockButtonsMethod =
                it.mutableClass.methods.find { method -> method.name == "initializeSponsorBlockButtons" }!!
        }
    }

    private fun MutableMethod.initializeHook(classDescriptor: String) =
        addInstruction(
            0,
            "invoke-static {p0}, $classDescriptor->initialize(Landroid/view/View;)V"
        )

    private fun changeVisibilityHook(classDescriptor: String) =
        changeVisibilityMethod.addInstruction(
            0,
            "invoke-static {p0, p1}, $classDescriptor->changeVisibility(ZZ)V"
        )

    internal fun hookOverlayButtons(classDescriptor: String) {
        initializeOverlayButtonsMethod.initializeHook(classDescriptor)
        changeVisibilityHook(classDescriptor)
    }

    internal fun hookSponsorBlockButtons(classDescriptor: String) {
        initializeSponsorBlockButtonsMethod.initializeHook(classDescriptor)
        changeVisibilityHook(classDescriptor)
    }
}