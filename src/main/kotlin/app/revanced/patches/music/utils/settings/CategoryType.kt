package app.revanced.patches.music.utils.settings

enum class CategoryType(val value: String, var added: Boolean) {
    ACCOUNT("account", false),
    ACTION_BAR("action_bar", false),
    ADS("ads", false),
    FLYOUT("flyout", false),
    GENERAL("general", false),
    MISC("misc", false),
    NAVIGATION("navigation", false),
    PLAYER("player", false),
    RETURN_YOUTUBE_DISLIKE("ryd", false),
    SPONSOR_BLOCK("sb", false),
    VIDEO("video", false)
}