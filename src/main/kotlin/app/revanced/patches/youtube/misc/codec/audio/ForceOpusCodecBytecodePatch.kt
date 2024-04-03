package app.revanced.patches.youtube.misc.codec.audio

import app.revanced.patches.shared.opus.BaseOpusCodecsPatch
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH

object ForceOpusCodecBytecodePatch : BaseOpusCodecsPatch(
    "$MISC_PATH/CodecOverridePatch;->shouldForceOpus()Z"
)
