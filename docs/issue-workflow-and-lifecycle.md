# Issue WorkFlow and LifeCycle

This page describes the process flow for working on issues (AKA bug reports)

- we receive an issue
- developer & triage team looks at issue, adds labels
- if developer chooses to work on issue, they refresh page and assign issue to themself
- developers works on the issue

  **Happy Path - Developer Finds a Fix:**
    - tries to reproduce the problem locally
    - hypothesizes about problem and applies a fix
    - confirms fix manually
    - attempts to build a unit test to confirm bug and fix
    - opens PR
    - comments on issue that PR to fix issue is open
    - if completely confident in fix, closes issue before merge
    - if not confident, confirms fix after merge, then repeats this process if not fixed or closes the issue if fixed.

  **Less Happy Path - Developer does NOT Find a Fix:**
    - developer documents any findings/discoveries
    - developer unassigns the issue (and someone can then pick it up or they return to it repeating this process)

