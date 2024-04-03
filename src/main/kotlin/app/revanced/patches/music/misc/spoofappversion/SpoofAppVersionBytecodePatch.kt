package app.revanced.patches.music.misc.spoofappversion

import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.shared.spoofappversion.BaseSpoofAppVersionPatch

object SpoofAppVersionBytecodePatch : BaseSpoofAppVersionPatch(
    "$MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
)