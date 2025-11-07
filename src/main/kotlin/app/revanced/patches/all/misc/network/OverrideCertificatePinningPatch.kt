package app.revanced.patches.all.misc.network

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.util.ResourceUtils.adoptChild
import app.revanced.util.ResourceUtils.base
import app.revanced.util.ResourceUtils.getNode
import app.revanced.util.ResourceUtils.getResourcePath
import app.revanced.util.ResourceUtils.manifestEditor
import app.revanced.util.ResourceUtils.trimIndentMultiline
import org.w3c.dom.Element
import java.io.File

@Patch
@Name("Override certificate pinning")
@Description("Overrides certificate pinning, allowing to inspect traffic via a proxy.")
@Suppress("unused")
class OverrideCertificatePinningPatch : ResourcePatch {
    companion object : OptionsContainer() {
        private const val NETWORK_SECURITY_CONFIG_ATTRIBUTE_NAME =
            "android:networkSecurityConfig"

        private var OverrideCertificatePinning by option(
            PatchOption.BooleanOption(
                key = "OverrideCertificatePinning",
                default = false,
                title = "Override certificate pinning",
                description = "Overrides certificate pinning, allowing to inspect traffic via a proxy.",
                required = true,
            )
        )
    }

    override fun execute(context: ResourceContext) {
        if (OverrideCertificatePinning == false) {
            println("INFO: Certificate pinning will remain unchanged as 'OverrideCertificatePinning' is false.")
            return
        }

        var networkSecurityFileName = "network_security_config.xml"

        context.manifestEditor().use { editor ->
            val document = editor.file
            val applicationNode = document
                .getElementsByTagName("application")
                .item(0) as Element

            if (applicationNode.hasAttribute(NETWORK_SECURITY_CONFIG_ATTRIBUTE_NAME)) {
                networkSecurityFileName =
                    applicationNode.getAttribute(NETWORK_SECURITY_CONFIG_ATTRIBUTE_NAME)
                        .split("/")[1] + ".xml"
            } else {
                document.createAttribute(NETWORK_SECURITY_CONFIG_ATTRIBUTE_NAME)
                    .apply { value = "@xml/network_security_config" }
                    .let(applicationNode.attributes::setNamedItem)
            }
        }

        val xmlPath = "res/xml/$networkSecurityFileName"
        val openFile = context.base.openFile(xmlPath)

        if (openFile.exists) {
            openFile.close()

            context.base.openXmlFile(xmlPath).use { document ->
                arrayOf(
                    "base-config",
                    "debug-overrides"
                ).forEach { tagName ->
                    val configElement = document.getNode(tagName) as Element
                    val configChildNodes = configElement.childNodes
                    for (i in 0 until configChildNodes.length) {
                        val anchorNode = configChildNodes.item(i)
                        if (anchorNode is Element && anchorNode.tagName == "trust-anchors") {
                            var injected = false
                            val certificatesChildNodes = anchorNode.childNodes
                            for (i in 0 until certificatesChildNodes.length) {
                                val node = certificatesChildNodes.item(i)
                                if (node is Element && node.tagName == "certificates") {
                                    if (node.hasAttribute("src") && node.getAttribute("src") == "user") {
                                        node.setAttribute("overridePins", "true")
                                        injected = true
                                    }
                                }
                            }
                            if (!injected) {
                                anchorNode.adoptChild("certificates") {
                                    setAttribute("src", "user")
                                    setAttribute("overridePins", "true")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            openFile.close()

            // In case the file does not exist create the "network_security_config.xml" file.
            File(getResourcePath().resolve("xml"), networkSecurityFileName).apply {
                writeText(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <network-security-config>
                        <base-config cleartextTrafficPermitted="true">
                            <trust-anchors>
                                <certificates src="system" />
                                <certificates
                                    src="user"
                                    overridePins="true" />
                            </trust-anchors>
                        </base-config>
                        <debug-overrides>
                            <trust-anchors>
                                <certificates src="system" />
                                <certificates
                                    src="user"
                                    overridePins="true" />
                            </trust-anchors>
                        </debug-overrides>
                    </network-security-config>
                    """.trimIndentMultiline(),
                )
            }
        }
    }
}
