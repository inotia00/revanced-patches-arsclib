package app.revanced.patches.youtube.swipe.controls

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.swipe.controls.fingerprints.FullScreenEngagementOverlayFingerprint
import app.revanced.patches.youtube.swipe.controls.fingerprints.HDRBrightnessFingerprint
import app.revanced.patches.youtube.swipe.controls.fingerprints.SwipeControlsHostActivityFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SWIPE_PATH
import app.revanced.patches.youtube.utils.lockmodestate.LockModeStateHookPatch
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch.mainActivityMutableClass
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementOverlay
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.transformMethods
import app.revanced.util.traverseClassHierarchy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

@Suppress("unused")
object SwipeControlsPatch : BaseBytecodePatch(
    name = "Swipe controls",
    description = "Adds options to enable and configure volume and brightness swipe controls.",
    dependencies = setOf(
        LockModeStateHookPatch::class,
        MainActivityResolvePatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FullScreenEngagementOverlayFingerprint,
        HDRBrightnessFingerprint,
        SwipeControlsHostActivityFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$SWIPE_PATH/SwipeControlsPatch;"

    override fun execute(context: BytecodeContext) {
        val wrapperClass = SwipeControlsHostActivityFingerprint.resultOrThrow().mutableClass
        val targetClass = mainActivityMutableClass

        // inject the wrapper class from integrations into the class hierarchy of MainActivity (WatchWhileActivity)
        wrapperClass.setSuperClass(targetClass.superclass)
        targetClass.setSuperClass(wrapperClass.type)

        // ensure all classes and methods in the hierarchy are non-final, so we can override them in integrations
        context.traverseClassHierarchy(targetClass) {
            accessFlags = accessFlags and AccessFlags.FINAL.value.inv()
            transformMethods {
                ImmutableMethod(
                    definingClass,
                    name,
                    parameters,
                    returnType,
                    accessFlags and AccessFlags.FINAL.value.inv(),
                    annotations,
                    hiddenApiRestrictions,
                    implementation
                ).toMutable()
            }
        }

        FullScreenEngagementOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = getWideLiteralInstructionIndex(FullScreenEngagementOverlay) + 3
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstruction(
                    viewIndex + 1,
                    "sput-object v$viewRegister, $INTEGRATIONS_CLASS_DESCRIPTOR->engagementOverlay:Landroid/view/View;"
                )
            }
        }

        HDRBrightnessFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->disableHDRAutoBrightness()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
            }

            /**
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: DISABLE_HDR_BRIGHTNESS"
                )
            )
        } // no exceptions are raised for compatibility with all versions.

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SWIPE_SETTINGS",
                "SETTINGS: SWIPE_CONTROLS"
            )
        )

        SettingsPatch.updatePatchStatus("Swipe controls")

        contexts.copyResources(
            "youtube/swipecontrols",
            ResourceGroup(
                "drawable",
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml"
            )
        )
    }
}