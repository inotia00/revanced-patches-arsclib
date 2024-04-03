package app.revanced.patches.youtube.general.channelprofile

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.general.channelprofile.fingerprints.DefaultsTabsBarFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TabsBarTextTabView
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object ChannelProfileComponentsPatch : BaseBytecodePatch(
    name = "Hide channel profile components",
    description = "Adds an option to hide channel profile components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(DefaultsTabsBarFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ChannelProfileFilter;"

    override fun execute(context: BytecodeContext) {

        DefaultsTabsBarFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = getWideLiteralInstructionIndex(TabsBarTextTabView) + 2
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstruction(
                    viewIndex + 1,
                    "sput-object v$viewRegister, $FILTER_CLASS_DESCRIPTOR->channelTabView:Landroid/view/View;"
                )
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_CHANNEL_PROFILE_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide channel profile components")

    }
}
