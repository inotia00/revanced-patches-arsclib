package app.revanced.patches.reddit.ad.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.ad.banner.HideBannerPatch
import app.revanced.patches.reddit.ad.comments.HideCommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithFieldReferenceName
import app.revanced.util.getTargetIndexWithMethodReferenceName
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Hide ads",
    description = "Adds options to hide ads.",
    dependencies = [HideBannerPatch::class, HideCommentAdsPatch::class, SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.reddit.frontpage",
            [
                "2023.12.0",
                "2024.04.0"
            ]
        )
    ],
    requiresIntegrations = true,
)
@Suppress("unused")
object HideAdsPatch : BytecodePatch(
    setOf(
        AdPostFingerprint,
        NewAdPostFingerprint
    )
) {
    private const val INTEGRATIONS_OLD_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/GeneralAdsPatch;->hideOldPostAds(Ljava/util/List;)Ljava/util/List;"

    private const val INTEGRATIONS_NEW_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/GeneralAdsPatch;->hideNewPostAds()Z"

    override fun execute(context: BytecodeContext) {
        // region Filter promoted ads (does not work in popular or latest feed)
        AdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithFieldReferenceName("children")
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_OLD_METHOD_DESCRIPTOR
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: throw AdPostFingerprint.exception

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        NewAdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("add")
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(targetIndex).registerD + 1

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $INTEGRATIONS_NEW_METHOD_DESCRIPTOR
                        move-result v$targetRegister
                        if-nez v$targetRegister, :show
                        """, ExternalLabel("show", getInstruction(targetIndex + 1))
                )
            }
        } ?: throw NewAdPostFingerprint.exception

        updateSettingsStatus("enableGeneralAds")

    }
}
