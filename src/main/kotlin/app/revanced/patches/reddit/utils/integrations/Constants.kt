package app.revanced.patches.reddit.utils.integrations

import app.revanced.patcher.patch.Patch

@Suppress("MemberVisibilityCanBePrivate")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/reddit"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"

    val COMPATIBLE_PACKAGE = setOf(Patch.CompatiblePackage("com.reddit.frontpage"))
}