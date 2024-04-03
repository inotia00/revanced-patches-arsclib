package app.revanced.patches.music.general.floatingbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonFingerprint
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonParentFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object FloatingButtonPatch : BaseBytecodePatch(
    name = "Hide new playlist button",
    description = "Adds an option to hide the \"New playlist\" button in the library.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(FloatingButtonParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        FloatingButtonParentFingerprint.resultOrThrow().let { parentResult ->
            FloatingButtonFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.resultOrThrow().let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        1, """
                            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideNewPlaylistButton()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(1))
                    )
                }
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_new_playlist_button",
            "false"
        )

    }
}
