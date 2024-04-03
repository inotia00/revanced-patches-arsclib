package app.revanced.patches.youtube.misc.codec.video

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.codec.video.fingerprints.VideoPrimaryFingerprint
import app.revanced.patches.youtube.misc.codec.video.fingerprints.VideoPropsFingerprint
import app.revanced.patches.youtube.misc.codec.video.fingerprints.VideoPropsParentFingerprint
import app.revanced.patches.youtube.misc.codec.video.fingerprints.VideoSecondaryFingerprint
import app.revanced.patches.youtube.utils.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithFieldReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object ForceVideoCodecPatch : BaseBytecodePatch(
    name = "Force video codec",
    description = "Adds an option to force the video codec.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        LayoutSwitchFingerprint,
        VideoPropsParentFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/CodecOverridePatch;"

    private const val INTEGRATIONS_CLASS_METHOD_REFERENCE =
        "$INTEGRATIONS_CLASS_DESCRIPTOR->shouldForceCodec(Z)Z"

    override fun execute(context: BytecodeContext) {

        LayoutSwitchFingerprint.resultOrThrow().classDef.let { classDef ->
            arrayOf(
                VideoPrimaryFingerprint,
                VideoSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.also { it.resolve(context, classDef) }.resultOrThrow().injectOverride()
            }
        }

        VideoPropsParentFingerprint.resultOrThrow().let { parentResult ->
            VideoPropsFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.resultOrThrow().mutableMethod.let {
                mapOf(
                    "MANUFACTURER" to "getManufacturer",
                    "BRAND" to "getBrand",
                    "MODEL" to "getModel"
                ).forEach { (fieldName, descriptor) ->
                    it.hookProps(fieldName, descriptor)
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_VIDEO_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("Force video codec")

    }

    private fun MethodFingerprintResult.injectOverride() {
        mutableMethod.apply {
            val startIndex = scanResult.patternScanResult!!.startIndex
            val endIndex = scanResult.patternScanResult!!.endIndex

            val startRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
            val endRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

            hookOverride(endIndex + 1, endRegister)
            removeInstruction(endIndex)
            hookOverride(startIndex + 1, startRegister)
            removeInstruction(startIndex)
        }
    }

    private fun MutableMethod.hookOverride(
        index: Int,
        register: Int
    ) {
        addInstructions(
            index, """
                invoke-static {v$register}, $INTEGRATIONS_CLASS_METHOD_REFERENCE
                move-result v$register
                return v$register
                """
        )
    }

    private fun MutableMethod.hookProps(
        fieldName: String,
        descriptor: String
    ) {
        val index = getTargetIndexWithFieldReferenceName(fieldName)
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index + 1, """
                invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$register
                """
        )
    }

}
