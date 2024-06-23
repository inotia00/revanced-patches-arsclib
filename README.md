## 🧩 Patches

The official Patch bundle provided by ReVanced and the community.

> Looking for the JSON variant of this? [Click here](patches.json).

### [📦 `com.reddit.frontpage`](https://play.google.com/store/apps/details?id=com.reddit.frontpage)
<details>

| 💊 Patch | 📜 Description | 🏹 Target Version |
|:--------:|:--------------:|:-----------------:|
| `Change version code` | Changes the version code of the app. By default the highest version code is set. This allows older versions of an app to be installed if their version code is set to the same or a higher value and can stop app stores to update the app. | ALL |
| `Custom branding name for Reddit` | Renames the Reddit app to the name specified in options.json. | ALL |
| `Disable screenshot popup` | Adds an option to disable the popup that appears when taking a screenshot. | ALL |
| `Hide Recently Visited shelf` | Adds an option to hide the Recently Visited shelf in the sidebar. | ALL |
| `Hide ads` | Adds options to hide ads. | ALL |
| `Hide navigation buttons` | Adds options to hide buttons in the navigation bar. | ALL |
| `Hide recommended communities shelf` | Adds an option to hide the recommended communities shelves in subreddits. | ALL |
| `Open links directly` | Adds an option to skip over redirection URLs in external links. | ALL |
| `Open links externally` | Adds an option to always open links in your browser instead of in the in-app-browser. | ALL |
| `Premium icon` | Unlocks premium app icons. | ALL |
| `Remove subreddit dialog` | Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically. | ALL |
| `Sanitize sharing links` | Adds an option to remove tracking query parameters from URLs when sharing links. | ALL |
| `Settings for Reddit` | Applies mandatory patches to implement ReVanced Extended settings into the application. | ALL |
</details>



## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

The file contains an array of objects, each object representing a patch. The object contains the following properties:

| key                           | description                                                                                                                                                                           |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name`                        | The name of the patch.                                                                                                                                                                |
| `description`                 | The description of the patch.                                                                                                                                                         |
| `version`                     | The version of the patch.                                                                                                                                                             |
| `excluded`                    | Whether the patch is excluded by default. If `true`, the patch must never be included by default.                                                                                     |
| `options`                     | An array of options for this patch.                                                                                                                                                   |
| `options.key`                 | The key of the option.                                                                                                                                                                |
| `options.title`               | The title of the option.                                                                                                                                                              |
| `options.description`         | The description of the option.                                                                                                                                                        |
| `options.required`            | Whether the option is required.                                                                                                                                                       |
| `options.choices?`            | An array of choices of the option. This may be `null` if this option has no choices. The element type of this array may be any type. It can be a `String`, `Int` or something else.   |
| `dependencies`                | An array of dependencies, which are patch names.                                                                                                                                      |
| `compatiblePackages`          | An array of packages compatible with this patch.                                                                                                                                      |
| `compatiblePackages.name`     | The name of the package.                                                                                                                                                              |
| `compatiblePackages.versions` | An array of versions of the package compatible with this patch. If empty, all versions are seemingly compatible.                                                                      |