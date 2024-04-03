package app.revanced.patches.reddit.utils.integrations

import app.revanced.patcher.patch.Patch

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/reddit"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"

    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.reddit.frontpage",
            setOf(
                "2023.12.0",
                "2024.04.0"
            )
        )
    )
}