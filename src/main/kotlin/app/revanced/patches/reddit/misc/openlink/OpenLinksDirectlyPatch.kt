package app.revanced.patches.reddit.misc.openlink

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.misc.openlink.ScreenNavigatorMethodResolverPatch.Companion.screenNavigatorMethod
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch

@Patch
@Name("Open links directly")
@Description("Adds an option to skip over redirection URLs in external links.")
@DependsOn([SettingsPatch::class, ScreenNavigatorMethodResolverPatch::class])
@RedditCompatibility
@Suppress("unused")
class OpenLinksDirectlyPatch : BytecodePatch() {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/OpenLinksDirectlyPatch;" +
                    "->" +
                    "parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;"
    }

    override fun execute(context: BytecodeContext) {
        screenNavigatorMethod.addInstructions(
            0, """
                invoke-static {p2}, $INTEGRATIONS_METHOD_DESCRIPTOR
                move-result-object p2
                """
        )

        updateSettingsStatus("enableOpenLinksDirectly")

    }
}