package app.revanced.patches.youtube.ads.video

import app.revanced.patches.shared.ads.BaseAdsPatch
import app.revanced.patches.youtube.utils.integrations.Constants.ADS_PATH

object VideoAdsBytecodePatch : BaseAdsPatch(
    "$ADS_PATH/VideoAdsPatch;",
    "hideVideoAds"
)
