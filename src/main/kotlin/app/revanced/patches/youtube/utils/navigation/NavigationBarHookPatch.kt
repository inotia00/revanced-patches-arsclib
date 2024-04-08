package app.revanced.patches.youtube.utils.navigation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.InitializeButtonsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.navigation.fingerprints.ActionBarSearchResultsFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.NavigationEnumFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarButtonsCreateDrawableViewFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarButtonsCreateResourceViewFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarConstructorFingerprint
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getReference
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    description = "Hooks the active navigation or search bar.",
    dependencies = [
        PlayerTypeHookPatch::class,
        SharedResourceIdPatch::class
    ],
)
@Suppress("unused")
object NavigationBarHookPatch : BytecodePatch(
    setOf(
        ActionBarSearchResultsFingerprint,
        NavigationEnumFingerprint,
        PivotBarButtonsCreateDrawableViewFingerprint,
        PivotBarButtonsCreateResourceViewFingerprint,
        PivotBarConstructorFingerprint
    ),
) {
    internal const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/shared/NavigationBar;"

    private const val INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR =
        "$INTEGRATIONS_PATH/shared/NavigationBar\$NavigationButton;"

    private lateinit var navigationTabCreatedCallback: MutableMethod

    override fun execute(context: BytecodeContext) {
        fun MutableMethod.addHook(hook: Hook, insertPredicate: Instruction.() -> Boolean) {
            val filtered = getInstructions().filter(insertPredicate)
            if (filtered.isEmpty()) throw PatchException("Could not find insert indexes")
            filtered.forEach {
                val insertIndex = it.location.index + 2
                val register = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->${hook.methodName}(${hook.parameters})V",
                )
            }
        }

        InitializeButtonsFingerprint.apply {
            resolve(context, PivotBarConstructorFingerprint.resultOrThrow().classDef)
        }.resultOrThrow().mutableMethod.apply {
            // Hook the current navigation bar enum value. Note, the 'You' tab does not have an enum value.
            val navigationEnumClassName = NavigationEnumFingerprint.resultOrThrow().mutableClass.type
            addHook(Hook.SET_LAST_APP_NAVIGATION_ENUM) {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.definingClass == navigationEnumClassName
            }

            // Hook the creation of navigation tab views.
            val drawableTabMethod = PivotBarButtonsCreateDrawableViewFingerprint.resultOrThrow().mutableMethod
            addHook(Hook.NAVIGATION_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    drawableTabMethod,
                )
            }

            val imageResourceTabMethod = PivotBarButtonsCreateResourceViewFingerprint.resultOrThrow().method
            addHook(Hook.NAVIGATION_IMAGE_RESOURCE_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    imageResourceTabMethod,
                )
            }
        }

        // Hook the search bar.

        // Two different layouts are used at the hooked code.
        // Insert before the first ViewGroup method call after inflating,
        // so this works regardless which layout is used.
        ActionBarSearchResultsFingerprint.resultOrThrow().mutableMethod.apply {
            val instructionIndex = getTargetIndexWithMethodReferenceName("setLayoutDirection")
            val viewRegister = getInstruction<FiveRegisterInstruction>(instructionIndex).registerC

            addInstruction(
                instructionIndex,
                "invoke-static { v$viewRegister }, " +
                        "$INTEGRATIONS_CLASS_DESCRIPTOR->searchBarResultsViewLoaded(Landroid/view/View;)V",
            )
        }

        navigationTabCreatedCallback = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)?.mutableClass?.methods?.first { method ->
            method.name == "navigationTabCreatedCallback"
        } ?: throw PatchException("Could not find navigationTabCreatedCallback method")
    }

    val hookNavigationButtonCreated: (String) -> Unit by lazy {
        navigationTabCreatedCallback
        { integrationsClassDescriptor ->
            navigationTabCreatedCallback.addInstruction(
                0,
                "invoke-static { p0, p1 }, " +
                        "$integrationsClassDescriptor->navigationTabCreated" +
                        "(${INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR}Landroid/view/View;)V",
            )
        }
    }

    private enum class Hook(val methodName: String, val parameters: String) {
        SET_LAST_APP_NAVIGATION_ENUM("setLastAppNavigationEnum", "Ljava/lang/Enum;"),
        NAVIGATION_TAB_LOADED("navigationTabLoaded", "Landroid/view/View;"),
        NAVIGATION_IMAGE_RESOURCE_TAB_LOADED("navigationImageResourceTabLoaded", "Landroid/view/View;"),
        SEARCH_BAR_RESULTS_VIEW_LOADED("searchBarResultsViewLoaded", "Landroid/view/View;"),
    }
}