@file:Suppress("unused")

package app.revanced.util

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableField
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * The [PatchException] of failing to resolve a [MethodFingerprint].
 *
 * @return The [PatchException].
 */
val MethodFingerprint.exception
    get() = PatchException("Failed to resolve ${this.javaClass.simpleName}")

/**
 * Find the [MutableMethod] from a given [Method] in a [MutableClass].
 *
 * @param method The [Method] to find.
 * @return The [MutableMethod].
 */
fun MutableClass.findMutableMethodOf(method: Method) = this.methods.first {
    MethodUtil.methodSignaturesMatch(it, method)
}

/**
 * Apply a transform to all fields of the class.
 *
 * @param transform The transformation function. Accepts a [MutableField] and returns a transformed [MutableField].
 */
fun MutableClass.transformFields(transform: MutableField.() -> MutableField) {
    val transformedFields = fields.map { it.transform() }
    fields.clear()
    fields.addAll(transformedFields)
}

/**
 * Apply a transform to all methods of the class.
 *
 * @param transform The transformation function. Accepts a [MutableMethod] and returns a transformed [MutableMethod].
 */
fun MutableClass.transformMethods(transform: MutableMethod.() -> MutableMethod) {
    val transformedMethods = methods.map { it.transform() }
    methods.clear()
    methods.addAll(transformedMethods)
}

/**
 * Inject a call to a method that hides a view.
 *
 * @param insertIndex The index to insert the call at.
 * @param viewRegister The register of the view to hide.
 * @param classDescriptor The descriptor of the class that contains the method.
 * @param targetMethod The name of the method to call.
 */
fun MutableMethod.injectHideViewCall(
    insertIndex: Int,
    viewRegister: Int,
    classDescriptor: String,
    targetMethod: String
) = addInstruction(
    insertIndex,
    "invoke-static { v$viewRegister }, $classDescriptor->$targetMethod(Landroid/view/View;)V"
)

/**
 * Find the index of the first wide literal instruction with the given value.
 *
 * @return the first literal instruction with the value, or -1 if not found.
 */
fun Method.getWideLiteralInstructionIndex(literal: Long) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1

fun MutableMethod.getEmptyStringInstructionIndex()
= getStringInstructionIndex("")

fun MutableMethod.getStringInstructionIndex(value: String) = indexOfFirstInstruction {
    opcode == Opcode.CONST_STRING
            && (this as? BuilderInstruction21c)?.reference.toString() == value
}

/**
 * Check if the method contains a literal with the given value.
 *
 * @return if the method contains a literal with the given value.
 */
fun Method.containsWideLiteralInstructionIndex(literal: Long) =
    getWideLiteralInstructionIndex(literal) >= 0

fun Method.containsMethodReferenceNameInstructionIndex(methodName: String) =
    getTargetIndexWithMethodReferenceName(methodName) >= 0

fun Method.containsReferenceInstructionIndex(reference: String) =
    getTargetIndexWithReference(reference) >= 0


/**
 * Traverse the class hierarchy starting from the given root class.
 *
 * @param targetClass the class to start traversing the class hierarchy from.
 * @param callback function that is called for every class in the hierarchy.
 */
fun BytecodeContext.traverseClassHierarchy(
    targetClass: MutableClass,
    callback: MutableClass.() -> Unit
) {
    callback(targetClass)
    this.findClass(targetClass.superclass ?: return)?.mutableClass?.let {
        traverseClassHierarchy(it, callback)
    }
}

/**
 * Get the [Reference] of an [Instruction] as [T].
 *
 * @param T The type of [Reference] to cast to.
 * @return The [Reference] as [T] or null
 * if the [Instruction] is not a [ReferenceInstruction] or the [Reference] is not of type [T].
 * @see ReferenceInstruction
 */
inline fun <reified T : Reference> Instruction.getReference() =
    (this as? ReferenceInstruction)?.reference as? T

/**
 * Get the index of the first [Instruction] that matches the predicate.
 *
 * @param predicate The predicate to match.
 * @return The index of the first [Instruction] that matches the predicate.
 */
fun Method.indexOfFirstInstruction(predicate: Instruction.() -> Boolean) =
    this.implementation!!.instructions.indexOfFirst(predicate)

fun MutableMethod.getTargetIndex(opcode: Opcode) = getTargetIndex(0, opcode)

fun MutableMethod.getTargetIndexReversed(opcode: Opcode) =
    getTargetIndex(implementation!!.instructions.size - 1, opcode)

fun MutableMethod.getTargetIndex(startIndex: Int, opcode: Opcode) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.opcode == opcode
        }
    }

fun MutableMethod.getTargetIndexReversed(startIndex: Int, opcode: Opcode): Int {
    for (index in startIndex downTo 0) {
        if (getInstruction(index).opcode != opcode)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithFieldReferenceName(filedName: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<FieldReference>()?.name == filedName
    }
} ?: -1

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversed(returnType: String)
        = getTargetIndexWithFieldReferenceTypeReversed(implementation!!.instructions.size - 1, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceName(startIndex: Int, filedName: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<FieldReference>()?.name == filedName
        }
    }

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversed(startIndex: Int, filedName: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<FieldReference>()?.name != filedName)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithFieldReferenceType(returnType: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<FieldReference>()?.type == returnType
    }
} ?: -1

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversed(returnType: String)
= getTargetIndexWithFieldReferenceTypeReversed(implementation!!.instructions.size - 1, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceType(startIndex: Int, returnType: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<FieldReference>()?.type == returnType
        }
    }

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversed(startIndex: Int, returnType: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<FieldReference>()?.type != returnType)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithMethodReferenceName(methodName: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.name == methodName
    }
} ?: -1

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversed(methodName: String)
= getTargetIndexWithMethodReferenceNameReversed(implementation!!.instructions.size - 1, methodName)


fun MutableMethod.getTargetIndexWithMethodReferenceName(startIndex: Int, methodName: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<MethodReference>()?.name == methodName
        }
    }

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversed(startIndex: Int, methodName: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<MethodReference>()?.name != methodName)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithReference(reference: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? ReferenceInstruction)?.reference.toString().contains(reference)
    }
} ?: -1

fun MutableMethod.getTargetIndexWithReference(reference: String) =
    getTargetIndexWithReference(0, reference)

fun MutableMethod.getTargetIndexWithReferenceReversed(reference: String) =
    getTargetIndexWithReferenceReversed(implementation!!.instructions.size - 1, reference)

fun MutableMethod.getTargetIndexWithReference(startIndex: Int, reference: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            (instruction as? ReferenceInstruction)?.reference.toString().contains(reference)
        }
    }

fun MutableMethod.getTargetIndexWithReferenceReversed(startIndex: Int, reference: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (!(instruction as? ReferenceInstruction)?.reference.toString().contains(reference))
            continue

        return index
    }
    return -1
}

fun MethodFingerprintResult.getWalkerMethod(context: BytecodeContext, index: Int) =
    mutableMethod.getWalkerMethod(context, index)

fun MutableMethod.getWalkerMethod(context: BytecodeContext, index: Int) =
    context.toMethodWalker(this)
        .nextMethod(index, true)
        .getMethod() as MutableMethod

fun BytecodeContext.updatePatchStatus(
    className: String,
    methodName: String
) {
    this.classes.forEach { classDef ->
        if (classDef.type.endsWith(className)) {
            val patchStatusMethod =
                this.proxy(classDef).mutableClass.methods.first { it.name == methodName }

            patchStatusMethod.replaceInstruction(
                0,
                "const/4 v0, 0x1"
            )
        }
    }
}
