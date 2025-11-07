package app.revanced.util

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.iface.reference.Reference
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.util.MethodUtil

fun MethodFingerprint.resultOrThrow() = result ?: throw exception

/**
 * The [PatchException] of failing to resolve a [MethodFingerprint].
 *
 * @return The [PatchException].
 */
val MethodFingerprint.exception
    get() = PatchException("Failed to resolve ${this.javaClass.simpleName}")

fun MethodFingerprint.alsoResolve(context: BytecodeContext, fingerprint: MethodFingerprint) =
    also { resolve(context, fingerprint.resultOrThrow().classDef) }.resultOrThrow()

fun BytecodeContext.findClass(className: String) =
    classes.findClassProxied { classDef -> classDef.type == className }

/**
 * Find the [MutableMethod] from a given [Method] in a [MutableClass].
 *
 * @param method The [Method] to find.
 * @return The [MutableMethod].
 */
internal fun MutableClass.findMutableMethodOf(method: Method) = this.methods.first {
    MethodUtil.methodSignaturesMatch(it, method)
}

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
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, opcode: Opcode): Int =
    indexOfFirstInstruction(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, predicate: Instruction.() -> Boolean): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = implementation!!.instructions
    if (startIndex != 0) {
        instructions = instructions.drop(startIndex)
    }
    val index = instructions.indexOfFirst(predicate)

    return if (index >= 0) {
        startIndex + index
    } else {
        -1
    }
}

fun Method.indexOfFirstInstructionOrThrow(opcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(0, opcode)

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return the index of the instruction
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(startIndex: Int = 0, opcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(startIndex) {
        this.opcode == opcode
    }

fun Method.indexOfFirstInstructionReversedOrThrow(opcode: Opcode): Int =
    indexOfFirstInstructionReversedOrThrow(null, opcode)

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

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun Method.indexOfFirstInstructionReversed(startIndex: Int? = null, opcode: Opcode): Int =
    indexOfFirstInstructionReversed(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun Method.indexOfFirstInstructionReversed(
    startIndex: Int? = null,
    predicate: Instruction.() -> Boolean
): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = implementation!!.instructions
    if (startIndex != null) {
        instructions = instructions.take(startIndex + 1)
    }

    return instructions.indexOfLast(predicate)
}

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversed
 */
fun Method.indexOfFirstInstructionReversedOrThrow(
    startIndex: Int? = null,
    opcode: Opcode
): Int =
    indexOfFirstInstructionReversedOrThrow(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversed
 */
fun Method.indexOfFirstInstructionReversedOrThrow(
    startIndex: Int? = null,
    predicate: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstructionReversed(startIndex, predicate)

    if (index < 0) {
        throw PatchException("Could not find instruction index")
    }

    return index
}

/**
 * @return The list of indices of the opcode in reverse order.
 */
fun Method.findOpcodeIndicesReversed(opcode: Opcode): List<Int> =
    findOpcodeIndicesReversed { this.opcode == opcode }

/**
 * @return The list of indices of the opcode in reverse order.
 */
fun Method.findOpcodeIndicesReversed(filter: Instruction.() -> Boolean): List<Int> {
    val indexes = implementation!!.instructions
        .withIndex()
        .filter { (_, instruction) -> filter.invoke(instruction) }
        .map { (index, _) -> index }
        .reversed()

    if (indexes.isEmpty()) throw PatchException("No matching instructions found in: $this")

    return indexes
}

/**
 * Find the index of the first wide literal instruction with the given value.
 *
 * @return the first literal instruction with the value, or -1 if not found.
 * @see indexOfFirstWideLiteralInstructionValueOrThrow
 */
fun Method.indexOfFirstWideLiteralInstructionValue(literal: Long) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1


/**
 * Find the index of the first wide literal instruction with the given value,
 * or throw an exception if not found.
 *
 * @return the first literal instruction with the value, or throws [PatchException] if not found.
 */
fun Method.indexOfFirstWideLiteralInstructionValueOrThrow(literal: Long): Int {
    val index = indexOfFirstWideLiteralInstructionValue(literal)
    if (index < 0) {
        val value =
            if (literal >= 2130706432) // 0x7f000000, general resource id
                String.format("%#X", literal).lowercase()
            else
                literal.toString()

        throw PatchException("Found literal value: '$value' but method does not contain the id: $this")
    }

    return index
}

fun Method.indexOfFirstStringInstruction(str: String) =
    indexOfFirstInstruction {
        opcode == Opcode.CONST_STRING &&
                getReference<StringReference>()?.string == str
    }


fun Method.indexOfFirstStringInstructionOrThrow(str: String): Int {
    val index = indexOfFirstStringInstruction(str)
    if (index < 0) {
        throw PatchException("Found string value for: '$str' but method does not contain the id: $this")
    }

    return index
}

/**
 * Check if the method contains a literal with the given value.
 *
 * @return if the method contains a literal with the given value.
 */
fun Method.containsWideLiteralInstructionValue(literal: Long) =
    indexOfFirstWideLiteralInstructionValue(literal) >= 0

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

fun MethodFingerprintResult.getWalkerMethod(context: BytecodeContext, offset: Int) =
    mutableMethod.getWalkerMethod(context, offset)

/**
 * MethodWalker can find the wrong class:
 * https://github.com/ReVanced/revanced-patcher/issues/309
 *
 * As a workaround, redefine MethodWalker here
 */
fun MutableMethod.getWalkerMethod(context: BytecodeContext, offset: Int): MutableMethod {
    val newMethod = getInstruction<ReferenceInstruction>(offset).reference as MethodReference
    return context.findMethodOrThrow(newMethod.definingClass) {
        MethodUtil.methodSignaturesMatch(this, newMethod)
    }
}

fun BytecodeContext.findMethodOrThrow(
    reference: String,
    methodPredicate: Method.() -> Boolean = { MethodUtil.isConstructor(this) }
) = findMethodsOrThrow(reference).first(methodPredicate)

fun BytecodeContext.findMethodsOrThrow(reference: String): MutableSet<MutableMethod> {
    val methods =
        findClass(reference)
            ?.mutableClass
            ?.methods

    if (methods != null) {
        return methods
    } else {
        throw PatchException("No matching methods found in: $reference")
    }
}

fun MutableMethod.methodCall(): String {
    var methodCall = "$definingClass->$name("
    for (i in 0 until parameters.size) {
        methodCall += parameterTypes[i]
    }
    methodCall += ")$returnType"
    return methodCall
}

