package app.revanced.patches.music.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.fix.accessibility.AccessibilityNodeInfoPatch
import app.revanced.patches.music.utils.settings.ResourceUtils.YOUTUBE_MUSIC_SETTINGS_KEY
import app.revanced.patches.music.utils.settings.ResourceUtils.addMusicPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.addMusicPreferenceCategory
import app.revanced.patches.music.utils.settings.ResourceUtils.addMusicPreferenceWithIntent
import app.revanced.patches.music.utils.settings.ResourceUtils.addMusicPreferenceWithoutSummary
import app.revanced.patches.music.utils.settings.ResourceUtils.addReVancedMusicPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.sortMusicPreferenceCategory
import app.revanced.patches.shared.settings.AbstractSettingsResourcePatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import org.w3c.dom.Element
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Patch(
    name = "Settings",
    description = "Adds ReVanced Extended settings to YouTube Music.",
    dependencies = [
        AccessibilityNodeInfoPatch::class,
        SettingsBytecodePatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
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
            ]
        )
    ]
)
@Suppress("DEPRECATION", "unused")
object SettingsPatch : AbstractSettingsResourcePatch(
    "music/settings"
), Closeable {
    private val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
    private val threadPoolExecutor = Executors.newFixedThreadPool(THREAD_COUNT)

    lateinit var contexts: ResourceContext
    internal var upward0636: Boolean = false
    internal var upward0642: Boolean = false

    override fun execute(context: ResourceContext) {
        contexts = context

        val resourceXmlFile = context["res/values/integers.xml"].readBytes()

        for (threadIndex in 0 until THREAD_COUNT) {
            threadPoolExecutor.execute thread@{
                context.xmlEditor[resourceXmlFile.inputStream()].use { editor ->
                    val resources = editor.file.documentElement.childNodes
                    val resourcesLength = resources.length
                    val jobSize = resourcesLength / THREAD_COUNT

                    val batchStart = jobSize * threadIndex
                    val batchEnd = jobSize * (threadIndex + 1)
                    element@ for (i in batchStart until batchEnd) {
                        if (i >= resourcesLength) return@thread

                        val node = resources.item(i)
                        if (node !is Element) continue

                        if (node.nodeName != "integer" || !node.getAttribute("name")
                                .startsWith("google_play_services_version")
                        ) continue

                        val playServicesVersion = node.textContent.toInt()

                        upward0636 = 240399000 <= playServicesVersion
                        upward0642 = 240999000 <= playServicesVersion

                        break
                    }
                }
            }
        }

        threadPoolExecutor
            .also { it.shutdown() }
            .awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)

        /**
         * create directory for the untranslated language resources
         */
        context["res/values-v21"].mkdirs()

        arrayOf(
            ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("music/settings", resourceGroup)
        }

        /**
         * hide divider
         */
        val styleFile = context["res/values/styles.xml"]

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "allowDividerAbove\">true",
                    "allowDividerAbove\">false"
                ).replace(
                    "allowDividerBelow\">true",
                    "allowDividerBelow\">false"
                )
        )


        /**
         * Copy colors
         */
        context.xmlEditor["res/values/colors.xml"].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "material_deep_teal_500" -> "@android:color/white"

                    else -> continue
                }
            }
        }

        context.addReVancedMusicPreference(YOUTUBE_MUSIC_SETTINGS_KEY)

        super.execute(context)

    }

    internal fun addMusicPreference(
        category: CategoryType,
        key: String,
        defaultValue: String
    ) {
        addMusicPreference(category, key, defaultValue, "")
    }

    internal fun addMusicPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        dependencyKey: String
    ) {
        val categoryValue = category.value
        contexts.addMusicPreferenceCategory(categoryValue)
        contexts.addMusicPreference(categoryValue, key, defaultValue, dependencyKey)
    }

    internal fun addMusicPreferenceWithoutSummary(
        category: CategoryType,
        key: String,
        defaultValue: String
    ) {
        val categoryValue = category.value
        contexts.addMusicPreferenceCategory(categoryValue)
        contexts.addMusicPreferenceWithoutSummary(categoryValue, key, defaultValue)
    }

    internal fun addMusicPreferenceWithIntent(
        category: CategoryType,
        key: String
    ) {
        addMusicPreferenceWithIntent(category, key, "")
    }

    internal fun addMusicPreferenceWithIntent(
        category: CategoryType,
        key: String,
        dependencyKey: String
    ) {
        val categoryValue = category.value
        contexts.addMusicPreferenceCategory(categoryValue)
        contexts.addMusicPreferenceWithIntent(categoryValue, key, dependencyKey)
    }

    override fun close() {
        /**
         * Copy arrays
         */
        contexts.copyXmlNode("music/settings/host", "values/arrays.xml", "resources")

        addMusicPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_extended_settings_import_export",
            ""
        )

        CategoryType.entries.sorted().forEach {
            contexts.sortMusicPreferenceCategory(it.value)
        }
    }
}
