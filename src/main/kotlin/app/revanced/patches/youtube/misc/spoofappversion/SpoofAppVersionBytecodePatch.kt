package app.revanced.patches.youtube.misc.spoofappversion

import app.revanced.patches.shared.spoofappversion.BaseSpoofAppVersionPatch
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH

object SpoofAppVersionBytecodePatch : BaseSpoofAppVersionPatch(
    "$MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
)