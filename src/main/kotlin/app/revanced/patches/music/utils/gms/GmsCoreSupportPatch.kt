package app.revanced.patches.music.utils.gms

import app.revanced.patches.music.utils.fix.clientspoof.ClientSpoofPatch
import app.revanced.patches.music.utils.fix.fileprovider.FileProviderPatch
import app.revanced.patches.music.utils.integrations.IntegrationsPatch
import app.revanced.patches.music.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(ClientSpoofPatch::class, PackageNamePatch::class, FileProviderPatch::class),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = setOf(
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            setOf(
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ),
        ),
    )
)
