@file:Suppress("unused")

package app.revanced.util

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.iface.reference.Reference

fun MethodFingerprint.resultOrThrow() = result ?: throw exception

/**
 * The [PatchException] of failing to resolve a [MethodFingerprint].
 *
 * @return The [PatchException].
 */
val MethodFingerprint.exception
    get() = PatchException("Failed to resolve ${this.javaClass.simpleName}")

fun MutableMethodImplementation.getInstruction(index: Int): BuilderInstruction =
    instructions[index]

@Suppress("UNCHECKED_CAST")
fun <T> MutableMethodImplementation.getInstruction(index: Int): T =
    getInstruction(index) as T

fun MutableMethod.getInstruction(index: Int): BuilderInstruction =
    implementation!!.getInstruction(index)

fun <T> MutableMethod.getInstruction(index: Int): T =
    implementation!!.getInstruction<T>(index)

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

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, predicate: Instruction.() -> Boolean): Int {
    val index = this.implementation!!.instructions.drop(startIndex).indexOfFirst(predicate)

    return if (index >= 0) {
        startIndex + index
    } else {
        -1
    }
}

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return the index of the instruction
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(
    startIndex: Int = 0,
    predicate: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstruction(startIndex, predicate)
    if (index < 0) {
        throw PatchException("Could not find instruction index")
    }
    return index
}

fun MutableMethod.getTargetIndexOrThrow(opcode: Opcode) =
    getTargetIndexOrThrow(0, opcode)

fun MutableMethod.getTargetIndexOrThrow(startIndex: Int, opcode: Opcode) =
    checkIndex(getTargetIndex(startIndex, opcode), startIndex, opcode)

fun MutableMethod.getTargetIndexReversedOrThrow(opcode: Opcode) =
    getTargetIndexReversedOrThrow(implementation!!.instructions.lastIndex, opcode)

fun MutableMethod.getTargetIndexReversedOrThrow(startIndex: Int, opcode: Opcode) =
    checkIndex(getTargetIndexReversed(startIndex, opcode), startIndex, opcode)

fun Method.getTargetIndexWithFieldReferenceNameOrThrow(filedName: String) =
    checkIndex(getTargetIndexWithFieldReferenceName(filedName), 0, filedName)

fun MutableMethod.getTargetIndexWithFieldReferenceNameOrThrow(startIndex: Int, filedName: String) =
    checkIndex(getTargetIndexWithFieldReferenceName(startIndex, filedName), startIndex, filedName)

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversedOrThrow(returnType: String) =
    getTargetIndexWithFieldReferenceNameReversedOrThrow(implementation!!.instructions.lastIndex, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversedOrThrow(startIndex: Int, returnType: String) =
    checkIndex(getTargetIndexWithFieldReferenceNameReversed(startIndex, returnType), startIndex, returnType)

fun Method.getTargetIndexWithFieldReferenceTypeOrThrow(returnType: String) =
    checkIndex(getTargetIndexWithFieldReferenceType(returnType), 0, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceTypeOrThrow(startIndex: Int, returnType: String) =
    checkIndex(getTargetIndexWithFieldReferenceType(startIndex, returnType), startIndex, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversedOrThrow(returnType: String) =
    getTargetIndexWithFieldReferenceTypeReversedOrThrow(implementation!!.instructions.lastIndex, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversedOrThrow(startIndex: Int, returnType: String) =
    checkIndex(getTargetIndexWithFieldReferenceTypeReversed(startIndex, returnType), startIndex, returnType)

fun Method.getTargetIndexWithMethodReferenceNameOrThrow(methodName: String) =
    checkIndex(getTargetIndexWithMethodReferenceName(methodName), 0, methodName)

fun MutableMethod.getTargetIndexWithMethodReferenceNameOrThrow(startIndex: Int, methodName: String) =
    checkIndex(getTargetIndexWithMethodReferenceName(startIndex, methodName), startIndex, methodName)

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversedOrThrow(methodName: String) =
    getTargetIndexWithMethodReferenceNameReversedOrThrow(implementation!!.instructions.lastIndex, methodName)

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversedOrThrow(startIndex: Int, methodName: String) =
    checkIndex(getTargetIndexWithMethodReferenceNameReversed(startIndex, methodName), startIndex, methodName)

fun Method.getTargetIndexWithReferenceOrThrow(reference: String) =
    checkIndex(getTargetIndexWithReference(reference), 0, reference)

fun MutableMethod.getTargetIndexWithReferenceOrThrow(startIndex: Int, reference: String) =
    checkIndex(getTargetIndexWithReference(startIndex, reference), startIndex, reference)

fun MutableMethod.getTargetIndexWithReferenceReversedOrThrow(reference: String) =
    getTargetIndexWithReferenceReversedOrThrow(implementation!!.instructions.lastIndex, reference)

fun MutableMethod.getTargetIndexWithReferenceReversedOrThrow(startIndex: Int, reference: String) =
    checkIndex(getTargetIndexWithReferenceReversed(startIndex, reference), startIndex, reference)

fun checkIndex(index: Int, startIndex: Int, opcode: Opcode): Int {
    if (index < 0) {
        throw PatchException("Target index not found. startIndex: $startIndex, opcode: $opcode")
    }
    return index
}

fun checkIndex(index: Int, startIndex: Int, name: String): Int {
    if (index < 0) {
        throw PatchException("Target index not found. startIndex: $startIndex, name: $name")
    }
    return index
}

fun MutableMethod.getTargetIndex(opcode: Opcode) = getTargetIndex(0, opcode)

fun MutableMethod.getTargetIndexReversed(opcode: Opcode) =
    getTargetIndexReversed(implementation!!.instructions.size - 1, opcode)

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
    context
        .traceMethodCalls(this)
        .nextMethod(index, true)
        .getMethod() as MutableMethod
