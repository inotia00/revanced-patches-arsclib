package app.revanced.patches.reddit.misc.openlink

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.misc.openlink.fingerprints.CustomReportsFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.ScreenNavigatorFingerprint
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow

class ScreenNavigatorMethodResolverPatch : BytecodePatch(
    listOf(
        CustomReportsFingerprint,
        ScreenNavigatorFingerprint
    )
) {
    companion object {
        lateinit var screenNavigatorMethod: MutableMethod
    }

    override fun execute(context: BytecodeContext) {
        ScreenNavigatorFingerprint.result?.mutableMethod?.apply {
            // ~ Reddit 2024.25.3
            screenNavigatorMethod = this
        } ?: CustomReportsFingerprint.resultOrThrow().mutableMethod.apply {
            // Reddit 2024.26.1 ~
            val walkerIndex = CustomReportsFingerprint.indexOfScreenNavigator(this)
            screenNavigatorMethod = getWalkerMethod(context, walkerIndex)
        }
    }
}