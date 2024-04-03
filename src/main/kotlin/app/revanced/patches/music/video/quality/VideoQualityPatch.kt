package app.revanced.patches.music.video.quality

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.overridequality.OverrideQualityHookPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.quality.fingerprints.UserQualityChangeFingerprint
import app.revanced.patches.music.video.videoid.VideoIdPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c

@Suppress("unused")
object VideoQualityPatch : BaseBytecodePatch(
    name = "Remember video quality",
    description = "Adds an option to remember the last video quality selected.",
    dependencies = setOf(
        OverrideQualityHookPatch::class,
        SettingsPatch::class,
        VideoIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(UserQualityChangeFingerprint)
) {
    private const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/VideoQualityPatch;"

    override fun execute(context: BytecodeContext) {

        UserQualityChangeFingerprint.result?.let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val qualityChangedClass =
                    context.findClass(
                        (getInstruction<BuilderInstruction21c>(endIndex))
                            .reference.toString()
                    )!!
                        .mutableClass

                val onItemClickMethod =
                    qualityChangedClass.methods.find { method -> method.name == "onItemClick" }

                onItemClickMethod?.apply {
                    val listItemIndexParameter = 3

                    addInstruction(
                        0,
                        "invoke-static {p$listItemIndexParameter}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
                    )
                } ?: throw PatchException("Failed to find onItemClick method")
            }
        } ?: throw UserQualityChangeFingerprint.exception

        VideoIdPatch.hookVideoId("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        SettingsPatch.addMusicPreference(
            CategoryType.VIDEO,
            "revanced_enable_save_video_quality",
            "true"
        )

    }
}
