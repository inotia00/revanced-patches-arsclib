package app.revanced.patches.all.misc.installer

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.AbstractTransformInstructionsPatch
import app.revanced.util.IMethodCall
import app.revanced.util.Instruction35cInfo
import app.revanced.util.ResourceUtils.valueOrThrow
import app.revanced.util.filterMapInstruction35c
import app.revanced.util.getInstruction
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Change installer package name")
@Description("Spoof the installer package name to make it appear that the app was installed from the App Store.")
@Suppress("unused")
internal class ChangeInstallerPackageNamePatch : AbstractTransformInstructionsPatch<Instruction35cInfo>() {
    companion object : OptionsContainer() {
        private const val GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending"
        private lateinit var installerPackageName: String

        private var ChangeInstallerPackageName by option(
            PatchOption.BooleanOption(
                key = "ChangeInstallerPackageName",
                default = false,
                title = "Change installer package name",
                description = "Spoof the installer package name.",
                required = true,
            )
        )

        private var InstallerPackageName = option(
            PatchOption.StringOption(
                key = "InstallerPackageName",
                default = GOOGLE_PLAY_STORE_PACKAGE,
                title = "Installer package name",
                description = "The package name from which the app was installed, such as '$GOOGLE_PLAY_STORE_PACKAGE'",
                required = true,
            )
        )
    }

    // Information about method calls we want to replace
    enum class MethodCall(
        override val definedClassName: String,
        override val methodName: String,
        override val methodParams: Array<String>,
        override val returnType: String
    ): IMethodCall {
        GetInstallerPackageName(
            "Landroid/content/pm/PackageManager;",
            "getInstallerPackageName",
            arrayOf("Ljava/lang/String;"),
            "Ljava/lang/String;",
        ),
        GetInitiatingPackageName(
            "Landroid/content/pm/InstallSourceInfo;",
            "getInitiatingPackageName",
            arrayOf(),
            "Ljava/lang/String;",
        );
    }

    override fun filterMap(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        instructionIndex: Int
    ) = filterMapInstruction35c<MethodCall>(
        "Lapp/",
        classDef,
        instruction,
        instructionIndex
    )

    override fun transform(mutableMethod: MutableMethod, entry: Instruction35cInfo) {
        val (_, _, instructionIndex) = entry

        mutableMethod.apply {
            val targetRegister = (
                    getInstruction(instructionIndex + 1)
                            as? OneRegisterInstruction ?: return@transform
                    ).registerA

            replaceInstruction(
                instructionIndex + 1,
                "const-string v$targetRegister, \"$installerPackageName\"",
            )
            replaceInstruction(
                instructionIndex,
                "const-string v$targetRegister, \"$installerPackageName\"",
            )
        }
    }

    override fun execute(context: BytecodeContext) {
        if (ChangeInstallerPackageName == false) {
            println("INFO: Installer package name will remain unchanged as 'ChangeInstallerPackageName' is false.")
            return
        }
        installerPackageName = InstallerPackageName.valueOrThrow()

        super.execute(context)
    }
}
