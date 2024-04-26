package app.revanced.patches.youtube.general.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.voicesearch.VoiceSearchUtils.patchXml
import app.revanced.patches.youtube.general.toolbar.fingerprints.CreateSearchSuggestionsFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchBarFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchBarParentFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchResultFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SetActionBarRingoFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SetWordMarkHeaderFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.TrendingSearchConfigFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.YouActionBarFingerprint
import app.revanced.patches.youtube.utils.castbutton.CastButtonPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VoiceSearch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getTargetIndexWithReferenceReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("DEPRECATION", "unused")
object ToolBarComponentsPatch : BaseBytecodePatch(
    name = "Toolbar components",
    description = "Adds options to hide or change components located on the toolbar such as toolbar buttons, search bar, and header.",
    dependencies = setOf(
        CastButtonPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ToolBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CreateSearchSuggestionsFingerprint,
        SearchBarParentFingerprint,
        SearchResultFingerprint,
        SetActionBarRingoFingerprint,
        SetWordMarkHeaderFingerprint,
        TrendingSearchConfigFingerprint
    )
) {
    private const val FLAG = "android:paddingStart"
    private const val TARGET_RESOURCE_PATH = "res/layout/action_bar_ringo_background.xml"

    private val ForceHideVoiceSearchButton by booleanPatchOption(
        key = "ForceHideVoiceSearchButton",
        default = false,
        title = "Force hide voice search button",
        description = "Hide voice search button with legacy method, button will always be hidden"
    )

    override fun execute(context: BytecodeContext) {

        // region patch for enable wide search bar

        val parentClassDef = SetActionBarRingoFingerprint.resultOrThrow().classDef
        YouActionBarFingerprint.resolve(context, parentClassDef)

        SetWordMarkHeaderFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)

            walkerMethod.apply {
                injectSearchBarHook(
                    implementation!!.instructions.size - 1,
                    "enableWideSearchBar"
                )
            }
        }

        YouActionBarFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                injectSearchBarHook(
                    it.scanResult.patternScanResult!!.endIndex,
                    "enableWideSearchBarInYouTab"
                )
            }
        }

        contexts.xmlEditor[TARGET_RESOURCE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("RelativeLayout").item(0)) {
                if (attributes.getNamedItem(FLAG) != null) return@with

                document.createAttribute(FLAG)
                    .apply { value = "8.0dip" }
                    .let(attributes::setNamedItem)
            }
        }

        // endregion

        // region patch for hide cast button

        CastButtonPatch.hookToolBarButton(context)

        // endregion

        // region patch for hide create button

        ToolBarHookPatch.hook("$GENERAL_CLASS_DESCRIPTOR->hideCreateButton")

        // endregion

        // region patch for hide notification button

        ToolBarHookPatch.hook("$GENERAL_CLASS_DESCRIPTOR->hideNotificationButton")

        // endregion

        // region patch for hide search term thumbnail

        CreateSearchSuggestionsFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val relativeIndex = getWideLiteralInstructionIndex(40)
                val replaceIndex = getTargetIndexWithReferenceReversed(
                    relativeIndex,
                    "Landroid/widget/ImageView;->setVisibility(I)V"
                ) - 1

                val jumpIndex = getTargetIndexWithReference(
                    relativeIndex,
                    "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                ) + 4

                val replaceIndexInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                val replaceIndexReference =
                    getInstruction<ReferenceInstruction>(replaceIndex).reference

                addInstructionsWithLabels(
                    replaceIndex + 1, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                        move-result v${replaceIndexInstruction.registerA}
                        if-nez v${replaceIndexInstruction.registerA}, :hidden
                        iget-object v${replaceIndexInstruction.registerA}, v${replaceIndexInstruction.registerB}, $replaceIndexReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(replaceIndex)
            }
        }

        // endregion

        // region patch for hide trending searches

        TrendingSearchConfigFingerprint.literalInstructionBooleanHook(
            45399984,
            "$GENERAL_CLASS_DESCRIPTOR->hideTrendingSearches(Z)Z"
        )

        // endregion

        // region patch for hide voice search button

        if (ForceHideVoiceSearchButton == true) {
            contexts.patchXml(
                arrayOf(
                    "action_bar_search_results_view_mic.xml",
                    "action_bar_search_view.xml",
                    "action_bar_search_view_grey.xml",
                    "action_bar_search_view_mic_out.xml"
                ),
                arrayOf(
                    "height",
                    "marginEnd",
                    "marginStart",
                    "width"
                )
            )
        } else {
            SearchBarFingerprint.resolve(context, SearchBarParentFingerprint.resultOrThrow().classDef)

            SearchBarFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val setVisibilityIndex = getTargetIndexWithMethodReferenceName(startIndex, "setVisibility")
                    val setVisibilityInstruction = getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                    replaceInstruction(
                        setVisibilityIndex,
                        "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                                "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;I)V"
                    )
                }
            }

            SearchResultFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val startIndex = getWideLiteralInstructionIndex(VoiceSearch)
                    val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName(startIndex, "setOnClickListener")
                    val viewRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                    addInstruction(
                        setOnClickListenerIndex + 1,
                        "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;)V"
                    )
                }
            }

            /**
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: HIDE_VOICE_SEARCH_BUTTON"
                )
            )
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: TOOLBAR_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MutableMethod.injectSearchBarHook(
        insertIndex: Int,
        descriptor: String
    ) {
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstructions(
            insertIndex, """
                invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->$descriptor(Z)Z
                move-result v$insertRegister
                """
        )
    }
}
