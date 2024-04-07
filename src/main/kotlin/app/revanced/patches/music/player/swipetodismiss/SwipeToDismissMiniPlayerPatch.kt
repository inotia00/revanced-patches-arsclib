package app.revanced.patches.music.player.swipetodismiss

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.player.swipetodismiss.fingerprints.HandleSearchRenderedFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.HandleSignInEventFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.InteractionLoggingEnumFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.MiniPlayerDefaultTextFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.MiniPlayerDefaultViewVisibilityFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.MusicActivityWidgetFingerprint
import app.revanced.patches.music.player.swipetodismiss.fingerprints.SwipeToCloseFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithFieldReferenceType
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import kotlin.properties.Delegates

@Suppress("unused")
object SwipeToDismissMiniPlayerPatch : BaseBytecodePatch(
    name = "Enable swipe to dismiss miniplayer",
    description = "Adds an option to swipe down to dismiss the miniplayer.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        HandleSearchRenderedFingerprint,
        InteractionLoggingEnumFingerprint,
        MiniPlayerDefaultTextFingerprint,
        MiniPlayerDefaultViewVisibilityFingerprint,
        MusicActivityWidgetFingerprint,
        SwipeToCloseFingerprint
    )
) {
    private var widgetIndex by Delegates.notNull<Int>()
    private lateinit var iGetObjectReference: Reference
    private lateinit var invokeInterfacePrimaryReference: Reference
    private lateinit var checkCastReference: Reference
    private lateinit var sGetObjectReference: Reference
    private lateinit var newInstanceReference: Reference
    private lateinit var invokeStaticReference: Reference
    private lateinit var invokeDirectReference: Reference
    private lateinit var invokeInterfaceSecondaryReference: Reference

    override fun execute(context: BytecodeContext) {

        if (!SettingsPatch.upward0642) {
            SwipeToCloseFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = implementation!!.instructions.size - 1
                    val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            }
        } else {

            // Dismiss mini player by swiping down

            InteractionLoggingEnumFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val stringIndex = getStringInstructionIndex("INTERACTION_LOGGING_GESTURE_TYPE_SWIPE")
                    val sPutObjectIndex = getTargetIndex(stringIndex, Opcode.SPUT_OBJECT)

                    sGetObjectReference = getInstruction<ReferenceInstruction>(sPutObjectIndex).reference
                }
            }

            MusicActivityWidgetFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    widgetIndex = getWideLiteralInstructionIndex(79500)

                    iGetObjectReference = getReference(Opcode.IGET_OBJECT, true)
                    invokeInterfacePrimaryReference = getReference(Opcode.INVOKE_INTERFACE, true)
                    checkCastReference = getReference(Opcode.CHECK_CAST, true)
                    newInstanceReference = getReference(Opcode.NEW_INSTANCE, true)
                    invokeStaticReference = getReference(Opcode.INVOKE_STATIC, false)
                    invokeDirectReference = getReference(Opcode.INVOKE_DIRECT, false)
                    invokeInterfaceSecondaryReference = getReference(Opcode.INVOKE_INTERFACE, false)
                }
            }

            HandleSearchRenderedFingerprint.resultOrThrow().let { parentResult ->
                // Resolves fingerprints
                HandleSignInEventFingerprint.resolve(context, parentResult.classDef)

                HandleSignInEventFingerprint.resultOrThrow().let {
                    val dismissBehaviorMethod = context.toMethodWalker(it.method)
                        .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                        .getMethod() as MutableMethod

                    dismissBehaviorMethod.apply {
                        val insertIndex = getTargetIndexWithFieldReferenceType("Ljava/util/concurrent/atomic/AtomicBoolean;")
                        val primaryRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerB
                        val secondaryRegister = primaryRegister + 1
                        val tertiaryRegister = secondaryRegister + 1

                        val freeRegister = implementation!!.registerCount - parameters.size - 2

                        addInstructionsWithLabels(
                            insertIndex, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer()Z
                                move-result v$freeRegister
                                if-nez v$freeRegister, :dismiss
                                iget-object v$primaryRegister, v$primaryRegister, $iGetObjectReference
                                invoke-interface {v$primaryRegister}, $invokeInterfacePrimaryReference
                                move-result-object v$primaryRegister
                                check-cast v$primaryRegister, $checkCastReference
                                sget-object v$secondaryRegister, $sGetObjectReference
                                new-instance v$tertiaryRegister, $newInstanceReference
                                const p0, 0x878b
                                invoke-static {p0}, $invokeStaticReference
                                move-result-object p0
                                invoke-direct {v$tertiaryRegister, p0}, $invokeDirectReference
                                const/4 p0, 0x0
                                invoke-interface {v$primaryRegister, v$secondaryRegister, v$tertiaryRegister, p0}, $invokeInterfaceSecondaryReference
                                return-void
                                """, ExternalLabel("dismiss", getInstruction(insertIndex))
                        )
                    }
                }
            }

            // Endregion

            // Hides default text display when the app is cold started

            MiniPlayerDefaultTextFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerB

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer(Ljava/lang/Object;)Ljava/lang/Object;
                            move-result-object v$insertRegister
                            """
                    )
                }
            }

            // Endregion

            // Hides default text display after dismissing the mini player

            MiniPlayerDefaultViewVisibilityFingerprint.resultOrThrow().let {
                it.mutableClass.methods.find { method ->
                    method.parameters == listOf("Landroid/view/View;", "I")
                }?.apply {
                    val bottomSheetBehaviorIndex = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_VIRTUAL
                                && instruction.getReference<MethodReference>()?.definingClass == "Lcom/google/android/material/bottomsheet/BottomSheetBehavior;"
                                && instruction.getReference<MethodReference>()?.parameterTypes?.first() == "Z"
                    }
                    if (bottomSheetBehaviorIndex < 0)
                        throw PatchException("Could not find bottomSheetBehaviorIndex")

                    val freeRegister = getInstruction<FiveRegisterInstruction>(bottomSheetBehaviorIndex).registerD

                    addInstructionsWithLabels(
                        bottomSheetBehaviorIndex - 2, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :dismiss
                            """, ExternalLabel("dismiss", getInstruction(bottomSheetBehaviorIndex + 1))
                    )
                } ?: throw PatchException("Could not find targetMethod")

            }

            // Endregion

        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_swipe_to_dismiss_mini_player",
            "true"
        )

    }

    private fun MutableMethod.getReference(
        opcode: Opcode,
        reversed: Boolean
    ): Reference {
        val targetIndex = if (reversed)
            getTargetIndexReversed(widgetIndex, opcode)
        else
            getTargetIndex(widgetIndex, opcode)

        return getInstruction<ReferenceInstruction>(targetIndex).reference
    }
}