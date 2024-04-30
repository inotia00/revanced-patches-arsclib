package app.revanced.patches.reddit.utils.fix

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import org.w3c.dom.Element

@Patch(
    description = "Replace resource formats that APKTool does not yet support decoding with hardcoded values.\n" +
            "This method is not ideal, please remove this patch if APKTool will support decoding."
)
@Suppress("DEPRECATION", "unused")
object BrokenResourcePatch : ResourcePatch() {
    private val attrsName = arrayOf(
        "cornerFamily",
        "cornerFamilyBottomLeft",
        "cornerFamilyBottomRight",
        "cornerFamilyTopLeft",
        "labelBehavior",
        "labelVisibilityMode",
        "layout_constraintHorizontal_chainStyle",
        "layout_constraintVertical_chainStyle",
        "redditBaseTheme",
        "showAsAction",
        "surface_type"
    )

    private val styleMap = mapOf(
        "redditBaseTheme\">0<" to "redditBaseTheme\">AlienBlue<",
        "cornerFamilyBottomRight\">1<" to "cornerFamilyBottomRight\">cut<",
        "cornerFamily\">1<" to "cornerFamily\">cut<",
        "cornerFamilyTopLeft\">1<" to "cornerFamilyTopLeft\">cut<"
    )

    private val layoutXml = arrayOf(
        "award_sheet_footer.xml",
        "bottom_nav_item_normal.xml",
        "comment_header.xml",
        "comment_header_two_line.xml",
        "custom_feed_community_list_item.xml",
        "custom_feed_user_list_item.xml",
        "dialog_award_info.xml",
        "dialog_edit_overlay_text.xml",
        "dialog_user_modal.xml",
        "home_empty.xml",
        "include_edit_ugc_custom_actions.xml",
        "item_carousel_large.xml",
        "item_comment_two_line_header.xml",
        "item_community.xml",
        "item_create_community.xml",
        "item_pick_community.xml",
        "item_subreddit.xml",
        "item_topic.xml",
        "layout_community_type.xml",
        "layout_webembed_error.xml",
        "legacy_comment_header.xml",
        "link_carousel_item_subreddit_header.xml",
        "link_carousel_subreddit_header.xml",
        "listitem_community.xml",
        "listitem_modtools_user_v2.xml",
        "listitem_subreddit_with_status.xml",
        "listitem_user_flair.xml",
        "merge_button_wear_all.xml",
        "merge_equipped_fab.xml",
        "merge_link_footer.xml",
        "merge_link_header_metadata.xml",
        "merge_link_header_minimized_metadata.xml",
        "post_header_card.xml",
        "premium_marketing_perk_list_item.xml",
        "premium_marketing_perk_tile.xml",
        "premium_marketing_perk_tile_wide_highlighted.xml",
        "promoted_user_post_list_item.xml",
        "question_input_slider.xml",
        "reddit_video_controls.xml",
        "screen_confirm_recommended_snoovatar.xml",
        "screen_copy_snoovatar.xml",
        "screen_custom_feed.xml",
        "screen_default_two_button_dialog.xml",
        "screen_fullbleed_video.xml",
        "screen_ratingsurvey_tag.xml",
        "screen_share_and_download.xml",
        "screen_vault_feed_empty_content.xml",
        "setting_subredditnotiflevel.xml",
        "toast.xml",
        "trophy_item.xml",
        "vault_toast.xml",
        "video_user_profile_card.xml",
        "view_video_controls.xml"
    )
    private lateinit var context: ResourceContext

    override fun execute(context: ResourceContext) {
        this.context = context
    }

    internal fun fixBrokenResource() {
        try {
            styleMap.forEach { (from, to) ->
                context["res/values/styles.xml"].apply {
                    writeText(
                        readText()
                            .replace(
                                from,
                                to
                            )
                    )
                }
            }

            layoutXml.forEach { file ->
                val targetXml = context["res"].resolve("layout").resolve(file)
                if (targetXml.exists()) {
                    context["res/layout/$file"].apply {
                        writeText(
                            readText()
                                .replace(
                                    "chainStyle=\"2\"",
                                    "chainStyle=\"packed\""
                                )
                        )
                    }
                }
            }

            arrayOf(
                "experiments_override_menu.xml",
                "menu_protect_vault.xml"
            ).forEach { file ->
                val targetXml = context["res"].resolve("menu").resolve(file)
                if (targetXml.exists()) {
                    context.xmlEditor["res/menu/$file"].use { editor ->
                        editor.file.doRecursively loop@{
                            if (it !is Element) return@loop

                            it.getAttributeNode("app:showAsAction")?.let { attribute ->
                                attribute.textContent = "ifRoom|withText"
                            }
                        }
                    }
                }
            }

            context.xmlEditor["res/values/attrs.xml"].use { editor ->
                editor.file.doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("name")?.let { attribute ->
                        if (attrsName.indexOf(attribute.textContent) >= 0) {
                            it.parentNode?.removeChild(it)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        context.copyXmlNode("reddit/fix/host", "values/attrs.xml", "resources")
    }

}