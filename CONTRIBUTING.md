For now this project is still pretty small, but here are some general guidelines on how to contribute to this repository

- This project ships with a code style config in .idea, your IDE should automatically apply it when you pull the repo.
When making pull requests, please try to keep to this style as much as possible.
- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), and scope as much as you can.
- The project has support for the PSI tree, but try to keep "intelligent" behaviour out of it, all code inspection and
other semantics-aware help should come from ZLS where possible.
  - Generally, if the answer to "Can i write some glue code that can get this info from ZLS?" is YES, do not use the PSI
  tree.