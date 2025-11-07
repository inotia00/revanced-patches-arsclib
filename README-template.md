## 🧩 ReVanced Extended Patches ARSCLib

ReVanced Extended Patches ARSCLib. 

See the [documentation](https://github.com/inotia00/revanced-documentation#readme) to learn how to apply patches and build ReVanced Extended apps.

## 📋 List of patches in this repository

{{ table }}

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
      "com.reddit.frontpage": "COMPATIBLE_PACKAGE_REDDIT"
    }
  }
]
```