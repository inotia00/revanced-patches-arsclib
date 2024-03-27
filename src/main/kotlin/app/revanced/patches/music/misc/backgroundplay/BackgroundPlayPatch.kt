package app.revanced.patches.music.misc.backgroundplay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.misc.backgroundplay.fingerprints.BackgroundPlaybackFingerprint
import app.revanced.util.exception

@Patch(
    name = "Background play",
    description = "Enables playing music in the background.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object BackgroundPlayPatch : BytecodePatch(
    setOf(BackgroundPlaybackFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        BackgroundPlaybackFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: throw BackgroundPlaybackFingerprint.exception

    }
}