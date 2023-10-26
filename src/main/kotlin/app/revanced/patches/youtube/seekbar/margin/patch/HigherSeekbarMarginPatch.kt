package app.revanced.patches.youtube.seekbar.margin.patch

import app.revanced.patcher.data.ResourceContext

import app.revanced.extensions.doRecursively

import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import org.w3c.dom.Element

@Patch(
    name = "Higher fullscreen seekbar height",
    description = "When turned on Hide Fullscreen Bottom Container, the seekbar become unclickable for some users. This patch will solve it.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
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
                "18.40.34"
            ]
        )
    ]
)
@Suppress("unused")
object HigherSeekbarMarginPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
        context.xmlEditor["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:id")?.let { attribute ->
                    if (attribute.textContent == "@id/quick_actions_container") {
                        it.getAttributeNode("android:paddingTop").textContent = "20.0dip"
                    }
                }
            }
        }

        SettingsPatch.updatePatchStatus("Higher fullscreen seekbar height")
    }
}
