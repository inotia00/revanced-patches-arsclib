package app.revanced.patches.reddit.utils.integrations

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch

@Name("reddit-integrations")
@RedditCompatibility
@RequiresIntegrations
internal class IntegrationsPatch : BaseIntegrationsPatch(
    setOf(InitFingerprint),
)