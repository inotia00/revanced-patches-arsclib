package app.revanced.patches.youtube.general.widesearchbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.general.widesearchbar.fingerprints.SetActionBarRingoFingerprint
import app.revanced.patches.youtube.general.widesearchbar.fingerprints.SetWordMarkHeaderFingerprint
import app.revanced.patches.youtube.general.widesearchbar.fingerprints.YouActionBarFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("DEPRECATION", "unused")
object WideSearchBarPatch : BaseBytecodePatch(
    name = "Enable wide search bar",
    description = "Adds an option to replace the search icon with a wide search bar. This will hide the YouTube logo when active.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        SetActionBarRingoFingerprint,
        SetWordMarkHeaderFingerprint
    )
) {
    private const val FLAG = "android:paddingStart"
    private const val TARGET_RESOURCE_PATH = "res/layout/action_bar_ringo_background.xml"

    override fun execute(context: BytecodeContext) {

        // resolves fingerprints
        val parentClassDef = SetActionBarRingoFingerprint.result?.classDef
            ?: throw SetActionBarRingoFingerprint.exception
        YouActionBarFingerprint.resolve(context, parentClassDef)

        // patch methods
        SetWordMarkHeaderFingerprint.result?.let {
            val targetMethod =
                context.toMethodWalker(it.method)
                    .nextMethod(1, true)
                    .getMethod() as MutableMethod

            targetMethod.apply {
                injectSearchBarHook(
                    implementation!!.instructions.size - 1,
                    "enableWideSearchBar"
                )
            }
        } ?: throw SetWordMarkHeaderFingerprint.exception

        YouActionBarFingerprint.result?.let {
            it.mutableMethod.apply {
                injectSearchBarHook(
                    it.scanResult.patternScanResult!!.endIndex,
                    "enableWideSearchBarInYouTab"
                )
            }
        } ?: throw YouActionBarFingerprint.exception

        /**
         * Set Wide SearchBar Start Margin
         */
        contexts.xmlEditor[TARGET_RESOURCE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("RelativeLayout").item(0)) {
                if (attributes.getNamedItem(FLAG) != null) return@with

                document.createAttribute(FLAG)
                    .apply { value = "8.0dip" }
                    .let(attributes::setNamedItem)
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ENABLE_WIDE_SEARCH_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("Enable wide search bar")

    }

    /**
     * Injects instructions required for certain methods.
     */
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
