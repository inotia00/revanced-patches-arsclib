package app.revanced.patches.youtube.utils.gms

import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE
import app.revanced.patches.youtube.utils.fix.clientspoof.ClientSpoofPatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(ClientSpoofPatch::class, PackageNamePatch::class, SettingsPatch::class),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = setOf(
        CompatiblePackage(
            "com.google.android.youtube",
            setOf(
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39"
            ),
        ),
    )
)
