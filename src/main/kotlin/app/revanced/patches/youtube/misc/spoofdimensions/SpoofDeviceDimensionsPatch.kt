package app.revanced.patches.youtube.misc.spoofdimensions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.youtube.misc.spoofdimensions.fingerprints.DeviceDimensionsModelToStringFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
object SpoofDeviceDimensionsPatch : BaseBytecodePatch(
    name = "Spoof device dimensions",
    description = "Adds an option to spoof the device dimensions which unlocks higher video qualities if they aren't available on the device.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(DeviceDimensionsModelToStringFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofDeviceDimensionsPatch;"

    override fun execute(context: BytecodeContext) {
        DeviceDimensionsModelToStringFingerprint.result?.let { result ->
            result.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }
                .addInstructions(
                    1, // Add after super call.
                    mapOf(
                        1 to "MinHeightOrWidth", // p1 = min height
                        2 to "MaxHeightOrWidth", // p2 = max height
                        3 to "MinHeightOrWidth", // p3 = min width
                        4 to "MaxHeightOrWidth"  // p4 = max width
                    ).map { (parameter, method) ->
                        """
                            invoke-static { p$parameter }, $INTEGRATIONS_CLASS_DESCRIPTOR->get$method(I)I
                            move-result p$parameter
                            """
                    }.joinToString("\n") { it }
                )
        } ?: throw DeviceDimensionsModelToStringFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_DEVICE_DIMENSIONS"
            )
        )

        SettingsPatch.updatePatchStatus("Spoof device dimensions")
    }
}