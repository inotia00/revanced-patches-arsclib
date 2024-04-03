package app.revanced.patches.music.misc.minimizedplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object MinimizedPlaybackPatch : BaseBytecodePatch(
    name = "Enable minimized playback",
    description = "Enables playback in miniplayer for Kids music.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(MinimizedPlaybackManagerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MinimizedPlaybackManagerFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0, "return-void"
        )

    }
}
