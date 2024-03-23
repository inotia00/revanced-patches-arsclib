package app.revanced.patches.music.player.nextprevious

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.utils.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.player.nextprevious.fingerprints.MiniPlayerParentFingerprint
import app.revanced.patches.music.player.nextprevious.fingerprints.MppWatchWhileLayoutFingerprint
import app.revanced.patches.music.player.nextprevious.fingerprints.NextButtonVisibilityFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TopEnd
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TopStart
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Enable next previous button",
    description = "Adds an options to show the next and previous buttons to the miniplayer.",
    dependencies = [
        MiniPlayerButtonResourcePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
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
object MiniPlayerButtonPatch : BytecodePatch(
    setOf(
        MiniPlayerConstructorFingerprint,
        MiniPlayerParentFingerprint,
        MppWatchWhileLayoutFingerprint
    )
) {
    private const val NEXT_BUTTON_FIELD_NAME =
        "nextButton"
    private const val PREVIOUS_BUTTON_FIELD_NAME =
        "previousButton"
    private const val NEXT_BUTTON_METHOD_NAME =
        "setNextButtonOnClickListener"
    private const val PREVIOUS_BUTTON_METHOD_NAME =
        "setPreviousButtonOnClickListener"

    private var arrayCount = 1

    override fun execute(context: BytecodeContext) {

        val miniPlayerConstructorMutableMethod =
            MiniPlayerConstructorFingerprint.result?.mutableMethod
            ?: throw MiniPlayerConstructorFingerprint.exception

        val mppWatchWhileLayoutMutableMethod =
            MppWatchWhileLayoutFingerprint.result?.mutableMethod
                ?: throw MppWatchWhileLayoutFingerprint.exception

        if (!SettingsPatch.upward0642) {
            MiniPlayerParentFingerprint.result?.let { parentResult ->
                // Resolves fingerprints
                NextButtonVisibilityFingerprint.resolve(context, parentResult.classDef)

                NextButtonVisibilityFingerprint.result?.let {
                    it.mutableMethod.apply {
                        val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                        val targetRegister =
                            getInstruction<OneRegisterInstruction>(targetIndex).registerA

                        addInstructions(
                            targetIndex + 1, """
                                invoke-static {v$targetRegister}, $PLAYER->enableMiniPlayerNextButton(Z)Z
                                move-result v$targetRegister
                                """
                        )
                    }
                } ?: throw NextButtonVisibilityFingerprint.exception
            } ?: throw MiniPlayerParentFingerprint.exception
        } else {
            miniPlayerConstructorMutableMethod.setInstanceFieldValue(NEXT_BUTTON_METHOD_NAME, TopStart)
            mppWatchWhileLayoutMutableMethod.setStaticFieldValue(NEXT_BUTTON_FIELD_NAME, TopStart)
        }

        miniPlayerConstructorMutableMethod.setInstanceFieldValue(PREVIOUS_BUTTON_METHOD_NAME, TopEnd)
        mppWatchWhileLayoutMutableMethod.setStaticFieldValue(PREVIOUS_BUTTON_FIELD_NAME, TopEnd)

        mppWatchWhileLayoutMutableMethod.setViewArray()

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_mini_player_next_button",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_mini_player_previous_button",
            "false"
        )

    }

    private fun MutableMethod.setViewArray() {
        val miniPlayerPlayPauseReplayButtonIndex = getWideLiteralInstructionIndex(MiniPlayerPlayPauseReplayButton)
        val invokeStaticIndex = getTargetIndex(miniPlayerPlayPauseReplayButtonIndex, Opcode.INVOKE_STATIC)
        val viewArrayRegister = getInstruction<FiveRegisterInstruction>(invokeStaticIndex).registerC

        addInstructions(
            invokeStaticIndex, """
                invoke-static {v$viewArrayRegister}, $PLAYER->getViewArray([Landroid/view/View;)[Landroid/view/View;
                move-result-object v$viewArrayRegister
                """
        )
    }

    private fun MutableMethod.setInstanceFieldValue(
        methodName: String,
        viewId: Long
    ) {
        val miniPlayerPlayPauseReplayButtonIndex = getWideLiteralInstructionIndex(MiniPlayerPlayPauseReplayButton)
        val miniPlayerPlayPauseReplayButtonRegister = getInstruction<OneRegisterInstruction>(miniPlayerPlayPauseReplayButtonIndex).registerA
        val findViewByIdIndex = getTargetIndex(miniPlayerPlayPauseReplayButtonIndex, Opcode.INVOKE_VIRTUAL)
        val parentViewRegister = getInstruction<FiveRegisterInstruction>(findViewByIdIndex).registerC

        addInstructions(
            miniPlayerPlayPauseReplayButtonIndex, """
                const v$miniPlayerPlayPauseReplayButtonRegister, $viewId
                invoke-virtual {v$parentViewRegister, v$miniPlayerPlayPauseReplayButtonRegister}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                move-result-object v$miniPlayerPlayPauseReplayButtonRegister
                invoke-static {v$miniPlayerPlayPauseReplayButtonRegister}, $PLAYER->$methodName(Landroid/view/View;)V
                """
        )
    }

    private fun MutableMethod.setStaticFieldValue(
        fieldName: String,
        viewId: Long
    ) {
        val miniPlayerPlayPauseReplayButtonIndex = getWideLiteralInstructionIndex(MiniPlayerPlayPauseReplayButton)
        val constRegister = getInstruction<OneRegisterInstruction>(miniPlayerPlayPauseReplayButtonIndex).registerA
        val findViewByIdIndex = getTargetIndex(miniPlayerPlayPauseReplayButtonIndex, Opcode.INVOKE_VIRTUAL)
        val findViewByIdRegister = getInstruction<FiveRegisterInstruction>(findViewByIdIndex).registerC

        addInstructions(
            miniPlayerPlayPauseReplayButtonIndex, """
                const v$constRegister, $viewId
                invoke-virtual {v$findViewByIdRegister, v$constRegister}, $definingClass->findViewById(I)Landroid/view/View;
                move-result-object v$constRegister
                sput-object v$constRegister, $PLAYER->$fieldName:Landroid/view/View;
                """
        )
    }
}