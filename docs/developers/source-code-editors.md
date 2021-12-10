Please note that currently, the source code does not contain any configuration files for editors or IDEs. We added some name patterns of such files to `.gitignore` to prevent accidental commits of environment-specific settings and potential conflicts. This document, however, contains some recommendations and patterns you may want to use when editing the source code.

We _recommend_ to use [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download) for editing the backend and [Visual Studio Code](https://code.visualstudio.com) for the rest of the code.

## VSCode multi-root workspaces

It may be convenient to have a single [multi-root workspace](https://code.visualstudio.com/docs/editor/multi-root-workspaces) in VSCode.

For that, create a single file called `rdx.code-workspace` in the root of the source code with the following content:

```
{
  "folders": [
    {
      "name": "root",
      "path": "."
    },
    {
      "name": "Frontend",
      "path": "frontend"
    },
    {
      "name": "Deployment Scripts",
      "path": "deploy"
    },
    {
      "name": "Documentation",
      "path": "docs"
    }
  ],
  "settings": {
    "editor.defaultFormatter": "esbenp.prettier-vscode",
    "files.exclude": {
      "**/backend": true,
      "**/frontend": true,
      "**/deploy": true,
      "**/docs": true
    }
  }
}
```

This will allow you to have all source code (except backend) loaded into a single VSCode window. Because it's only possible to add folders to such workspace (see [this issue](https://github.com/microsoft/vscode/issues/45177)), we need the trick with "root" folder.

### Why not open each module in a separate VSCode session?

You can, but dependent on what you want to do, you may need to do a lot of switching, and it will still be problematic to edit files like `.gitlab-ci.yml` and `README.md` because you'll need to either open them separately or open the root folder as a single workspace (read further).

### Why not just open the root folder as a single workspace?

It can work, but it's not easy to do workspace-specific configuration then, and you still have to exclude the backend if you want to edit it in IntelliJ.
