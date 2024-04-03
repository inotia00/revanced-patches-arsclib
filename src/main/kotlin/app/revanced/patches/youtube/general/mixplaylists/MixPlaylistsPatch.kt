package app.revanced.patches.youtube.general.mixplaylists

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.ElementParserFingerprint
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.ElementParserParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object MixPlaylistsPatch : BaseBytecodePatch(
    name = "Hide mix playlists",
    description = "Adds an option to hide mix playlists in feed.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(ElementParserParentFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/MixPlaylistsFilter;"

    override fun execute(context: BytecodeContext) {

        ElementParserParentFingerprint.resultOrThrow().let { parentResult ->
            ElementParserFingerprint.resolve(context, parentResult.classDef)

            ElementParserFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val freeRegister = implementation!!.registerCount - parameters.size - 2
                    val insertIndex = indexOfFirstInstruction {
                        val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                        reference?.parameterTypes?.size == 1
                                && reference.parameterTypes.first() == "[B"
                                && reference.returnType.startsWith("L")
                    }

                    val objectIndex = getTargetIndex(Opcode.MOVE_OBJECT)
                    val objectRegister = getInstruction<TwoRegisterInstruction>(objectIndex).registerA

                    val jumpIndex = it.scanResult.patternScanResult!!.startIndex

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {v$objectRegister, v$freeRegister}, $FILTER_CLASS_DESCRIPTOR->filterMixPlaylists(Ljava/lang/Object;[B)Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :filter
                            """, ExternalLabel("filter", getInstruction(jumpIndex))
                    )

                    addInstruction(
                        0,
                        "move-object/from16 v$freeRegister, p3"
                    )
                }
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_MIX_PLAYLISTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide mix playlists")

    }
}
