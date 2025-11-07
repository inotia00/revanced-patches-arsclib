package app.revanced.patches.reddit.utils.annotation

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [
        Package(
            "com.reddit.frontpage",
            arrayOf(
                "2025.40.0",
                "2025.44.0",
            )
        )
    ]
)
@Target(AnnotationTarget.CLASS)
internal annotation class RedditCompatibility

