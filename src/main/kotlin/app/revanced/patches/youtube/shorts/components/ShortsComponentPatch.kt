package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsButtonFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsInfoPanelFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPaidPromotionFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPivotFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPivotLegacyFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynRemix
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynShare
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelForcedMuteButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPivotButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPlayerBadge
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPlayerBadge2
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPlayerInfoPanel
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightDislikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightLikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.RightComment
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide shorts components",
    description = "Adds options to hide components related to YouTube Shorts.",
    dependencies = [
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ShortsNavigationBarPatch::class,
        ShortsSubscriptionsButtonPatch::class,
        ShortsToolBarPatch::class
    ],
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
object ShortsComponentPatch : BytecodePatch(
    setOf(
        ShortsButtonFingerprint,
        ShortsInfoPanelFingerprint,
        ShortsPaidPromotionFingerprint,
        ShortsPivotFingerprint,
        ShortsPivotLegacyFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Comment button
         */
        ShortsButtonFingerprint.hideButton(RightComment, "hideShortsPlayerCommentsButton", false)

        /**
         * Dislike button
         */
        ShortsButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(ReelRightDislikeIcon)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA

                val jumpIndex = getTargetIndex(constIndex, Opcode.CONST_CLASS) + 2

                addInstructionsWithLabels(
                    constIndex + 1, """
                        invoke-static {}, $SHORTS->hideShortsPlayerDislikeButton()Z
                        move-result v$constRegister
                        if-nez v$constRegister, :hide
                        const v$constRegister, $ReelRightDislikeIcon
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } ?: throw ShortsButtonFingerprint.exception

        /**
         * Info panel
         */
        ShortsInfoPanelFingerprint.hideButtons(ReelPlayerInfoPanel, "hideShortsPlayerInfoPanel(Landroid/view/ViewGroup;)Landroid/view/ViewGroup;")

        /**
         * Like button
         */
        ShortsButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralInstructionIndex(ReelRightLikeIcon)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                val jumpIndex = getTargetIndex(insertIndex, Opcode.CONST_CLASS) + 2

                addInstructionsWithLabels(
                    insertIndex + 1, """
                        invoke-static {}, $SHORTS->hideShortsPlayerLikeButton()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hide
                        const v$insertRegister, $ReelRightLikeIcon
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } ?: throw ShortsButtonFingerprint.exception

        /**
         * Paid promotion
         */
        ShortsPaidPromotionFingerprint.hideButtons(ReelPlayerBadge, "hideShortsPlayerPaidPromotionBanner(Landroid/view/ViewStub;)Landroid/view/ViewStub;")
        ShortsPaidPromotionFingerprint.hideButtons(ReelPlayerBadge2, "hideShortsPlayerPaidPromotionBanner(Landroid/view/ViewStub;)Landroid/view/ViewStub;")

        /**
         * Pivot button
         */
        ShortsPivotLegacyFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(ReelForcedMuteButton)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val insertIndex = getTargetIndexReversed(targetIndex, Opcode.IF_EQZ)
                val jumpIndex = getTargetIndex(targetIndex, Opcode.GOTO)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS->hideShortsPlayerPivotButton()Z
                        move-result v$targetRegister
                        if-nez v$targetRegister, :hide
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } ?: ShortsPivotFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(ReelPivotButton)
                val insertIndex = getTargetIndexReversed(targetIndex, Opcode.INVOKE_STATIC) + 1

                hideButtons(insertIndex, "hideShortsPlayerPivotButton(Ljava/lang/Object;)Ljava/lang/Object;")
            }
        } ?: throw ShortsPivotFingerprint.exception

        /**
         * Remix button
         */
        ShortsButtonFingerprint.hideButton(ReelDynRemix, "hideShortsPlayerRemixButton", true)

        /**
         * Share button
         */
        ShortsButtonFingerprint.hideButton(ReelDynShare, "hideShortsPlayerShareButton", true)

        LithoFilterPatch.addFilter("$COMPONENTS_PATH/ShortsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: HIDE_SHORTS_SHELF",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: HIDE_SHORTS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide shorts components")

    }

    private fun MethodFingerprint.hideButton(
        id: Long,
        descriptor: String,
        reversed: Boolean
    ) {
        result?.let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(id)
                val insertIndex = if (reversed)
                    getTargetIndexReversed(constIndex, Opcode.CHECK_CAST)
                else
                    getTargetIndex(constIndex, Opcode.CHECK_CAST)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $SHORTS->$descriptor(Landroid/view/View;)V"
                )
            }
        } ?: throw exception
    }

    private fun MethodFingerprint.hideButtons(
        id: Long,
        descriptor: String
    ) {
        result?.let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(id)
                val insertIndex = getTargetIndex(constIndex, Opcode.CHECK_CAST)

                hideButtons(insertIndex, descriptor)
            }
        } ?: throw exception
    }

    private fun MutableMethod.hideButtons(
        insertIndex: Int,
        descriptor: String
    ) {
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstructions(
            insertIndex + 1, """
                invoke-static {v$insertRegister}, $SHORTS->$descriptor
                move-result-object v$insertRegister
                """
        )
    }
}
