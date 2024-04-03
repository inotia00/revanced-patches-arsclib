package app.revanced.patches.youtube.video.customspeed

import app.revanced.patches.shared.customspeed.BaseCustomPlaybackSpeedPatch
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH

object CustomPlaybackSpeedBytecodePatch : BaseCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    8.0f
)
