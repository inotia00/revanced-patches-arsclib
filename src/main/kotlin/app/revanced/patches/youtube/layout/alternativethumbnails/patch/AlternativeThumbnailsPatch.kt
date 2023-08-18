package app.revanced.patches.youtube.layout.alternativethumbnails.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.layout.alternativethumbnails.fingerprints.CronetURLRequestCallbackOnFailureFingerprint
import app.revanced.patches.youtube.layout.alternativethumbnails.fingerprints.CronetURLRequestCallbackOnResponseStartedFingerprint
import app.revanced.patches.youtube.layout.alternativethumbnails.fingerprints.CronetURLRequestCallbackOnSucceededFingerprint
import app.revanced.patches.youtube.layout.alternativethumbnails.fingerprints.MessageDigestImageUrlFingerprint
import app.revanced.patches.youtube.layout.alternativethumbnails.fingerprints.MessageDigestImageUrlParentFingerprint
import app.revanced.patches.youtube.utils.integrations.patch.IntegrationsPatch
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts

@Patch
@Name("Alternative thumbnails")
@Description("Adds an option to replace video thumbnails with still image captures of the video.")
@YouTubeCompatibility

@DependsOn(
    [
        IntegrationsPatch::class,
        SettingsPatch::class
    ]
)
class AlternativeThumbnailsPatch : BytecodePatch(
    listOf(
        MessageDigestImageUrlParentFingerprint,
        CronetURLRequestCallbackOnResponseStartedFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        MessageDigestImageUrlParentFingerprint.result
            ?: throw MessageDigestImageUrlParentFingerprint.exception

        MessageDigestImageUrlFingerprint.resolve(context, MessageDigestImageUrlParentFingerprint.result!!.classDef)

        MessageDigestImageUrlFingerprint.result?.apply {
            loadImageUrlMethod = mutableMethod
        } ?: throw MessageDigestImageUrlFingerprint.exception
        addImageUrlHook(INTEGRATIONS_CLASS_DESCRIPTOR, true)

        CronetURLRequestCallbackOnResponseStartedFingerprint.result
            ?: throw CronetURLRequestCallbackOnResponseStartedFingerprint.exception

        CronetURLRequestCallbackOnSucceededFingerprint.resolve(
            context,
            CronetURLRequestCallbackOnResponseStartedFingerprint.result!!.classDef
        )

        CronetURLRequestCallbackOnSucceededFingerprint.result?.apply {
            loadImageSuccessCallbackMethod = mutableMethod
        } ?: throw CronetURLRequestCallbackOnSucceededFingerprint.exception
        addImageUrlSuccessCallbackHook(INTEGRATIONS_CLASS_DESCRIPTOR)


        CronetURLRequestCallbackOnFailureFingerprint.resolve(
            context,
            CronetURLRequestCallbackOnResponseStartedFingerprint.result!!.classDef
        )

        CronetURLRequestCallbackOnFailureFingerprint.result?.apply {
            loadImageErrorCallbackMethod = mutableMethod
        } ?: throw CronetURLRequestCallbackOnFailureFingerprint.exception

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/alternativethumbnails/host", "values/arrays.xml", "resources")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ALTERNATIVE_VIDEO_THUMBNAILS"
            )
        )

        SettingsPatch.updatePatchStatus("alternative-video-thumbnails")
    }

    internal companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/alternativethumbnails/AlternativeThumbnailsPatch;"

        private lateinit var loadImageUrlMethod: MutableMethod
        private var loadImageUrlIndex = 0

        private lateinit var loadImageSuccessCallbackMethod: MutableMethod
        private var loadImageSuccessCallbackIndex = 0

        private lateinit var loadImageErrorCallbackMethod: MutableMethod
        private var loadImageErrorCallbackIndex = 0

        /**
         * @param highPriority If the hook should be called before all other hooks.
         */
        fun addImageUrlHook(targetMethodClass: String, highPriority: Boolean) {
            loadImageUrlMethod.addInstructions(
                if (highPriority) 0 else loadImageUrlIndex, """
                    invoke-static { p1 }, $targetMethodClass->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                """
            )
            loadImageUrlIndex += 2
        }

        /**
         * If a connection completed, which includes normal 200 responses but also includes
         * status 404 and other error like http responses.
         */
        fun addImageUrlSuccessCallbackHook(targetMethodClass: String) {
            loadImageSuccessCallbackMethod.addInstruction(
                loadImageSuccessCallbackIndex++,
                "invoke-static { p2 }, $targetMethodClass->handleCronetSuccess(Lorg/chromium/net/UrlResponseInfo;)V"
            )
        }

        /**
         * If a connection outright failed to complete any connection.
         */
        fun addImageUrlErrorCallbackHook(targetMethodClass: String) {
            loadImageErrorCallbackMethod.addInstruction(
                loadImageErrorCallbackIndex++,
                "invoke-static { p2, p3 }, $targetMethodClass->handleCronetFailure(Lorg/chromium/net/UrlResponseInfo;Ljava/io/IOException;)V"
            )
        }
    }
}
