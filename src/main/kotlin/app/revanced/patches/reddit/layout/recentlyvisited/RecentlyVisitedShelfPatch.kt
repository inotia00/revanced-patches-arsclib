package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.recentlyvisited.fingerprints.CommunityDrawerPresenterFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexReversedOrThrow
import app.revanced.util.getTargetIndexWithFieldReferenceNameOrThrow
import app.revanced.util.getTargetIndexWithReferenceOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.Reference

@Patch
@Name("Hide Recently Visited shelf")
@Description("Adds an option to hide the Recently Visited shelf in the sidebar.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class RecentlyVisitedShelfPatch : BytecodePatch(
    listOf(CommunityDrawerPresenterFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/RecentlyVisitedShelfPatch;" +
                    "->" +
                    "hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"
    }

    override fun execute(context: BytecodeContext) {

        CommunityDrawerPresenterFingerprint.resultOrThrow().let {
            lateinit var recentlyVisitedReference: Reference

            it.mutableClass.methods.find { method -> method.name == "<init>" }
                ?.apply {
                    val recentlyVisitedFieldIndex =
                        getTargetIndexWithFieldReferenceNameOrThrow("RECENTLY_VISITED")
                    val recentlyVisitedObjectIndex =
                        getTargetIndexOrThrow(recentlyVisitedFieldIndex, Opcode.IPUT_OBJECT)
                    recentlyVisitedReference =
                        getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference
                } ?: throw PatchException("Constructor method not found!")

            it.mutableMethod.apply {
                val recentlyVisitedObjectIndex =
                    getTargetIndexWithReferenceOrThrow(recentlyVisitedReference.toString())
                arrayOf(
                    getTargetIndexOrThrow(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC),
                    getTargetIndexReversedOrThrow(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC)
                ).forEach { staticIndex ->
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(staticIndex + 1).registerA

                    addInstructions(
                        staticIndex + 2, """
                            invoke-static {v$insertRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR
                            move-result-object v$insertRegister
                            """
                    )
                }
            }
        }

        updateSettingsStatus("enableRecentlyVisitedShelf")

    }
}
