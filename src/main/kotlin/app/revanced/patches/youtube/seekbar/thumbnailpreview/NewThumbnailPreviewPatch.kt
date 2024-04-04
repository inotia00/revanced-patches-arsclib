package app.revanced.patches.youtube.seekbar.thumbnailpreview

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.seekbar.thumbnailpreview.fingerprints.ThumbnailPreviewConfigFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SEEKBAR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object NewThumbnailPreviewPatch : BaseBytecodePatch(
    name = "Enable new thumbnail preview",
    description = "Adds an option to enables the new seekbar thumbnails preview.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(ThumbnailPreviewConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ThumbnailPreviewConfigFingerprint.literalInstructionBooleanHook(
            45398577,
            "$SEEKBAR_CLASS_DESCRIPTOR->enableNewThumbnailPreview()Z"
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_NEW_THUMBNAIL_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("Enable new thumbnail preview")

    }
}
