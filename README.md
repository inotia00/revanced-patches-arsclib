## 🧩 ReVanced Extended Patches ARSCLib

ReVanced Extended Patches ARSCLib. 

See the [documentation](https://github.com/inotia00/revanced-documentation#readme) to learn how to apply patches and build ReVanced Extended apps.

## 📋 List of patches in this repository

### [📦 `com.reddit.frontpage`](https://play.google.com/store/apps/details?id=com.reddit.frontpage)
<details>

| 💊 Patch | 📜 Description | 🏹 Target Version |
|:--------:|:--------------:|:-----------------:|
| `Hide ads` | Adds options to hide ads. | 2025.40.0 ~ 2025.52.0 |
| `Custom branding name for Reddit` | Renames the Reddit app to the name specified in options.json. | 2025.40.0 ~ 2025.52.0 |
| `Hide recommended communities shelf` | Adds an option to hide the recommended communities shelves in subreddits. | 2025.40.0 ~ 2025.52.0 |
| `Hide navigation buttons` | Adds options to hide buttons in the navigation bar. | 2025.40.0 ~ 2025.52.0 |
| `Disable screenshot popup` | Adds an option to disable the popup that appears when taking a screenshot. | 2025.40.0 ~ 2025.52.0 |
| `Hide sidebar components` | Adds options to hide the sidebar components. | 2025.40.0 ~ 2025.52.0 |
| `Remove subreddit dialog` | Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically. | 2025.40.0 ~ 2025.52.0 |
| `Hide Trending Today shelf` | Adds an option to hide the Trending Today shelf from search suggestions. | 2025.40.0 ~ 2025.52.0 |
| `Spoof signature` | Spoofs the signature of the app. | 2025.40.0 ~ 2025.52.0 |
| `Open links directly` | Adds an option to skip over redirection URLs in external links. | 2025.40.0 ~ 2025.52.0 |
| `Open links externally` | Adds an option to always open links in your browser instead of in the in-app-browser. | 2025.40.0 ~ 2025.52.0 |
| `Sanitize sharing links` | Adds an option to remove tracking query parameters from URLs when sharing links. | 2025.40.0 ~ 2025.52.0 |
| `Settings for Reddit` | Applies mandatory patches to implement RVX settings into the application. | 2025.40.0 ~ 2025.52.0 |
</details>



## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

Example:

```json
[
  {
    "name": "Hide ads",
    "description": "Adds options to hide ads.",
    "excluded":false,
    "options": [],
    "dependencies": [
      "Settings for Reddit",
      "CommentAdsPatch"
    ],
    "compatiblePackages": {
      "com.reddit.frontpage": [
        "2025.40.0",
        "2025.43.0",
        "2025.45.0",
        "2025.52.0"
      ]
    }
  }
]
```