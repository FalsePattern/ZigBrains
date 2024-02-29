For now this project is still pretty small, but here are some general guidelines on how to contribute to this repository

- This project ships with a code style config in .idea, your IDE should automatically apply it when you pull the repo.
When making pull requests, please try to keep to this style as much as possible.

- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), and scope as much as you can.

- Do not edit the relationship tree between modules without unanimous agreement from the project lead.
  
  If you want an "upstream" module (for instance, the LSP) receive data from a "downstream" module
  (for instance, the project module), you should use service providers, dependency injections, or other ways to implement
  the dataflow in a way where the upstream module is not directly aware of downstream modules.
  
  The main purpose of this is to avoid any circular dependencies, which could cause proprietary IDE-only features
  (for instance, the CLion debugger module) to be depended on by FOSS modules. This restriction is non-negotiable.

- Any complex language inspection, syntax action, or similar, should be done by ZLS, and ZigBrains will just act as an
  adapter for these features. This notion of "complexity" is determined by the project maintainer. Open an issue
  that explains the feature before you start implementing anything!