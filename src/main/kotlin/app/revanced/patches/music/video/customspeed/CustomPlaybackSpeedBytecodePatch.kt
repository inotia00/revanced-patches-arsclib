package app.revanced.patches.music.video.customspeed

import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.shared.customspeed.BaseCustomPlaybackSpeedPatch

object CustomPlaybackSpeedBytecodePatch : BaseCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    3.0f
)
