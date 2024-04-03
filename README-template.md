## 🧩 ReVanced Patches

ReVanced Extended Patches.

## 📋 List of patches in this repository

{{ table }}

## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

Example:

```json
[
  {
    "name": "Default video quality",
    "description": "Adds an option to set the default video quality.",
    "compatiblePackages":[
      {
        "name":"com.google.android.youtube",
        "versions": COMPATIBLE_PACKAGE_YOUTUBE
      }
    ],
    "use":true,
    "requiresIntegrations":false,
    "options": []
  },
  {
    "name": "Remember video quality",
    "description": "Adds an option to remember the last video quality selected.",
    "compatiblePackages": [
      {
        "name": "com.google.android.apps.youtube.music",
        "versions": COMPATIBLE_PACKAGE_MUSIC
      }
    ],
    "use":true,
    "requiresIntegrations":false,
    "options": []
  },
  {
    "name": "Hide ads",
    "description": "Adds options to hide ads.",
    "compatiblePackages": [
      {
        "name": "com.reddit.frontpage",
        "versions": COMPATIBLE_PACKAGE_REDDIT
      }
    ],
    "use":true,
    "requiresIntegrations":true,
    "options": []
  }
]
```