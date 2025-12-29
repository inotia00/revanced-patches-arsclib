package app.revanced.patches.reddit.misc.openlink

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.misc.openlink.ScreenNavigatorMethodResolverPatch.Companion.screenNavigatorMethod
import app.revanced.patches.reddit.misc.openlink.fingerprints.ArticleConstructorFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.ArticleConstructorFingerprint.indexOfNullCheckInstruction
import app.revanced.patches.reddit.misc.openlink.fingerprints.ArticleToStringFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.FbpActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_45_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.getInstruction
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Open links externally")
@Description("Adds an option to always open links in your browser instead of in the in-app-browser.")
@DependsOn([SettingsPatch::class, ScreenNavigatorMethodResolverPatch::class])
@RedditCompatibility
@Suppress("unused")
class OpenLinksExternallyPatch : BytecodePatch(
    listOf(
        ArticleToStringFingerprint,
        FbpActivityOnCreateFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/OpenLinksExternallyPatch;"
    }

    override fun execute(context: BytecodeContext) {

        screenNavigatorMethod.apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow("uri") + 2

            addInstructions(
                insertIndex, """
                    invoke-static {p1, p2}, $INTEGRATIONS_METHOD_DESCRIPTOR->openLinksExternally(Landroid/app/Activity;Landroid/net/Uri;)Z
                    move-result v0
                    if-eqz v0, :dismiss
                    return-void
                    """, listOf(ExternalLabel("dismiss", getInstruction(insertIndex)))
            )
        }

        if (is_2025_45_or_greater) {
            FbpActivityOnCreateFingerprint.resultOrThrow().mutableMethod.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $INTEGRATIONS_METHOD_DESCRIPTOR->" +
                        "setActivity(Landroid/app/Activity;)V"
            )

            ArticleConstructorFingerprint.alsoResolve(
                context, ArticleToStringFingerprint
            ).mutableMethod.apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("url")
                val nullCheckIndex = indexOfNullCheckInstruction(this, stringIndex)
                val stringRegister = getInstruction<FiveRegisterInstruction>(nullCheckIndex).registerC

                addInstruction(
                    nullCheckIndex + 1,
                    "invoke-static/range { v$stringRegister .. v$stringRegister }, $INTEGRATIONS_METHOD_DESCRIPTOR->" +
                            "openLinksExternally(Ljava/lang/String;)V"
                )
            }
        }

        updateSettingsStatus("enableOpenLinksExternally")

    }
}