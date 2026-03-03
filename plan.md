# Instructions

Review this plan thoroughly before making any code changes. For every issue or recommendation, explain the concrete tradeoffs, give me an opinionated recommendation, and ask for my input before assuming a direction.

My engineering preferences (use these to guide your recommendations):

    DRY is important—flag repetition aggressively.
    Well-tested code is non-negotiable; I'd rather have too many tests than too few.
    I want code that's "engineered enough" — not under-engineered (fragile, hacky) and not over-engineered (premature abstraction, unnecessary complexity).
    I err on the side of handling more edge cases, not fewer; thoughtfulness > speed.
    Bias toward explicit over clever.

1. Architecture review Evaluate:

    Overall system design and component boundaries.
    Dependency graph and coupling concerns.
    Data flow patterns and potential bottlenecks.
    Scaling characteristics and single points of failure.
    Security architecture (auth, data access, API boundaries).

2. Code quality review Evaluate:

    Code organization and module structure.
    DRY violations—be aggressive here.
    Error handling patterns and missing edge cases (call these out explicitly).
    Technical debt hotspots.
    Areas that are over-engineered or under-engineered relative to my preferences.

3. Test review Evaluate:

    Test coverage gaps (unit, integration, e2e).
    Test quality and assertion strength.
    Missing edge case coverage—be thorough.
    Untested failure modes and error paths.

4. Performance review Evaluate:

    N+1 queries and database access patterns.
    Memory-usage concerns.
    Caching opportunities.
    Slow or high-complexity code paths.

For each issue you find For every specific issue (bug, smell, design concern, or risk):

    Describe the problem concretely, with file and line references.
    Present 2–3 options, including "do nothing" where that's reasonable.
    For each option, specify: implementation effort, risk, impact on other code, and maintenance burden.
    Give me your recommended option and why, mapped to my preferences above.
    Then explicitly ask whether I agree or want to choose a different direction before proceeding.

Workflow and interaction

    Do not assume my priorities on timeline or scale.
    After each section, pause and ask for my feedback before moving on.

BEFORE YOU START: Ask if I want one of two options: 1/ BIG CHANGE: Work through this interactively, one section at a time (Architecture → Code Quality → Tests → Performance) with at most 4 top issues in each section. 2/ SMALL CHANGE: Work through interactively ONE question per review section

FOR EACH STAGE OF REVIEW: output the explanation and pros and cons of each stage's questions AND your opinionated recommendation and why, and then use AskUserQuestion. Also NUMBER issues and then give LETTERS for options and when using AskUserQuestion make sure each option clearly labels the issue NUMBER and option LETTER so the user doesn't get confused. Make the recommended option always the 1st option.

# Plan

Create a plan to star a UI framework for my purposes.

Its should be similar to https://oat.ink/

It will be used in my clojure / clojurescript / squint apps.

So we need to make sure to have components that are abstract, I'm also fine with duplication of the varions components as only LLMs will modify it.

We can write out the components using clojure reader tags like

```cljc
   #?(:cljs (js/console.log x)
      :clj (println x))
```

So we need component levels for 

- Squint (/home/floscr/Code/My/ressources/skills/squint/SKILL.md)
- Clojurescript with replicant 
- Pure hiccup components from a backend that get targeted via something like HTMX or vanilla JS (no interaction needed, just styles)

The components should have a themable system that gets defined via edn but transforms to css variables.
- So we should easily have dark / light mode.
- Multiple levlels of strokes
- Multiple levels of shadows
- Multiple levels of backgrounds

Here are some examples where I want to use these components

- ~/Code/Projects/org-mode-agenda-cli/app/collab-v2/src/
  (Replicant cljs)
- /home/floscr/Code/Projects/piui/ 
  (Replicant cljs with 
  - With /home/floscr/Code/Projects/piui/apps/
    Apps are written in (squint + eucalypt)
- /home/floscr/Code/Projects/org-mode-agenda-cli/app/calendar
  (squint eucalypt)
  
As a first step we could do simple button component that works for all the targets.
Ideally we have a test page where we can tab between the targets.

Something like https://github.com/cjohansen/portfolio as a storybook alternative would be awesome but we can handroll and keep it simple.
