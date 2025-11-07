package app.revanced.patches.reddit.ad.comments

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.or
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.ad.comments.fingerprints.CommentAdCommentScreenAdViewFingerprint
import app.revanced.patches.reddit.ad.comments.fingerprints.CommentAdDetailListHeaderViewFingerprint
import app.revanced.patches.reddit.ad.comments.fingerprints.CommentsViewModelConstructorFingerprint
import app.revanced.patches.reddit.ad.comments.fingerprints.PostDetailAdLoaderFingerprint
import app.revanced.patches.reddit.ad.comments.fingerprints.PostDetailPresenterFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstStringInstruction
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

class CommentAdsPatch : BytecodePatch(
    setOf(
        CommentAdCommentScreenAdViewFingerprint,
        CommentAdDetailListHeaderViewFingerprint,
        CommentsViewModelConstructorFingerprint,
        PostDetailAdLoaderFingerprint,
        PostDetailPresenterFingerprint,
    )
) {
    companion object {
        private const val INTEGRATION_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/GeneralAdsPatch;"
    }

    override fun execute(context: BytecodeContext) {
        fun MutableMethod.hook() =
            addInstructions(
                0, """
                    invoke-static {}, $INTEGRATION_CLASS_DESCRIPTOR->hideCommentAds()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                    """
            )

        if (!is_2025_06_or_greater) {
            val isCommentAdsMethod: Method.() -> Boolean = {
                parameterTypes.size == 1 &&
                        parameterTypes.first().startsWith("Lcom/reddit/ads/conversation/") &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        returnType == "V" &&
                        indexOfFirstStringInstruction("ad") >= 0
            }

            context.classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.isCommentAdsMethod()) {
                        context.classes.proxy(classDef)
                            .mutableClass
                            .findMutableMethodOf(method)
                            .hook()
                    }
                }
            }
        } else if (!is_2025_40_or_greater) {
            listOf(
                CommentAdCommentScreenAdViewFingerprint,
                CommentAdDetailListHeaderViewFingerprint,
                PostDetailPresenterFingerprint
            ).forEach { fingerprint ->
                fingerprint.resultOrThrow().mutableMethod.hook()
            }
        } else {
            CommentsViewModelConstructorFingerprint.resultOrThrow().let {
                it.classDef.methods.filter { method ->
                    method.indexOfFirstInstruction {
                        opcode == Opcode.INVOKE_DIRECT &&
                                getReference<MethodReference>()?.toString()
                                    ?.endsWith("<init>(ZI)V") == true
                    } >= 0
                }.forEach { method ->
                    context.classes.proxy(it.classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .hook()
                }
            }

            PostDetailAdLoaderFingerprint
                .resultOrThrow()
                .mutableMethod
                .apply {
                    implementation!!.instructions
                        .withIndex()
                        .filter { (_, instruction) ->
                            val reference =
                                (instruction as? ReferenceInstruction)?.reference
                            reference is MethodReference &&
                                    reference.toString() == "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                        }
                        .map { (index, _) -> index }
                        .reversed()
                        .forEach { index ->
                            val instruction =
                                getInstruction<FiveRegisterInstruction>(index)

                            replaceInstruction(
                                index,
                                "invoke-static { v${instruction.registerC}, v${instruction.registerD}, v${instruction.registerE} }, " +
                                        "$INTEGRATION_CLASS_DESCRIPTOR->hideCommentAdMap(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                            )
                        }
                }
        }
    }
}
