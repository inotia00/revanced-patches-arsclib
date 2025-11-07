package app.revanced.patches.reddit.ad.general

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.ad.comments.CommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostSectionConstructorFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostSectionToStringFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.ImmutableListBuilderFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.ImmutableListBuilderFingerprint.indexOfImmutableListBuilderInstruction
import app.revanced.patches.reddit.ad.general.fingerprints.ListingFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostLegacyFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.SubmittedListingFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide ads")
@Description("Adds options to hide ads.")
@DependsOn([SettingsPatch::class, CommentAdsPatch::class])
@RedditCompatibility
@RequiresIntegrations
@Suppress("unused")
class AdsPatch : BytecodePatch(
    listOf(
        AdPostSectionToStringFingerprint,
        ImmutableListBuilderFingerprint,
        ListingFingerprint,
        NewAdPostFingerprint,
        NewAdPostLegacyFingerprint,
        SubmittedListingFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/GeneralAdsPatch;"

        fun indexOfAddArrayListInstruction(methodDef: Method, index: Int = 0) =
            methodDef.indexOfFirstInstruction(index) {
                getReference<MethodReference>()?.toString() == "Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"
            }
    }

    override fun execute(context: BytecodeContext) {
        // region Filter promoted ads (does not work in popular or latest feed)
        listOf(
            ListingFingerprint,
            SubmittedListingFingerprint,
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().mutableMethod.apply {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.name == "children"
                }
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideOldPostAds(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        if (is_2025_40_or_greater) {
            val immutableListBuilderReference =
                with (ImmutableListBuilderFingerprint.resultOrThrow().mutableMethod) {
                    val index = indexOfImmutableListBuilderInstruction(this)

                    getInstruction<ReferenceInstruction>(index).reference
                }

            AdPostSectionConstructorFingerprint.alsoResolve(
                context, AdPostSectionToStringFingerprint
            ).mutableMethod.apply {
                val sectionIndex =
                    indexOfFirstStringInstructionOrThrow("sections")
                val sectionRegister =
                    getInstruction<FiveRegisterInstruction>(sectionIndex + 1).registerC

                addInstructions(
                    sectionIndex, """
                        invoke-static {v$sectionRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$sectionRegister
                        if-nez v$sectionRegister, :ignore
                        new-instance v$sectionRegister, Ljava/util/ArrayList;
                        invoke-direct {v$sectionRegister}, Ljava/util/ArrayList;-><init>()V
                        invoke-static {v$sectionRegister}, $immutableListBuilderReference
                        move-result-object v$sectionRegister
                        :ignore
                        nop
                        """
                )
            }
        } else  {
            // The new feeds work by inserting posts into lists.
            // AdElementConverter is conveniently responsible for inserting all feed ads.
            // By removing the appending instruction no ad posts gets appended to the feed.
            val newAdPostResult = NewAdPostFingerprint.result
                ?: NewAdPostLegacyFingerprint.resultOrThrow()
            val newAdPostMethod = newAdPostResult.mutableMethod

            newAdPostMethod.apply {
                val startIndex =
                    0.coerceAtLeast(indexOfFirstStringInstructionOrThrow("android_feed_freeform_render_variant"))
                val targetIndex = indexOfAddArrayListInstruction(this, startIndex)
                val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                replaceInstruction(
                    targetIndex,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
                )
            }
        }

        updateSettingsStatus("enableGeneralAds")

    }
}
