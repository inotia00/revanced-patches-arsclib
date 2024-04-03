package app.revanced.patches.youtube.utils.integrations

import app.revanced.patcher.patch.Patch

@Suppress("MemberVisibilityCanBePrivate")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/youtube"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"

    const val ADS_PATH = "$PATCHES_PATH/ads"
    const val ALTERNATIVE_THUMBNAILS_PATH = "$PATCHES_PATH/alternativethumbnails"
    const val BOTTOM_PLAYER_PATH = "$PATCHES_PATH/bottomplayer"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"
    const val FLYOUT_PANEL_PATH = "$PATCHES_PATH/flyoutpanel"
    const val FULLSCREEN_PATH = "$PATCHES_PATH/fullscreen"
    const val GENERAL_PATH = "$PATCHES_PATH/general"
    const val MISC_PATH = "$PATCHES_PATH/misc"
    const val NAVIGATION_PATH = "$PATCHES_PATH/navigation"
    const val OVERLAY_BUTTONS_PATH = "$PATCHES_PATH/overlaybutton"
    const val PLAYER_PATH = "$PATCHES_PATH/player"
    const val SEEKBAR_PATH = "$PATCHES_PATH/seekbar"
    const val SHORTS_PATH = "$PATCHES_PATH/shorts"
    const val SWIPE_PATH = "$PATCHES_PATH/swipe"
    const val UTILS_PATH = "$PATCHES_PATH/utils"
    const val VIDEO_PATH = "$PATCHES_PATH/video"

    const val ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR = "$ALTERNATIVE_THUMBNAILS_PATH/AlternativeThumbnailsPatch;"
    const val BOTTOM_PLAYER_CLASS_DESCRIPTOR = "$BOTTOM_PLAYER_PATH/BottomPlayerPatch;"
    const val FLYOUT_PANEL_CLASS_DESCRIPTOR = "$FLYOUT_PANEL_PATH/FlyoutPanelPatch;"
    const val FULLSCREEN_CLASS_DESCRIPTOR = "$FULLSCREEN_PATH/FullscreenPatch;"
    const val GENERAL_CLASS_DESCRIPTOR = "$GENERAL_PATH/GeneralPatch;"
    const val NAVIGATION_CLASS_DESCRIPTOR = "$NAVIGATION_PATH/NavigationPatch;"
    const val PLAYER_CLASS_DESCRIPTOR = "$PLAYER_PATH/PlayerPatch;"
    const val SEEKBAR_CLASS_DESCRIPTOR = "$SEEKBAR_PATH/SeekBarPatch;"
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

    val LANGUAGE_LIST = arrayOf(
        "values",
        "values-af",
        "values-am",
        "values-ar",
        "values-as",
        "values-az",
        "values-b+sr+Latn",
        "values-be",
        "values-bg",
        "values-bn",
        "values-bs",
        "values-ca",
        "values-cs",
        "values-da",
        "values-de",
        "values-el",
        "values-en-rGB",
        "values-en-rIN",
        "values-es",
        "values-es-rUS",
        "values-et",
        "values-eu",
        "values-fa",
        "values-fi",
        "values-fr",
        "values-fr-rCA",
        "values-gl",
        "values-gu",
        "values-hi",
        "values-hr",
        "values-hu",
        "values-hy",
        "values-in",
        "values-is",
        "values-it",
        "values-iw",
        "values-ja",
        "values-ka",
        "values-kk",
        "values-km",
        "values-kn",
        "values-ko",
        "values-ky",
        "values-lo",
        "values-lt",
        "values-lv",
        "values-mk",
        "values-ml",
        "values-mn",
        "values-mr",
        "values-ms",
        "values-my",
        "values-nb",
        "values-ne",
        "values-nl",
        "values-or",
        "values-pa",
        "values-pl",
        "values-pt",
        "values-pt-rBR",
        "values-pt-rPT",
        "values-ro",
        "values-ru",
        "values-si",
        "values-sk",
        "values-sl",
        "values-sq",
        "values-sr",
        "values-sv",
        "values-sw",
        "values-ta",
        "values-te",
        "values-th",
        "values-tl",
        "values-tr",
        "values-uk",
        "values-ur",
        "values-uz",
        "values-vi",
        "values-zh-rCN",
        "values-zh-rHK",
        "values-zh-rTW",
        "values-zu"
    )
}