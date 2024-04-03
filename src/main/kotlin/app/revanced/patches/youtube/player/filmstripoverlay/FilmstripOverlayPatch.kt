package app.revanced.patches.youtube.player.filmstripoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayConfigFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayInteractionFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayParentFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayPreviewFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FineScrubbingOverlayFingerprint
import app.revanced.patches.youtube.utils.controlsoverlay.ControlsOverlayConfigPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
object FilmstripOverlayPatch : BaseBytecodePatch(
    name = "Hide filmstrip overlay",
    description = "Adds an option to hide filmstrip overlay in the video player.",
    dependencies = setOf(
        ControlsOverlayConfigPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FilmStripOverlayParentFingerprint,
        FineScrubbingOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        FilmStripOverlayParentFingerprint.resultOrThrow().classDef.let { classDef ->
            arrayOf(
                FilmStripOverlayConfigFingerprint,
                FilmStripOverlayInteractionFingerprint,
                FilmStripOverlayPreviewFingerprint
            ).forEach { fingerprint ->
                fingerprint.resolve(context, classDef)
                fingerprint.resultOrThrow().mutableMethod.injectHook()
            }
        }

        FineScrubbingOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                var insertIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val jumpIndex = getTargetIndexUpTo(insertIndex, Opcode.GOTO, Opcode.GOTO_16)
                val initialIndex = jumpIndex - 1

                if (getInstruction(insertIndex).opcode == Opcode.INVOKE_VIRTUAL)
                    insertIndex++

                val replaceInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex).reference

                addLiteralValues(insertIndex, initialIndex)

                addInstructionsWithLabels(
                    insertIndex + 1, literalComponent + """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                        move-result v${replaceInstruction.registerA}
                        if-nez v${replaceInstruction.registerA}, :hidden
                        iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(insertIndex)
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: PLAYER_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_FILMSTRIP_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Hide filmstrip overlay")

    }

    private var literalComponent: String = ""

    private fun MutableMethod.addLiteralValues(
        startIndex: Int,
        endIndex: Int
    ) {
        for (index in startIndex..endIndex) {
            val opcode = getInstruction(index).opcode
            if (opcode != Opcode.CONST_16 && opcode != Opcode.CONST_4 && opcode != Opcode.CONST)
                continue

            val register = getInstruction<OneRegisterInstruction>(index).registerA
            val value = getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

            val line =
                when (opcode) {
                    Opcode.CONST_16 -> """
                            const/16 v$register, $value
                            
                            """.trimIndent()

                    Opcode.CONST_4 -> """
                            const/4 v$register, $value
                            
                            """.trimIndent()

                    Opcode.CONST -> """
                            const v$register, $value
                            
                            """.trimIndent()

                    else -> ""
                }

            literalComponent += line
        }
    }

    private fun MutableMethod.getTargetIndexUpTo(
        startIndex: Int,
        opcode1: Opcode,
        opcode2: Opcode
    ): Int {
        for (index in startIndex until implementation!!.instructions.size) {
            if (getInstruction(index).opcode != opcode1 && getInstruction(index).opcode != opcode2)
                continue

            return index
        }
        throw PatchException("Failed to find hook method")
    }

    private fun MutableMethod.injectHook() {
        addInstructionsWithLabels(
            0, """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                move-result v0
                if-eqz v0, :shown
                const/4 v0, 0x0
                return v0
                """, ExternalLabel("shown", getInstruction(0))
        )
    }
}
