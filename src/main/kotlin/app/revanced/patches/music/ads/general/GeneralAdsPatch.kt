package app.revanced.patches.music.ads.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.ads.general.fingerprints.FloatingLayoutFingerprint
import app.revanced.patches.music.ads.general.fingerprints.InterstitialsContainerFingerprint
import app.revanced.patches.music.ads.general.fingerprints.NotifierShelfFingerprint
import app.revanced.patches.music.ads.general.fingerprints.ShowDialogCommandFingerprint
import app.revanced.patches.music.ads.music.MusicAdsPatch
import app.revanced.patches.music.navigation.component.NavigationBarComponentPatch
import app.revanced.patches.music.utils.integrations.Constants.ADS_PATH
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.litho.LithoFilterPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ButtonContainer
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.FloatingLayout
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide general ads",
    description = "Adds options to hide general ads.",
    dependencies = [
        LithoFilterPatch::class,
        MusicAdsPatch::class,
        NavigationBarComponentPatch::class,
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
object GeneralAdsPatch : BytecodePatch(
    setOf(
        FloatingLayoutFingerprint,
        InterstitialsContainerFingerprint,
        NotifierShelfFingerprint,
        ShowDialogCommandFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/AdsFilter;"

    private const val FULLSCREEN_ADS_CLASS_DESCRIPTOR =
        "$ADS_PATH/FullscreenAdsPatch;"

    private const val PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumPromotionPatch;"

    private const val PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumRenewalPatch;"

    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Hides fullscreen ads
         * Non-litho view, used in some old clients.
         */
        InterstitialsContainerFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(InterstitialsContainer) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
                )
            }
        } ?: throw InterstitialsContainerFingerprint.exception

        /**
         * Hides fullscreen ads
         * Litho view, used in 'ShowDialogCommandOuterClass' in innertube
         */
        ShowDialogCommandFingerprint.result?.let {
            it.mutableMethod.apply {
                // In this method, custom dialog is created and shown.
                // There were no issues despite adding ¡°return-void¡± to the first index.
                //
                // If an issue occurs due to patching due to server-side changes in the future,
                // Find the instruction whose name is "show" in [MethodReference] and click the 'AlertDialog.BUTTON_POSITIVE' button.
                //
                // In this case, an instruction for 'getButton' must be added to smali, not in integrations
                // (This custom dialog cannot be cast to [AlertDialog] or [Dialog])
                //
                // See the comments below.
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->hideFullscreenAds()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )

                /*
                val dialogIndex = getTargetIndexWithMethodReferenceName("show")
                val dialogReference = getInstruction<ReferenceInstruction>(dialogIndex).reference
                val dialogDefiningClass = (dialogReference as MethodReference).definingClass
                val getButtonMethod = context.findClass(dialogDefiningClass)!!
                    .mutableClass.methods.first { method ->
                        method.parameters == listOf("I")
                                && method.returnType == "Landroid/widget/Button;"
                    }
                val getButtonCall = dialogDefiningClass + "->" + getButtonMethod.name + "(I)Landroid/widget/Button;"

                val dialogRegister = getInstruction<FiveRegisterInstruction>(dialogIndex).registerC
                val freeIndex = getTargetIndex(dialogIndex, Opcode.IF_EQZ)
                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                addInstructions(
                    dialogIndex + 1, """
                        # Get the 'AlertDialog.BUTTON_POSITIVE' from custom dialog
                        # Since this custom dialog cannot be cast to AlertDialog or Dialog,
                        # It should come from smali, not integrations.
                        const/4 v$freeRegister, -0x1
                        invoke-virtual {v$dialogRegister, $freeRegister}, $getButtonCall
                        move-result-object $freeRegister
                        invoke-static {$freeRegister}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/Button;)V
                        """
                )
                 */
            }
        } ?: throw ShowDialogCommandFingerprint.exception

        /**
         * Hides premium promotion popup
         */
        FloatingLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(FloatingLayout) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR->hidePremiumPromotion(Landroid/view/View;)V"
                )
            }
        } ?: throw FloatingLayoutFingerprint.exception

        /**
         * Hides premium renewal banner
         */
        NotifierShelfFingerprint.result?.let {
            it.mutableMethod.apply {
                val linearLayoutIndex = getWideLiteralInstructionIndex(ButtonContainer) + 3
                val linearLayoutRegister =
                    getInstruction<OneRegisterInstruction>(linearLayoutIndex).registerA

                addInstruction(
                    linearLayoutIndex + 1,
                    "invoke-static {v$linearLayoutRegister}, $PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR->hidePremiumRenewal(Landroid/widget/LinearLayout;)V"
                )
            }
        } ?: throw NotifierShelfFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ADS,
            "revanced_hide_fullscreen_ads",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ADS,
            "revanced_hide_general_ads",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ADS,
            "revanced_hide_music_ads",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ADS,
            "revanced_hide_premium_promotion",
            "true"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ADS,
            "revanced_hide_premium_renewal",
            "true"
        )
    }
}
