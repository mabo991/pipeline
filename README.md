# pipeline

Convenience "super" Pipeline project that aggregates all sub-projects and 3rd-party libraries.

This makes branching, building, and releasing of several sub-projects at once easier.

The aggregating and the backporting of changes to the individual projects is done using a tool called [git-subrepo][]. The idea is that all the git magic will be done by the owners, and that committers can just treat this repository as a regular one. There is a rule though that committers need to follow because of some limitations of git-subrepo:

- Pull requests may not contain merge commits

Committers are of course also free to make pull requests to the individual repositories, or do other advanced things such as switching a certain sub-repository to an existing branch. Advanced git knowledge is required in these cases. Ask for help if needed.


[git-subrepo]: https://github.com/ingydotnet/git-subrepo
