---
title: The page format
description: How a UAT page is laid out.
---

Every UAT page is a Markdown file under `src/content/docs/features/`, one per feature or slice. The
sidebar lists it automatically. Set `sidebar.order` in the front matter to keep slices in order.

## Layout

1. **Front matter**: a `title` naming the feature and slice, and a one-line `description`.
2. **What it tests**: the commit hash and branch, plus one sentence on what the change does for the user.
3. **Setup**: how to build and launch that commit, and any song file, settings or device needed first.
4. **Steps**: a table with one row per step. **Do** is the action. **Expect** is what the user should see
   or hear when the change works. **Result** starts as `not run`, and the tester changes it to `passed`
   or `failed`.
5. **Notes**: optional. Known defects the tester should not report, and anything the tests already
   cover, so it is not checked twice.

## Skeleton

```md
---
title: Interleaved practice, slice 3a
description: A live practice session ends on a move or an edit.
sidebar:
  order: 3
---

## What it tests

Commit `1c3b50bd1` on `feature/interleaved-practice`. Moving the play position or editing
the song during a live practice session ends the session.

## Setup

Build and launch with the VS Code task **app: build + run**. Open a song with at least
two flagged measures.

## Steps

| #   | Do                                           | Expect                                         | Result  |
| --- | -------------------------------------------- | ---------------------------------------------- | ------- |
| 1   | Start a practice session.                    | A Count In plays, then the first turn.         | not run |
| 2   | Click a measure outside the turn mid-turn.   | The session ends; the song plays on from there. | not run |

## Notes

- Known defect: with a VST3 instrument, the note held at the move rings on.
```
