package app.revanced.patches.music.general.redirection

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.redirection.fingerprints.DislikeButtonOnClickListenerFingerprint
import app.revanced.patches.music.utils.fingerprints.PendingIntentReceiverFingerprint
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

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
    setOf(
        DislikeButtonOnClickListenerFingerprint,
        PendingIntentReceiverFingerprint
    )
) {
    private lateinit var onClickReference: Reference

    override fun execute(context: BytecodeContext) {

        PendingIntentReceiverFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = getStringInstructionIndex("YTM Dislike")
                val onClickRelayIndex = getTargetIndexReversed(startIndex, Opcode.INVOKE_VIRTUAL)
                val onClickRelayMethod = getWalkerMethod(context, onClickRelayIndex)

                onClickRelayMethod.apply {
                    val onClickMethodIndex = getTargetIndexReversed(Opcode.INVOKE_DIRECT)
                    val onClickMethod = getWalkerMethod(context, onClickMethodIndex)

                    onClickMethod.apply {
                        val onClickIndex = indexOfFirstInstruction {
                            val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                            opcode == Opcode.INVOKE_INTERFACE
                                    && reference?.returnType == "V"
                                    && reference.parameterTypes.size == 1
                        }
                        onClickReference = getInstruction<ReferenceInstruction>(onClickIndex).reference

                        injectCall(onClickIndex)
                    }
                }
            }
        } ?: throw PendingIntentReceiverFingerprint.exception

        DislikeButtonOnClickListenerFingerprint.result?.let {
            it.mutableMethod.apply {
                val onClickIndex = getTargetIndexWithReference(onClickReference.toString())
                injectCall(onClickIndex)
            }
        } ?: throw DislikeButtonOnClickListenerFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

    }

    private fun MutableMethod.injectCall(onClickIndex: Int) {
        val targetIndex = getTargetIndexReversed(onClickIndex, Opcode.IF_EQZ)
        val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        addInstructionsWithLabels(
            targetIndex + 1, """
                invoke-static {}, $GENERAL->disableDislikeRedirection()Z
                move-result v$insertRegister
                if-nez v$insertRegister, :disable
                """, ExternalLabel("disable", getInstruction(onClickIndex + 1))
        )
    }
}
