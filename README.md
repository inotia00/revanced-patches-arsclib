## 🧩 Patches

The official Patch bundle provided by ReVanced and the community.

> Looking for the JSON variant of this? [Click here](patches.json).

### [📦 `com.reddit.frontpage`](https://play.google.com/store/apps/details?id=com.reddit.frontpage)
<details>

| 💊 Patch | 📜 Description | 🏹 Target Version |
|:--------:|:--------------:|:-----------------:|
| `Custom branding name reddit` | Renames the Reddit app to the name specified in options.json. | all |
| `Disable screenshot popup` | Adds an option to disable the popup that shows up when taking a screenshot. | all |
| `Hide ads` | Adds options to hide ads. | all |
| `Hide navigation buttons` | Adds options to hide buttons in the navigation bar. | all |
| `Hide recently visited shelf` | Adds an option to hide the recently visited shelf in the sidebar. | all |
| `Open links directly` | Adds an option to skip over redirection URLs in external links. | all |
| `Open links externally` | Adds an option to always open links in your browser instead of in the in-app-browser. | all |
| `Premium icon` | Unlocks premium app icons. | all |
| `Remove subreddit dialog` | Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically. | all |
| `Sanitize sharing links` | Adds an option to remove tracking query parameters from URLs when sharing links. | all |
| `Settings` | Adds ReVanced Extended settings to Reddit. | all |
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
