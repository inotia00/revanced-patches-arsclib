package app.revanced.patches.reddit.layout.communities.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex
import org.jf.dexlib2.AccessFlags

internal object CommunityRecommendationSectionFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.startsWith("Lcom/reddit/onboardingfeedscomponents/communityrecommendation/impl/section/composables/")
                && methodDef.containsWideLiteralInstructionIndex(-2058690088)
    }
)