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
import app.revanced.patches.reddit.ad.banner.BannerAdsPatch
import app.revanced.patches.reddit.ad.comments.CommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostSectionConstructorFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getInstruction
import app.revanced.util.getTargetIndexWithFieldReferenceNameOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide ads")
@Description("Adds options to hide ads.")
@DependsOn([BannerAdsPatch::class, CommentAdsPatch::class, SettingsPatch::class])
@RedditCompatibility
@RequiresIntegrations
@Suppress("unused")
class AdsPatch : BytecodePatch(
    listOf(
        AdPostFingerprint,
        AdPostSectionConstructorFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/GeneralAdsPatch;"
    }

    override fun execute(context: BytecodeContext) {
        // region Filter promoted ads (does not work in popular or latest feed)
        AdPostFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithFieldReferenceNameOrThrow("children")
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideOldPostAds(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        var adPostSectionConstructorMethodCall: String
        AdPostSectionConstructorFingerprint.resultOrThrow().mutableMethod.apply {
            adPostSectionConstructorMethodCall = "$definingClass->$name("
            for (i in 0 until parameters.size) {
                adPostSectionConstructorMethodCall += parameterTypes[i]
            }
            adPostSectionConstructorMethodCall += ")$returnType"
        }

        context.classes.forEach { classDef ->
            if (classDef.methods.count() > 5)
                return@forEach
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { _, instruction ->
                        if (instruction.opcode != Opcode.INVOKE_DIRECT_RANGE)
                            return@forEachIndexed
                        if ((instruction as? ReferenceInstruction)?.reference.toString() != adPostSectionConstructorMethodCall)
                            return@forEachIndexed

                        context.classes.proxy(classDef)
                            .mutableClass
                            .findMutableMethodOf(method)
                            .apply {
                                val targetIndex = getTargetIndexWithMethodReferenceNameOrThrow("add")
                                val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                                replaceInstruction(
                                    targetIndex,
                                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                                            "$INTEGRATIONS_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
                                )
                            }
                    }
                }
            }
        }

        updateSettingsStatus("enableGeneralAds")

    }
}
