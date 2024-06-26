package app.revanced.patches.reddit.layout.recentlyvisited.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

internal object CommunityDrawerPresenterFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.XOR_INT_2ADDR,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    )
)