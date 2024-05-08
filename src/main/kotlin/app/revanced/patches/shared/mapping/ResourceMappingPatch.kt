package app.revanced.patches.shared.mapping

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.ResourceUtils.resourceIdOf

class ResourceMappingPatch : ResourcePatch {
    companion object {
        private var resourceContext: ResourceContext? = null

        fun getId(resourceType: ResourceType, name: String): Long =
            resourceIdOf(resourceType.value, name)

        /**
         * Resolve a resource id for the specified resource.
         *
         * @param type The type of the resource.
         * @param name The name of the resource.
         * @return The id of the resource.
         */
        private fun resourceIdOf(type: String, name: String): Long = resourceContext?.resourceIdOf(type, name)
            ?: -1L
    }

    override fun execute(context: ResourceContext) {
        resourceContext = context

    }
}
