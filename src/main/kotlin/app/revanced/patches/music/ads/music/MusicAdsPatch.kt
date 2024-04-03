package app.revanced.patches.music.ads.music

import app.revanced.patches.music.utils.integrations.Constants.ADS_PATH
import app.revanced.patches.shared.ads.BaseAdsPatch

object MusicAdsPatch : BaseAdsPatch(
    "$ADS_PATH/MusicAdsPatch;",
    "hideMusicAds"
)
