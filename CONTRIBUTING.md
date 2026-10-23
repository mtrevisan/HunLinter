# Contributing

## Introduction

Thank you so much for your interest in contributing!. All types of contributions are encouraged and valued. See the [table of contents](#toc) for different ways to help and details about how this project handles them!

Please make sure to read the relevant section before making your contribution! It will make it a lot easier for us maintainers to make the most of it and smooth out the experience for all involved.

Please note we have a code of conduct, please follow it in all your interactions with the project.

The [Project Team](#join-the-project-team) looks forward to your contributions.

## Pull Request Process

1. Ensure any install or build dependencies are removed before the end of the layer when doing a 
   build.
2. Update the README.md with details of changes to the interface, this includes new environment 
   variables, exposed ports, useful file locations and container parameters.
3. Increase the version numbers in any examples files and the README.md to the new version that this
   Pull Request would represent. The versioning scheme we use is [SemVer](http://semver.org/).
4. You may merge the Pull Request in once you have the sign-off of two other developers (if there
   are more than one), or if you do not have permission to do that, you may request the second
   reviewer (or the only one) to merge it for you.

-----


<a name="toc"></a>
## How do I…

* Ask or Say Something?
  * [Request support](#request-support)
  * [Report an error or bug](#report-an-error-or-bug)
  * [Request a feature](#request-a-feature)
* Make something?
  * [Project setup](#project-setup)
  * [Contribute documentation](#contribute-documentation)
  * [Contribute code](#contribute-code)
* Manage something
  * [Provide support on issues](#provide-support-on-issues)
    * [Commit message guidelines](#commit-message-guidelines)
  * [Label issues](#label-issues)
  * [Clean up issues and PRs](#clean-up-issues-and-prs)
  * [Review Pull Requests](#review-pull-requests)
  * [Merge Pull Requests](#merge-pull-requests)
  * [Tag a release](#tag-a-release)
  * [Join the Project Team](#join-the-project-team)

<a name="request-support"></a>
## Request support
If you have a question about this project, how to use it, or just need clarification about something:
* Open an Issue at https://github.com/mtrevisan/HunLinter/issues
* Provide as much context as you can about what you're running into.
* Provide OS version, processor speed, RAM amount, etc., depending on what seems relevant. If not, please be ready to provide these information if maintainers ask for them.

Once it's filed:
* The project team will [label the issue](#label-issues).
* Someone will try to have a response soon.
* If you or the maintainers don't respond to an issue for 30 days, the [issue will be closed](#clean-up-issues-and-prs).<br>
  If you want to come back to it, reply (once, please), and we'll reopen the existing issue. Please avoid filing new issues as extensions of one you already made.

<a name="report-an-error-or-bug"></a>
## Report an error or bug
If you run into an error or bug with the project:
* Open an Issue at https://github.com/mtrevisan/HunLinter/issues
* Include *reproduction steps* that someone else can follow to recreate the bug or error on their own.
* Provide OS version, processor speed, RAM amount, etc., depending on what seems relevant. If not, please be ready to provide these information if maintainers ask for them.

Once it's filed:
* The project team will [label the issue](#label-issues).
* A team member will try to reproduce the issue with your provided steps.<br>
  If there are no reproducible steps or no obvious way to reproduce the issue, the team will ask you for those steps and mark the issue as `needs-steps`.<br>
  Bugs with the `needs-steps` tag will not be addressed until they are reproduced.
* If the team is able to reproduce the issue, it will be marked `needs-fix`, as well as possibly other tags (such as `critical`), and the issue will be left to be [implemented by someone](#contribute-code).
* If you or the maintainers don't respond to an issue for 30 days, the [issue will be closed](#clean-up-issues-and-prs).<br>
  If you want to come back to it, reply (once, please), and we'll reopen the existing issue. Please avoid filing new issues as extensions of one you already made.
* `critical` issues may be left open, depending on perceived immediacy and severity, even past the 30-day deadline.

<a name="request-a-feature"></a>
## Request a feature
If the project doesn't do something you need or want it to do:
* Open an Issue at https://github.com/mtrevisan/HunLinter/issues
* Provide as much context as you can about what you're running into.
* Please try and be clear about why existing features and alternatives would not work for you.

Once it's filed:
* The project team will [label the issue](#label-issues).
* The project team will evaluate the feature request, possibly asking you more questions to understand its purpose and any relevant requirements.
* If the issue is closed, the team will convey their reasoning and suggest an alternative path forward.
* If the feature request is accepted, it will be marked for implementation with `feature-accepted`, which can then be done by either by a core team member or by anyone in the community who wants to [contribute code](#contribute-code).

Note: The team is unlikely to be able to accept every single feature request that is filed. Please understand if they need to say no.

<a name="project-setup"></a>
## Project setup
So you want to contribute some code! That's great! This project uses GitHub Pull Requests to manage contributions, so [read up on how to fork a GitHub project and file a PR](https://guides.github.com/activities/forking) if you've never done it before.

If this seems like a lot, or you aren't able to do all this setup, you might also be able to [edit the files directly](https://help.github.com/articles/editing-files-in-another-user-s-repository/) without having to do any of this setup. Yes, [even code](#contribute-code).

If you want to go the usual route and run the project locally, [fork the project](https://guides.github.com/activities/forking/#fork) and you should be ready to go!

<a name="contribute-documentation"></a>
## Contribute documentation
Documentation is a super important, critical part of this project. Docs are how we keep track of what we're doing, how, and why. It's how we stay on the same page about our policies. And it's how we tell others everything they need in order to be able to use this project -- or contribute to it. So thank you in advance.

Documentation contributions of any size are welcome! Feel free to file a PR even if you're just rewording a sentence to be more clear, or fixing a spelling mistake!

To contribute documentation:
* [Set up the project](#project-setup).
* Edit or add any relevant documentation.
* Make sure your changes are formatted correctly and consistently with the rest of the documentation.
* Re-read what you wrote, and run a spellchecker on it to make sure you didn't miss anything.
* Write clear, concise commit message(s). Documentation commits should use `docs(<component>): <message>`.
* Go to https://github.com/mtrevisan/HunLinter/pulls and open a new pull request with your changes.
* If your PR is connected to an open issue, add a line in your PR's description that says `Fixes: #123`, where `#123` is the number of the issue you're fixing.

Once you've filed the PR:
* One or more maintainers will use GitHub's review feature to review your PR.
* If the maintainer asks for any changes, edit your changes, push, and ask for another review.
* If the maintainer decides to pass on your PR, they will thank you for the contribution and explain why they won't be accepting the changes. That's ok! We still really appreciate you taking the time to do it, and we don't take that lightly.
* If your PR gets accepted, it will be marked as such, and merged into the `latest` branch soon after. Your contribution will be distributed to the masses next time the maintainers [tag a release](#tag-a-release)

<a name="contribute-code"></a>
## Contribute code
We like code commits a lot! They're super handy, and they keep the project going and doing the work it needs to do to be useful to others.

Code contributions of just about any size are acceptable!

The main difference between code contributions and documentation contributions is that contributing code requires inclusion of relevant tests for the code being added or changed.<br>
Contributions without accompanying tests will be held off until a test is added, unless the maintainers consider the specific tests to be either impossible, or way too much of a burden for such a contribution.

To contribute code:
* [Set up the project](#project-setup).
* Make any necessary changes to the source code.
* Include any [additional documentation](#contribute-documentation) the changes might need.
* Write tests that verify that your contribution works as expected.
* Write clear, concise commit message(s).
* Dependency updates, additions, or removals must be in individual commits, and the message must declare them.
* Go to https://github.com/mtrevisan/HunLinter/pulls and open a new pull request with your changes.
* If your PR is connected to an open issue, add a line in your PR's description that says `Fixes: #123`, where `#123` is the number of the issue you're fixing.

Once you've filed the PR:
* Barring special circumstances, maintainers will not review PRs until all checks pass.
* One or more maintainers will use GitHub's review feature to review your PR.
* If the maintainer asks for any changes, edit your changes, push, and ask for another review. Additional tags (such as `needs-tests`) will be added depending on the review.
* If the maintainer decides to pass on your PR, they will thank you for the contribution and explain why they won't be accepting the changes. That's ok! We still really appreciate you taking the time to do it, and we don't take that lightly.
* If your PR gets accepted, it will be marked as such, and merged into the `latest` branch soon after. Your contribution will be distributed to the masses next time the maintainers [tag a release](#tag-a-release)

<a name="commit-message-guidelines"></a>
## Commit message guidelines
As stated [here](https://www.freecodecamp.org/news/writing-good-commit-messages-a-practical-guide/) and [here](https://reflectoring.io/meaningful-commit-messages/):

1. Specify the type of commit:

       fix: Patches a bug in the codebase (correlates with PATCH in semantic versioning).
       feat: Introduces a new feature to the codebase (correlates with MINOR in semantic versioning).
       breaking change: Introduces a breaking API change by refactoring (correlating with MAJOR in semantic versioning).
       test: Everything related to testing.
       docs: Everything related to documentation.
       refactor: Regular code maintenance.

2. Separate the subject from the body with a blank line.
   1. Try to keep the subject to be less than 50 characters.
3. Your commit message should not contain any whitespace errors.
4. Remove unnecessary punctuation marks.
5. Do not end the subject line with a period.
6. Capitalize the subject line and each paragraph.
   1. Additional paragraphs come after blank lines.
   2. Bullet points are okay, too.
7. Use the imperative mood, present tense, in the subject line (We use an imperative verb because it’s going to complete the sentence “If applied, this commit will…”).
8. Use the body to explain what changes you have made and why you made them.
   1. Wrap the body at 72 characters.
9. Do not assume the reviewer understands what the original problem was, ensure you add it.
10. If the case, add the reference to the ticket solved as the last line and separated by a blank line.

   e.g.
   ```
   Subject line (try to keep under 50 characters)
   
   Multi-line description of commit,
   feel free to be detailed. (Up to 72)
   
   [Ticket: X]```

11. Do not think your code is self-explanatory.

The most important part of a commit message is that it should be clear and meaningful.


<a name="provide-support-on-issues"></a>
## Provide support on issues
Helping out other users with their questions is a really awesome way of contributing to any community. It's not uncommon for most of the issues on an open source projects being support-related questions by users trying to understand something they ran into, or find their way around a known bug.

Sometimes, the `support` label will be added to things that turn out to actually be other things, like bugs or feature requests. In that case, suss out the details with the person who filed the original issue, add a comment explaining what the bug is, and change the label from `support` to `bug` or `feature`. If you can't do this yourself, @mention a maintainer, so they can do it.

In order to help other folks out with their questions:
* Go to the issue tracker and [filter open issues by the `support` label](https://github.com/mtrevisan/HunLinter/issues?q=is%3Aopen+is%3Aissue+label%3Asupport).
* Read through the list until you find something that you're familiar enough with to give an answer to.
* Respond to the issue with whatever details are needed to clarify the question, or get more details about what's going on.
* Once the discussion wraps up and things are clarified, either close the issue, or ask the original issue filer (or a maintainer) to close it for you.

Some notes on picking up support issues:
* Avoid responding to issue you don't know you can answer accurately.
* As much as possible, try to refer to past issues with accepted answers. Link to them from your replies with the `#123` format.
* Be kind and patient with users -- often, folks who have run into confusing things might be upset or impatient. This is ok. Try to understand where they're coming from, and if you're too uncomfortable with the tone, feel free to stay away or withdraw from the issue.<br>
  (note: if the user is outright hostile or is violating the CoC, [refer to the Code of Conduct](CODE_OF_CONDUCT.md) to resolve the conflict).

<a name="label-issues"></a>
## Label issues
One of the most important tasks in handling issues is labeling them usefully and accurately. All other tasks involving issues ultimately rely on the issue being classified in such a way that relevant parties looking to do their own tasks can find them quickly and easily.

In order to label issues, [open up the list of unlabeled issues](https://github.com/mtrevisan/HunLinter/issues?q=is%3Aopen+is%3Aissue+no%3Alabel) and, **from newest to oldest**, read through each one and apply issue labels according to the table below. If you're unsure about what label to apply, skip the issue and try the next one: don't feel obligated to label each and every issue yourself!

Label | Apply When | Notes
--- | --- | ---
`bug` | Cases where the code (or documentation) is behaving in a way it wasn't intended to. | If something is happening that surprises the *user* but does not go against the way the code is designed, it should use the `enhancement` label.
`critical` | Added to `bug` issues if the problem described makes the code completely unusable in a common situation. |
`documentation` | Added to issues or pull requests that affect any of the documentation for the project. | Can be combined with other labels, such as `bug` or `enhancement`.
`duplicate` | Added to issues or PRs that refer to the exact same issue as another one that's been previously labeled. | Duplicate issues should be marked and closed right away, with a message referencing the issue it's a duplicate of (with `#123`)
`enhancement` | Added to [feature requests](#request-a-feature), PRs, or documentation issues that are purely additive: the code or docs currently work as expected, but a change is being requested or suggested. |
`help wanted` | Applied by [Committers](#join-the-project-team) to issues and PRs that they would like to get outside help for. Generally, this means it's lower priority for the maintainer team to itself implement, but that the community is encouraged to pick up if they so desire | Never applied on first-pass labeling.
`in-progress` | Applied by [Committers](#join-the-project-team) to PRs that are pending some work before they're ready for review. | The original PR submitter should @mention the team member that applied the label once the PR is complete.
`performance` | This issue or PR is directly related to improving performance. |
`refactor` | Added to issues or PRs that deal with cleaning up or modifying the project for the betterment of it. |
`starter` | Applied by [Committers](#join-the-project-team) to issues that they consider good introductions to the project for people who have not contributed before. These are not necessarily "easy", but rather focused around how much context is necessary in order to understand what needs to be done for this project in particular. | Existing project members are expected to stay away from these unless they increase in priority.
`support` | This issue is either asking a question about how to use the project, clarifying the reason for unexpected behavior, or possibly reporting a `bug` but does not have enough detail yet to determine whether it would count as such. | The label should be switched to `bug` if reliable reproduction steps are provided. Issues primarily with unintended configurations of a user's environment are not considered bugs, even if they cause crashes.
`tests` | This issue or PR either requests or adds primarily tests to the project. | If a PR is pending tests, that will be handled through the [PR review process](#review-pull-requests)
`wontfix` | Labelers may apply this label to issues that clearly have nothing at all to do with the project or are otherwise entirely outside of its scope/sphere of influence. [Committers](#join-the-project-team) may apply this label and close an issue or PR if they decide to pass on an otherwise relevant issue. | The issue or PR should be closed as soon as the label is applied, and a clear explanation provided of why the label was used. Contributors are free to contest the labeling, but the decision ultimately falls on committers as to whether to accept something or not.

<a name="clean-up-issues-and-prs"></a>
## Clean up issues and PRs
Issues and PRs can go stale after a while. Maybe they're abandoned. Maybe the team will just plain not have time to address them any time soon.

In these cases, they should be closed until they're brought up again or the interaction starts over.

To clean up issues and PRs:
* Search the issue tracker for issues or PRs, and add the term `updated:<=YYYY-MM-DD`, where the date is 30 days before today.
* Go through each issue *from oldest to newest*, and close them if **all the following are true**:
  * not opened by a maintainer
  * not marked as `critical`
  * not marked as `starter` or `help wanted` (these might stick around for a while, in general, as they're intended to be available)
  * no explicit messages in the comments asking for it to be left open
  * does not belong to a milestone
* Leave a message when closing saying "Cleaning up stale issue. Please reopen or ping us if and when you're ready to resume this. See https://github.com/mtrevisan/HunLinter/blob/latest/CONTRIBUTING.md#clean-up-issues-and-prs for more details."

<a name="review-pull-requests"></a>
## Review Pull Requests
While anyone can comment on a PR, add feedback, etc., PRs are only *approved* by team members with Issue Tracker or higher permissions.

PR reviews use [GitHub's own review feature](https://help.github.com/articles/about-pull-request-reviews/), which manages comments, approval, and review iteration.

Some notes:
* You may ask for minor changes ("nitpicks"), but consider whether they are really blockers to merging: try to err on the side of "approve, with comments".
* *ALL PULL REQUESTS* should be covered by a test: either by a previously-failing test, an existing test that covers the entire functionality of the submitted code, or new tests to verify any new/changed behavior.<br>
  All tests must also pass and follow established conventions. Test coverage should not drop, unless the specific case is considered reasonable by maintainers.
* Please make sure you're familiar with the code or documentation being updated, unless it's a minor change (spellchecking, minor formatting, etc.).<br>
  You may @mention another project member who you think is better suited for the review, but still provide a non-approving review of your own.
* Be extra kind: people who submit code/doc contributions are putting themselves in a pretty vulnerable position, and have put time and care into what they've done (even if that's not obvious to you!) -- always respond with respect, be understanding, but don't feel like you need to sacrifice your standards for their sake, either.

<a name="merge-pull-requests"></a>
## Merge Pull Requests
TBD - need to hash out a bit more of this process.

<a name="tag-a-release"></a>
## Tag a release
TBD - need to hash out a bit more of this process.<br>
The most important bit here is probably that all tests must pass, and tags must use [semver](https://semver.org).

<a name="join-the-project-team"></a>
## Join the Project Team
There are many ways to contribute! Most of them don't require any official status unless otherwise noted. That said, there's a couple of positions that grant special repository abilities, and this section describes how they're granted and what they do.

All the below positions are granted based on the project team's needs, as well as their consensus opinion about whether they would like to work with the person and think that they would fit well into that position. The process is relatively informal, and it's likely that people who express interest in participating can just be granted the permissions they'd like.

You can spot a collaborator on the repo by looking for the `[Collaborator]` or `[Owner]` tags next to their names.

Permission | Description
--- | ---
Issue Tracker | Granted to contributors who express a strong interest in spending time on the project's issue tracker. These tasks are mainly [labeling issues](#label-issues), [cleaning up old ones](#clean-up-issues-and-prs), and [reviewing pull requests](#review-pull-requests), as well as all the usual things non-team-member contributors can do. Issue handlers should not merge pull requests, tag releases, or directly commit code themselves: that should still be done through the usual pull request process. Becoming an Issue Handler means the project team trusts you to understand enough of the team's process and context to implement it on the issue tracker.
Committer | Granted to contributors who want to handle the actual pull request merges, tagging new versions, etc. Committers should have a good level of familiarity with the codebase, and enough context to understand the implications of various changes, as well as a good sense of the will and expectations of the project team.
Admin/Owner | Granted to people ultimately responsible for the project, its community, etc.
