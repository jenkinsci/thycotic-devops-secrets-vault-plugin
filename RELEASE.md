# Release

## Setup Tools

- `aqua install` will ensure github cli, changie, and task are setup for cli invocation.
- `task` to list the [Task](https://taskfile.dev/) commands available.

## General Process

- `task bump` to generate changelog and update release notes.
- `task release` to create the draft release.

## Other

- Use changie to add entries.
- The release body, ie the results from `cat .changes/$(change latest).md` would need to match the identified "interesting categories" emojis from Jenkins. [Interesting](https://github.com/jenkins-infra/interesting-category-action/blob/main/action.yaml)
  - Since we are using the "using" workflow I can't pass this in as an override to match changie so I've updated changie config to match the items that should be interesting.
  - [List of Emoji that are looked for automatically](https://github.com/jenkins-infra/interesting-category-action/blob/78f4b74509528c18790d9c36b2cccb5b21ed3451/action.yaml#L13)
  - Value: `default: '[💥🚨🎉🐛⚠🚀🌐👷]|:(boom|tada|construction_worker):'`
