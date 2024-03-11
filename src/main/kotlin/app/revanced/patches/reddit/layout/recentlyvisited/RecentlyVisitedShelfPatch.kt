package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.reddit.layout.recentlyvisited.fingerprints.CommunityDrawerPresenterFingerprint
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getReference
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch(
    name = "Hide recently visited shelf",
    description = "Adds an option to hide the recently visited shelf in the sidebar.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.reddit.frontpage",
            [
                "2023.12.0",
                "2024.04.0"
            ]
        )
    ]
)
@Suppress("unused")
object RecentlyVisitedShelfPatch : BytecodePatch(
    setOf(CommunityDrawerPresenterFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "Lapp/revanced/integrations/reddit/patches/RecentlyVisitedShelfPatch;" +
                "->hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"

    override fun execute(context: BytecodeContext) {

        CommunityDrawerPresenterFingerprint.result?.let {
            lateinit var recentlyVisitedReference: Reference

            it.mutableClass.methods.find { method -> method.name == "<init>" }
                ?.apply {
                    val recentlyVisitedFieldIndex = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.SGET_OBJECT
                                && instruction.getReference<FieldReference>()?.name == "RECENTLY_VISITED"
                    }
                    val recentlyVisitedObjectIndex = getTargetIndex(recentlyVisitedFieldIndex, Opcode.IPUT_OBJECT)
                    recentlyVisitedReference =
                        getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference
                } ?: throw PatchException("Constructor method not found!")

            it.mutableMethod.apply {
                val recentlyVisitedObjectIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.IGET_OBJECT
                            && (instruction as? ReferenceInstruction)?.reference == recentlyVisitedReference
                }
                arrayOf(
                    getTargetIndex(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC),
                    getTargetIndexReversed(recentlyVisitedObjectIndex, Opcode.INVOKE_STATIC)
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
        } ?: throw CommunityDrawerPresenterFingerprint.exception

        updateSettingsStatus("RecentlyVisitedShelf")

    }
}
