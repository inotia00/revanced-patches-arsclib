package app.revanced.patches.reddit.layout.sidebar

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.reddit.layout.sidebar.SidebarUtils.indexOfHeaderItemInstruction
import app.revanced.patches.reddit.layout.sidebar.fingerprints.CommunityDrawerPresenterConstructorFingerprint
import app.revanced.patches.reddit.layout.sidebar.fingerprints.CommunityDrawerPresenterFingerprint
import app.revanced.patches.reddit.layout.sidebar.fingerprints.CommunityDrawerPresenterFingerprint.indexOfKotlinCollectionInstruction
import app.revanced.patches.reddit.layout.sidebar.fingerprints.HeaderItemUiModelToStringFingerprint
import app.revanced.patches.reddit.layout.sidebar.fingerprints.RedditProLoaderFingerprint
import app.revanced.patches.reddit.layout.sidebar.fingerprints.SidebarComponentsPatchFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_45_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.findFieldFromToString
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.parametersStartsWith
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.ImmutableMethod

@Patch
@Name("Hide sidebar components")
@Description("Adds options to hide the sidebar components.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class SidebarComponentsPatch : BytecodePatch(
    listOf(
        CommunityDrawerPresenterConstructorFingerprint,
        HeaderItemUiModelToStringFingerprint,
        RedditProLoaderFingerprint,
        SidebarComponentsPatchFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/SidebarComponentsPatch;"
    }

    override fun execute(context: BytecodeContext) {

        if (is_2025_45_or_greater) {
            val helperMethodName = "getHeaderItemName"

            HeaderItemUiModelToStringFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val headerItemField = findFieldFromToString(", type=")

                    it.mutableClass.methods.add(
                        ImmutableMethod(
                            definingClass,
                            helperMethodName,
                            listOf(),
                            "Ljava/lang/String;",
                            AccessFlags.PUBLIC or AccessFlags.FINAL,
                            null, null,
                            MutableMethodImplementation(2)
                        ).toMutable().apply {
                            addInstructions(
                                0, """
                                    iget-object v0, p0, $headerItemField
                                    if-nez v0, :name
                                    const-string v0, ""
                                    return-object v0
                                    :name
                                    invoke-virtual {v0}, Ljava/lang/Enum;->name()Ljava/lang/String;
                                    move-result-object v0
                                    return-object v0
                                    """
                            )
                        }
                    )
                }
            }

            val headerItemUiModelClass =
                HeaderItemUiModelToStringFingerprint.result!!.mutableMethod.definingClass

            SidebarComponentsPatchFingerprint.resultOrThrow().mutableMethod.apply {
                addInstructions(
                    0, """
                        check-cast p0, $headerItemUiModelClass
                        invoke-virtual {p0}, $headerItemUiModelClass->$helperMethodName()Ljava/lang/String;
                        move-result-object v0
                        return-object v0
                        """
                )
            }

            val isShelfBuilderMethod: Method.() -> Boolean = {
                parameterTypes.size > 4 &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.STATIC &&
                        returnType == "V" &&
                        parametersStartsWith(
                            parameterTypes,
                            listOf(
                                "L",
                                "Ljava/util/List;",
                                "Ljava/util/Collection;",
                                headerItemUiModelClass
                            )
                        )
            }

            context.classes.forEach handler@{ classDef ->
                classDef.methods.forEach { method ->
                    if (method.isShelfBuilderMethod()) {
                        context.classes.proxy(classDef)
                            .mutableClass
                            .findMutableMethodOf(method)
                            .addInstructions(
                                0, """
                                    invoke-static/range { p2 .. p3 }, $INTEGRATIONS_CLASS_DESCRIPTOR->hideComponents(Ljava/util/Collection;Ljava/lang/Object;)Ljava/util/Collection;
                                    move-result-object p2
                                    """
                            )

                        return@handler
                    }
                }
            }
        } else {
            val communityDrawerPresenterConstructorMethod =
                CommunityDrawerPresenterConstructorFingerprint.resultOrThrow().mutableMethod

            val communityDrawerPresenterMethod =
                CommunityDrawerPresenterFingerprint.alsoResolve(
                    context, CommunityDrawerPresenterConstructorFingerprint
                ).mutableMethod

            fun getDrawerField(
                fieldName: String,
                isRedditPro: Boolean
            ): FieldReference {
                val targetMethod = if (isRedditPro) {
                    RedditProLoaderFingerprint.resultOrThrow().mutableMethod
                } else {
                    communityDrawerPresenterConstructorMethod
                }

                targetMethod.apply {
                    val headerItemIndex =
                        indexOfHeaderItemInstruction(this, fieldName)
                    val fieldIndex =
                        indexOfFirstInstructionOrThrow(headerItemIndex, Opcode.IPUT_OBJECT)

                    return getInstruction<ReferenceInstruction>(fieldIndex).reference
                            as FieldReference
                }
            }

            fun hideShelf(
                fieldName: String,
                methodNamePrefix: String,
                isRedditPro: Boolean
            ) {
                val fieldReference = getDrawerField(fieldName, isRedditPro)

                communityDrawerPresenterMethod.apply {
                    val fieldIndex =
                        indexOfFirstInstructionOrThrow {
                            opcode == Opcode.IGET_OBJECT &&
                                    getReference<FieldReference>() == fieldReference
                        }

                    val collectionIndex =
                        indexOfKotlinCollectionInstruction(this, fieldIndex)
                    val collectionInstruction =
                        getInstruction<FiveRegisterInstruction>(collectionIndex)

                    val iterableRegister = collectionInstruction.registerC
                    val collectionRegister = collectionInstruction.registerD

                    addInstructions(
                        collectionIndex, """
                            invoke-static {v$iterableRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->${methodNamePrefix}Divider(Ljava/lang/Iterable;)Ljava/lang/Iterable;
                            move-result-object v$iterableRegister
                            invoke-static {v$collectionRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->${methodNamePrefix}Shelf(Ljava/util/Collection;)Ljava/util/Collection;
                            move-result-object v$collectionRegister
                            """
                    )
                }
            }

            val hooks = mutableListOf(
                Triple(
                    "RECENTLY_VISITED",
                    "hideRecentlyVisited",
                    false
                )
            )

            if (is_2025_40_or_greater) {
                hooks += Triple(
                    "GAMES_ON_REDDIT",
                    "hideGamesOnReddit",
                    false
                )
                hooks += Triple(
                    "REDDIT_PRO",
                    "hideRedditPro",
                    true
                )
            }

            hooks.forEach { (fieldName, methodNamePrefix, isRedditPro) ->
                hideShelf(fieldName, methodNamePrefix, isRedditPro)
            }
        }

        updateSettingsStatus("enableRecentlyVisitedShelf")

        if (is_2025_40_or_greater) {
            updateSettingsStatus("enableGamesOnRedditShelf")
            updateSettingsStatus("enableRedditProShelf")
        }
    }
}
