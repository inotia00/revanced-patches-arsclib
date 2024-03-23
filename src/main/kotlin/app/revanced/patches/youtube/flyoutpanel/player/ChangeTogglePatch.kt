package app.revanced.patches.youtube.flyoutpanel.player

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.AdditionalSettingsConfigFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.CinematicLightingFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.PlaybackLoopInitFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.PlaybackLoopOnClickListenerFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.StableVolumeFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.FLYOUT_PANEL
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Change player flyout panel toggles",
    description = "Adds an option to use text toggles instead of switch toggles within the additional settings menu.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39"
            ]
        )
    ]
)
@Suppress("unused")
object ChangeTogglePatch : BytecodePatch(
    setOf(
        AdditionalSettingsConfigFingerprint,
        CinematicLightingFingerprint,
        PlaybackLoopOnClickListenerFingerprint,
        StableVolumeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val additionalSettingsConfigResult = AdditionalSettingsConfigFingerprint.result
            ?: throw AdditionalSettingsConfigFingerprint.exception

        val additionalSettingsConfigMethod = additionalSettingsConfigResult.mutableMethod
        val methodToCall = additionalSettingsConfigMethod.definingClass + "->" + additionalSettingsConfigMethod.name + "()Z"

        // Resolves fingerprints
        val playbackLoopOnClickListenerResult = PlaybackLoopOnClickListenerFingerprint.result
            ?: throw PlaybackLoopOnClickListenerFingerprint.exception
        PlaybackLoopInitFingerprint.resolve(context, playbackLoopOnClickListenerResult.classDef)

        arrayOf(
            CinematicLightingFingerprint,
            PlaybackLoopInitFingerprint,
            PlaybackLoopOnClickListenerFingerprint,
            StableVolumeFingerprint
        ).forEach { fingerprint ->
            injectCall(fingerprint, methodToCall)
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: PLAYER_FLYOUT_PANEL_ADDITIONAL_SETTINGS_HEADER",
                "SETTINGS: CHANGE_PLAYER_FLYOUT_PANEL_TOGGLE"
            )
        )

        SettingsPatch.updatePatchStatus("Change player flyout panel toggles")
    }

    private fun injectCall(
        fingerprint: MethodFingerprint,
        methodToCall: String
    ) {
        fingerprint.result?.let {
            it.mutableMethod.apply {
                val referenceIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL
                            && (instruction as ReferenceInstruction).reference.toString().endsWith(methodToCall)
                }
                if (referenceIndex > 0) {
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(referenceIndex + 1).registerA

                    addInstructions(
                        referenceIndex + 2, """
                            invoke-static {v$insertRegister}, $FLYOUT_PANEL->changeSwitchToggle(Z)Z
                            move-result v$insertRegister
                            """
                    )
                } else {
                    if (fingerprint == CinematicLightingFingerprint)
                        injectCinematicLightingMethod()
                    else
                        throw PatchException("Target reference'$methodToCall' was not found in ${this.javaClass.simpleName}.")
                }
            }
        } ?: throw fingerprint.exception
    }

    private fun injectCinematicLightingMethod() {
        val stableVolumeMethod = StableVolumeFingerprint.result?.mutableMethod
            ?: throw StableVolumeFingerprint.exception

        val stringReferenceIndex = stableVolumeMethod.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL
                    && (instruction as ReferenceInstruction).reference.toString().endsWith("(Ljava/lang/String;Ljava/lang/String;)V")
        }
        if (stringReferenceIndex < 0)
            throw PatchException("Target reference was not found in ${StableVolumeFingerprint.javaClass.simpleName}.")

        val stringReference = stableVolumeMethod.getInstruction<ReferenceInstruction>(stringReferenceIndex).reference

        CinematicLightingFingerprint.result?.let {
            it.mutableMethod.apply {
                val stringIndex = getStringInstructionIndex("menu_item_cinematic_lighting")

                val checkCastIndex = getTargetIndexReversed(stringIndex, Opcode.CHECK_CAST)
                val iGetObjectPrimaryIndex = getTargetIndexReversed(checkCastIndex, Opcode.IGET_OBJECT)
                val iGetObjectSecondaryIndex = getTargetIndex(checkCastIndex, Opcode.IGET_OBJECT)

                val checkCastReference = getInstruction<ReferenceInstruction>(checkCastIndex).reference
                val iGetObjectPrimaryReference = getInstruction<ReferenceInstruction>(iGetObjectPrimaryIndex).reference
                val iGetObjectSecondaryReference = getInstruction<ReferenceInstruction>(iGetObjectSecondaryIndex).reference

                val invokeVirtualIndex = getTargetIndex(stringIndex, Opcode.INVOKE_VIRTUAL)
                val invokeVirtualInstruction = getInstruction<FiveRegisterInstruction>(invokeVirtualIndex)
                val freeRegisterC = invokeVirtualInstruction.registerC
                val freeRegisterD = invokeVirtualInstruction.registerD
                val freeRegisterE = invokeVirtualInstruction.registerE

                val insertIndex = getTargetIndex(stringIndex, Opcode.RETURN_VOID)

                addInstructionsWithLabels(
                    insertIndex, """
                        const/4 v$freeRegisterC, 0x1
                        invoke-static {v$freeRegisterC}, $FLYOUT_PANEL->changeSwitchToggle(Z)Z
                        move-result v$freeRegisterC
                        if-nez v$freeRegisterC, :ignore
                        sget-object v$freeRegisterC, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                        if-eq v$freeRegisterC, v$freeRegisterE, :toggle_off
                        const-string v$freeRegisterE, "stable_volume_on"
                        invoke-static {v$freeRegisterE}, $FLYOUT_PANEL->getToggleString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegisterE
                        goto :set_string
                        :toggle_off
                        const-string v$freeRegisterE, "stable_volume_off"
                        invoke-static {v$freeRegisterE}, $FLYOUT_PANEL->getToggleString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegisterE
                        :set_string
                        iget-object v$freeRegisterC, p0, $iGetObjectPrimaryReference
                        check-cast v$freeRegisterC, $checkCastReference
                        iget-object v$freeRegisterC, v$freeRegisterC, $iGetObjectSecondaryReference
                        const-string v$freeRegisterD, "menu_item_cinematic_lighting"
                        invoke-virtual {v$freeRegisterC, v$freeRegisterD, v$freeRegisterE}, $stringReference
                        """, ExternalLabel("ignore", getInstruction(insertIndex))
                )
            }
        } ?: throw CinematicLightingFingerprint.exception
    }
}
