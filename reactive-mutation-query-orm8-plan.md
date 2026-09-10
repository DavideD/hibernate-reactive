# Plan: Fix ReactiveMutationQueryImpl for Hibernate ORM 8.0

## Overview

The `wip-5.0-bob` branch of `DavideD/hibernate-reactive` targets Hibernate ORM 8.0
from `DavideD/hibernate-orm:hr-orm-8.0-changes`. That branch already contains
`ReactiveMutationQueryImpl` and the correct class hierarchy — the class extends ORM 8.0's
`SqmMutationQueryImpl` and implements `ReactiveMutationQuery<R>`. However, the class
(and potentially adjacent files) has compilation errors against ORM 8.0's API.

The ORM 8.0 API changes relative to 7.x that affect reactive mutation queries include:
- `SqmQueryImpl` (which previously combined selection + mutation) is split into
  `SqmSelectionQueryImpl` (selection) and `SqmMutationQueryImpl` (mutation)
- Method signatures, constructor parameters, or internal API access points may differ
- The `localHibernateOrmPath` in `gradle.properties` must point to the local
  `DavideD/hibernate-orm` clone on the `hr-orm-8.0-changes` branch

The approach: check out the branch, configure the ORM dependency, compile once to
capture every error, then resolve each error in a dedicated sub-task so changes are
focused and reviewable.

---

## Sub-Tasks

---

### Sub-Task 1 — Check out `wip-5.0-bob`, configure ORM 8.0 dependency, capture all compilation errors

**Status:** `[ ] pending`

**Intent:**
Switch to the correct branch and run a compilation pass to produce the definitive error
list. The error list drives all subsequent sub-tasks.

**Expected Outcomes:**
- Working tree is on `wip-5.0-bob` (commit `6057c3ac`).
- `gradle.properties` has `localHibernateOrmPath = ../hibernate-orm` pointing to the
  local `DavideD/hibernate-orm` clone checked out on `hr-orm-8.0-changes`.
- `./gradlew :hibernate-reactive-core:compileJava` runs and produces a captured error log.
- The error log is appended to this plan file under "## Compilation Error Log".

**Todo List:**
1. Run `git checkout origin/wip-5.0-bob` (or `git checkout wip-5.0-bob`) to switch branch.
2. Confirm `../hibernate-orm` exists and is on branch `hr-orm-8.0-changes`; if not,
   clone `DavideD/hibernate-orm` and `git checkout hr-orm-8.0-changes` in that repo.
3. Uncomment/set `localHibernateOrmPath = ../hibernate-orm` in `gradle.properties`.
4. Run `./gradlew :hibernate-reactive-core:compileJava 2>&1 | tee /tmp/compile-errors.txt`.
5. Read `/tmp/compile-errors.txt` and append the full content to this plan file under
   "## Compilation Error Log".
6. Review the error list and update Sub-Tasks 2–N below to reflect the real errors found.

**Relevant Context:**
- Branch SHA in local pack: `6057c3ac4330778c62cef779f3f8f2e83078d76b`
  (`refs/remotes/origin/wip-5.0-bob` in `.git/packed-refs`)
- ORM dependency configuration: `gradle.properties` (`localHibernateOrmPath`),
  `gradle/libs.versions.toml` (`hibernateOrmVersion`)
- Build script: `hibernate-reactive-core/build.gradle`

---

### Sub-Task 2 — Fix compilation error #1 in `ReactiveMutationQueryImpl`

**Status:** `[ ] pending`

**Intent:**
Resolve the first compilation error in `ReactiveMutationQueryImpl` as identified in the
error log. Each error is addressed in isolation so the cause and fix are clearly traceable.

**Expected Outcomes:**
- The specific error from error log entry #1 no longer appears in a subsequent compile run.
- No new errors are introduced by the fix.

**Todo List:**
1. Read the error log entry: note the file, line number, and error message.
2. Read the relevant section of `ReactiveMutationQueryImpl.java`.
3. Identify the ORM 8.0 API that changed (e.g., method renamed, removed, signature
   changed, class moved).
4. Apply the minimal fix: rename the method call, update the import, adjust the type, etc.
5. Re-run `./gradlew :hibernate-reactive-core:compileJava` to confirm the error is gone.

**Relevant Context:**
- To be filled in after Sub-Task 1 captures the error log.
- Key files: `ReactiveMutationQueryImpl.java` (location confirmed after checkout)
- Likely ORM 8.0 changes affecting mutations: `SqmMutationQueryImpl` constructor
  signatures, `verifyUpdate()`, `beforeQuery()` / `afterQuery()`, `getDomainParameterXref()`,
  `SqmInterpretationsKey.generateNonSelectKey()`, `checkTransactionNeededForUpdateOperation()`

---

### Sub-Task 3 — Fix compilation error #2 in `ReactiveMutationQueryImpl`

**Status:** `[ ] pending`

**Intent:**
Resolve the second distinct compilation error in `ReactiveMutationQueryImpl`.

**Expected Outcomes:**
- Error #2 from the log is eliminated.
- No regressions.

**Todo List:**
1. Read error log entry #2.
2. Identify the changed ORM 8.0 API.
3. Apply the minimal fix.
4. Verify with a compile run.

**Relevant Context:**
- To be filled in after Sub-Task 1.

---

### Sub-Task 4 — Fix compilation error #3 in `ReactiveMutationQueryImpl`

**Status:** `[ ] pending`

**Intent:**
Resolve the third distinct compilation error in `ReactiveMutationQueryImpl`.

**Expected Outcomes:**
- Error #3 from the log is eliminated.
- No regressions.

**Todo List:**
1. Read error log entry #3.
2. Identify the changed ORM 8.0 API.
3. Apply the minimal fix.
4. Verify with a compile run.

**Relevant Context:**
- To be filled in after Sub-Task 1.

---

### Sub-Task 5 — Fix compilation error #4 in `ReactiveMutationQueryImpl`

**Status:** `[ ] pending`

**Intent:**
Resolve the fourth distinct compilation error in `ReactiveMutationQueryImpl`.

**Expected Outcomes:**
- Error #4 from the log is eliminated.
- No regressions.

**Todo List:**
1. Read error log entry #4.
2. Identify the changed ORM 8.0 API.
3. Apply the minimal fix.
4. Verify with a compile run.

**Relevant Context:**
- To be filled in after Sub-Task 1.

---

### Sub-Task 6 — Fix compilation error #5 in `ReactiveMutationQueryImpl`

**Status:** `[ ] pending`

**Intent:**
Resolve the fifth distinct compilation error in `ReactiveMutationQueryImpl`.

**Expected Outcomes:**
- Error #5 from the log is eliminated.
- No regressions.

**Todo List:**
1. Read error log entry #5.
2. Identify the changed ORM 8.0 API.
3. Apply the minimal fix.
4. Verify with a compile run.

**Relevant Context:**
- To be filled in after Sub-Task 1.

---

### Sub-Task 7 — Fix any remaining errors in `ReactiveMutationQueryImpl` and adjacent files

**Status:** `[ ] pending`

**Intent:**
After resolving the primary errors in `ReactiveMutationQueryImpl`, there may be
remaining errors in files that interact with it or that are independently broken by
ORM 8.0 API changes. This sub-task achieves a clean full compile.

**Expected Outcomes:**
- `./gradlew :hibernate-reactive-core:compileJava` completes with zero errors.
- No new warnings introduced by the changes in this branch.

**Todo List:**
1. Run `./gradlew :hibernate-reactive-core:compileJava` and capture any remaining errors.
2. For each remaining error file, read the relevant section and the changed ORM 8.0 API.
3. Apply the minimal fix for each (method rename, import correction, type adjustment).
4. Repeat compile-fix cycles until zero errors.

**Relevant Context:**
- Files adjacent to `ReactiveMutationQueryImpl` that may be affected:
  - `ReactiveSqmQueryImpl.java` — may have had mutation methods removed or its
    `SqmQueryImpl` base narrowed
  - `ReactiveSessionImpl.java` — session methods that create mutation queries
  - `ReactiveNativeQueryImpl.java` — native mutation query implementation
  - `StageMutationQueryImpl.java`, `MutinyMutationQueryImpl.java` — public API adapters
  - Non-select query plan classes under `query/sqm/internal/`
- Pattern reference: `ReactiveSqmSelectionQueryImpl` and `ReactiveSqmQueryImpl` in the
  same package show how ORM 7.x base classes were wrapped

---

### Sub-Task 8 — Run tests and confirm correct behavior

**Status:** `[ ] pending`

**Intent:**
Verify that mutation queries work correctly at runtime, not just at compile time.

**Expected Outcomes:**
- All existing mutation query tests pass.
- No regressions in selection query tests.

**Todo List:**
1. Run `./gradlew :hibernate-reactive-core:test -Pdb=PostgreSQL` (or the project's
   standard test invocation).
2. For any failing test, read the stack trace and identify whether it is a logic error
   in `ReactiveMutationQueryImpl` or a missing ORM 8.0 adaptation.
3. Fix each failure with a minimal targeted change.
4. Re-run until the test suite is green.

**Relevant Context:**
- Mutation-related tests: search under `hibernate-reactive-core/src/test/java` for
  `createReactiveMutationQuery`, `executeReactiveUpdate`, `MutationQuery`.
- Test infrastructure uses Testcontainers; a running database is required.

---

## Compilation Error Log

### Initial compile run (Sub-Task 1)

**Command:** `./gradlew :hibernate-reactive-core:compileJava`

**Result:** `BUILD FAILED — 1 error`

```
ReactiveMutationQueryImpl.java:55: error: ReactiveMutationQueryImpl is not abstract
and does not override abstract method
<P>setParameterList(QueryParameter<P>,P[],Type<P>) in ReactiveSqmQueryImplementor
```

The compiler reported one error but the real underlying problem was that **all**
`setParameter` and `setParameterList` covariant overrides were missing from
`ReactiveMutationQueryImpl`, along with several other methods required by the
`ReactiveSqmQueryImplementor` → `ReactiveQueryImplementor` → `ReactiveQuery` →
`ReactiveSelectionQuery` interface chain. Java stops at the first unresolved abstract
method, so the full set of missing methods was only revealed by iterative compilation.

### Full set of missing methods discovered (Sub-Tasks 2–7 combined)

The following were all missing from `ReactiveMutationQueryImpl`:

1. All `setParameter(...)` overloads (name, position, QueryParameter, Parameter — all variants
   with `Instant`, `Calendar`, `Date`, `Class<P>`, `Type<P>`)
2. All `setParameterList(...)` overloads (name, position, QueryParameter — all variants)
3. `setCacheMode(CacheMode)`
4. `setCacheRetrieveMode(CacheRetrieveMode)`
5. `setCacheStoreMode(CacheStoreMode)`
6. `setFlushMode(FlushModeType)`
7. `setLockMode(LockModeType)` (covariant)
8. `setTupleTransformer(TupleTransformer<T>)` — return type must be `ReactiveMutationQueryImpl<T>`
9. `setResultListTransformer(ResultListTransformer<R>)`
10. `setLockOptions(LockOptions)` — return type narrowed to `ReactiveMutationQueryImpl<R>`
11. `setLockMode(String, LockMode)` — return type narrowed to `ReactiveMutationQueryImpl<R>`
12. `enableFetchProfile(String)`
13. `disableFetchProfile(String)`
14. `setQueryPlanCacheable(boolean)`
15. `getHibernateLockMode()` → returns `LockMode.NONE`
16. `getCacheMode()` → returns `null`
17. `getCacheStoreMode()` → returns `null`
18. `getCacheRetrieveMode()` → returns `null`
19. `getFetchSize()` → returns `null`
20. `isReadOnly()` → returns `false`
21. `isCacheable()` → returns `false`
22. `isQueryPlanCacheable()` → returns `false`
23. `getFirstResult()` → returns `0`
24. `getMaxResults()` → returns `Integer.MAX_VALUE`

### Final result

`BUILD SUCCESSFUL` — zero errors.

---

## Notes

- Sub-Tasks 2–6 are placeholder slots for the first 5 individual errors found in Sub-Task 1.
  Once the error log is known, update each sub-task with the specific error details.
- If fewer than 5 errors are found, mark unused sub-tasks as `N/A` and merge them into
  Sub-Task 7.
- If more than 5 individual errors are found in `ReactiveMutationQueryImpl` alone, add
  additional sub-tasks before Sub-Task 7.
- `ReactiveStatelessSession` is explicitly out of scope for this work; it will be
  addressed in a follow-up after this plan is complete.
