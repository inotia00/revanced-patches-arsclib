package app.revanced.patches.youtube.utils.integrations

import app.revanced.patcher.patch.Patch

@Suppress("MemberVisibilityCanBePrivate")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/youtube"
    const val SHARED_PATH = "$INTEGRATIONS_PATH/shared"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"

    const val ADS_PATH = "$PATCHES_PATH/ads"
    const val ALTERNATIVE_THUMBNAILS_PATH = "$PATCHES_PATH/alternativethumbnails"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"
    const val FEED_PATH = "$PATCHES_PATH/feed"
    const val GENERAL_PATH = "$PATCHES_PATH/general"
    const val MISC_PATH = "$PATCHES_PATH/misc"
    const val OVERLAY_BUTTONS_PATH = "$PATCHES_PATH/overlaybutton"
    const val PLAYER_PATH = "$PATCHES_PATH/player"
    const val SHORTS_PATH = "$PATCHES_PATH/shorts"
    const val SWIPE_PATH = "$PATCHES_PATH/swipe"
    const val UTILS_PATH = "$PATCHES_PATH/utils"
    const val VIDEO_PATH = "$PATCHES_PATH/video"

    const val ADS_CLASS_DESCRIPTOR = "$ADS_PATH/AdsPatch;"
    const val ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR = "$ALTERNATIVE_THUMBNAILS_PATH/AlternativeThumbnailsPatch;"
    const val FEED_CLASS_DESCRIPTOR = "$FEED_PATH/FeedPatch;"
    const val GENERAL_CLASS_DESCRIPTOR = "$GENERAL_PATH/GeneralPatch;"
    const val PLAYER_CLASS_DESCRIPTOR = "$PLAYER_PATH/PlayerPatch;"
    const val SHORTS_CLASS_DESCRIPTOR = "$SHORTS_PATH/ShortsPatch;"

    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.google.android.youtube",
            setOf(
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
            )
        )
    )
}