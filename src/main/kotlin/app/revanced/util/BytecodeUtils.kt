package app.revanced.util

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.InstructionUtils.Companion.writeOpcodes
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcode.*
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.iface.reference.Reference
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.util.MethodUtil
import java.util.EnumSet

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

fun navigate(context: BytecodeContext, method: MethodReference) = MethodNavigator(context, method)

fun parametersStartsWith(
    parameters1: Iterable<CharSequence>,
    parameters2: Iterable<CharSequence>
): Boolean {
    if (parameters1.count() < parameters2.count()) return false
    val iterator1 = parameters1.iterator()
    parameters2.forEach {
        if (!iterator1.next().startsWith(it)) return false
    }
    return true
}

/**
 * @return The register that is written to by this instruction,
 *         or NULL if this is not a write opcode.
 */
internal val Instruction.writeRegister: Int?
    get() {
        if (this.opcode !in writeOpcodes) {
            return null
        }
        if (this !is OneRegisterInstruction) {
            throw IllegalStateException("Not a write instruction: $this")
        }
        return registerA
    }

/**
 * Find the instruction index used for a toString() StringBuilder write of a given String name.
 *
 * @param fieldName The name of the field to find.  Partial matches are allowed.
 */
private fun MutableMethod.findInstructionIndexFromToString(fieldName: String) : Int {
    val stringIndex = indexOfFirstInstruction {
        val reference = getReference<StringReference>()
        reference?.string?.contains(fieldName) == true
    }
    if (stringIndex < 0) {
        throw IllegalArgumentException("Could not find usage of string: '$fieldName'")
    }
    val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

    // Find use of the string with a StringBuilder.
    val stringUsageIndex = indexOfFirstInstruction(stringIndex) {
        val reference = getReference<MethodReference>()
        reference?.definingClass == "Ljava/lang/StringBuilder;" &&
                (this as? FiveRegisterInstruction)?.registerD == stringRegister
    }
    if (stringUsageIndex < 0) {
        throw IllegalArgumentException("Could not find StringBuilder usage in: $this")
    }

    // Find the next usage of StringBuilder, which should be the desired field.
    val fieldUsageIndex = indexOfFirstInstruction(stringUsageIndex + 1) {
        val reference = getReference<MethodReference>()
        reference?.definingClass == "Ljava/lang/StringBuilder;" && reference.name == "append"
    }
    if (fieldUsageIndex < 0) {
        // Should never happen.
        throw IllegalArgumentException("Could not find StringBuilder append usage in: $this")
    }
    val fieldUsageRegister = getInstruction<FiveRegisterInstruction>(fieldUsageIndex).registerD

    // Look backwards up the method to find the instruction that sets the register.
    var fieldSetIndex = indexOfFirstInstructionReversedOrThrow(fieldUsageIndex - 1) {
        fieldUsageRegister == writeRegister
    }

    // If the field is a method call, then adjust from MOVE_RESULT to the method call.
    val fieldSetOpcode = getInstruction(fieldSetIndex).opcode
    if (fieldSetOpcode == MOVE_RESULT ||
        fieldSetOpcode == MOVE_RESULT_WIDE ||
        fieldSetOpcode == MOVE_RESULT_OBJECT) {
        fieldSetIndex--
    }

    return fieldSetIndex
}

/**
 * Find the method used for a toString() StringBuilder write of a given String name.
 *
 * @param fieldName The name of the field to find.  Partial matches are allowed.
 */
internal fun MutableMethod.findMethodFromToString(context: BytecodeContext, fieldName: String) : MutableMethod {
    val methodUsageIndex = findInstructionIndexFromToString(fieldName)
    return navigate(context, this).to(methodUsageIndex).stop()
}

/**
 * Find the field used for a toString() StringBuilder write of a given String name.
 *
 * @param fieldName The name of the field to find.  Partial matches are allowed.
 */
internal fun MutableMethod.findFieldFromToString(fieldName: String) : FieldReference {
    val methodUsageIndex = findInstructionIndexFromToString(fieldName)
    return getInstruction<ReferenceInstruction>(methodUsageIndex).getReference<FieldReference>()!!
}

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

private class InstructionUtils {
    companion object {
        val branchOpcodes: EnumSet<Opcode> = EnumSet.of(
            GOTO, GOTO_16, GOTO_32,
            IF_EQ, IF_NE, IF_LT, IF_GE, IF_GT, IF_LE,
            IF_EQZ, IF_NEZ, IF_LTZ, IF_GEZ, IF_GTZ, IF_LEZ,
            PACKED_SWITCH_PAYLOAD, SPARSE_SWITCH_PAYLOAD
        )

        val returnOpcodes: EnumSet<Opcode> = EnumSet.of(
            RETURN_VOID, RETURN, RETURN_WIDE, RETURN_OBJECT, RETURN_VOID_NO_BARRIER,
            THROW
        )

        val writeOpcodes: EnumSet<Opcode> = EnumSet.of(
            ARRAY_LENGTH,
            INSTANCE_OF,
            NEW_INSTANCE, NEW_ARRAY,
            MOVE, MOVE_FROM16, MOVE_16, MOVE_WIDE, MOVE_WIDE_FROM16, MOVE_WIDE_16, MOVE_OBJECT,
            MOVE_OBJECT_FROM16, MOVE_OBJECT_16, MOVE_RESULT, MOVE_RESULT_WIDE, MOVE_RESULT_OBJECT, MOVE_EXCEPTION,
            CONST, CONST_4, CONST_16, CONST_HIGH16, CONST_WIDE_16, CONST_WIDE_32,
            CONST_WIDE, CONST_WIDE_HIGH16, CONST_STRING, CONST_STRING_JUMBO,
            IGET, IGET_WIDE, IGET_OBJECT, IGET_BOOLEAN, IGET_BYTE, IGET_CHAR, IGET_SHORT,
            IGET_VOLATILE, IGET_WIDE_VOLATILE, IGET_OBJECT_VOLATILE,
            SGET, SGET_WIDE, SGET_OBJECT, SGET_BOOLEAN, SGET_BYTE, SGET_CHAR, SGET_SHORT,
            SGET_VOLATILE, SGET_WIDE_VOLATILE, SGET_OBJECT_VOLATILE,
            AGET, AGET_WIDE, AGET_OBJECT, AGET_BOOLEAN, AGET_BYTE, AGET_CHAR, AGET_SHORT,
            // Arithmetic and logical operations.
            ADD_DOUBLE_2ADDR, ADD_DOUBLE, ADD_FLOAT_2ADDR, ADD_FLOAT, ADD_INT_2ADDR,
            ADD_INT_LIT8, ADD_INT, ADD_LONG_2ADDR, ADD_LONG, ADD_INT_LIT16,
            AND_INT_2ADDR, AND_INT_LIT8, AND_INT_LIT16, AND_INT, AND_LONG_2ADDR, AND_LONG,
            DIV_DOUBLE_2ADDR, DIV_DOUBLE, DIV_FLOAT_2ADDR, DIV_FLOAT, DIV_INT_2ADDR,
            DIV_INT_LIT16, DIV_INT_LIT8, DIV_INT, DIV_LONG_2ADDR, DIV_LONG,
            DOUBLE_TO_FLOAT, DOUBLE_TO_INT, DOUBLE_TO_LONG,
            FLOAT_TO_DOUBLE, FLOAT_TO_INT, FLOAT_TO_LONG,
            INT_TO_BYTE, INT_TO_CHAR, INT_TO_DOUBLE, INT_TO_FLOAT, INT_TO_LONG, INT_TO_SHORT,
            LONG_TO_DOUBLE, LONG_TO_FLOAT, LONG_TO_INT,
            MUL_DOUBLE_2ADDR, MUL_DOUBLE, MUL_FLOAT_2ADDR, MUL_FLOAT, MUL_INT_2ADDR,
            MUL_INT_LIT16, MUL_INT_LIT8, MUL_INT, MUL_LONG_2ADDR, MUL_LONG,
            NEG_DOUBLE, NEG_FLOAT, NEG_INT, NEG_LONG,
            NOT_INT, NOT_LONG,
            OR_INT_2ADDR, OR_INT_LIT16, OR_INT_LIT8, OR_INT, OR_LONG_2ADDR, OR_LONG,
            REM_DOUBLE_2ADDR, REM_DOUBLE, REM_FLOAT_2ADDR, REM_FLOAT, REM_INT_2ADDR,
            REM_INT_LIT16, REM_INT_LIT8, REM_INT, REM_LONG_2ADDR, REM_LONG,
            RSUB_INT_LIT8, RSUB_INT,
            SHL_INT_2ADDR, SHL_INT_LIT8, SHL_INT, SHL_LONG_2ADDR, SHL_LONG,
            SHR_INT_2ADDR, SHR_INT_LIT8, SHR_INT, SHR_LONG_2ADDR, SHR_LONG,
            SUB_DOUBLE_2ADDR, SUB_DOUBLE, SUB_FLOAT_2ADDR, SUB_FLOAT, SUB_INT_2ADDR,
            SUB_INT, SUB_LONG_2ADDR, SUB_LONG,
            USHR_INT_2ADDR, USHR_INT_LIT8, USHR_INT, USHR_LONG_2ADDR, USHR_LONG,
            XOR_INT_2ADDR, XOR_INT_LIT16, XOR_INT_LIT8, XOR_INT, XOR_LONG_2ADDR, XOR_LONG,
        )
    }
}
