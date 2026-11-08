# Instructions for adding code to the project

## General working rules
- Read the relevant code and existing project conventions before making changes.
- Keep changes within the scope of the request. Do not bundle unrelated cleanup or behavior changes.
- Preserve existing user changes and avoid overwriting unrelated work.
- Do not change `pom.xml`, dependencies, persistence formats, or public behavior unless the task calls for it.
- Do not add tests during planning or cleanup work unless requested. Add and run tests during the testing phase or when specifically asked.
- Explain what changed and how it was verified. Do not claim tests were run if they were not.

## Code design and formatting
- Follow consistent Java formatting: four spaces for indentation, braces on the same line as declarations, and one statement per line. 
- Enter a new line after each statement, declaration, or block. Use blank lines to separate logical sections of code. 
- Each `throws` clause should be on its own line. 
- Each conditional statement in a multi-conditional block should be on its own line. Use braces for all blocks, even single statements.
- Each method parameter should be on its own line.
- For long expressions, break after operators and before method calls.
- Avoid long lines; wrap at 100 characters.
- Use clear, descriptive names. Keep packages lowercase and use valid Java package names.
- Keep classes focused on one responsibility. Separate domain rules, application logic, I/O, and UI responsibilities.
- Avoid static state, global variables, and singletons. Pass collaborators explicitly to constructors or methods.
- All domain objects should be immutable when possible. If mutability is required, make it explicit and document the intended usage.
- All classes should be final unless they are designed for inheritance. Avoid protected members unless they are part of a public contract.
- All non-static classes must have a private instance check<class-name> function that verifies the object is in a valid state. Call this function from the constructor and any methods that modify state.
- Use consistent class names everywhere.
- Keep JDBC, SQL, file access, and persistence details out of domain and logic classes.
- Avoid exposing mutable internal state. Domain accessors should return safe copies where appropriate.
- Prefer small, understandable methods and explicit data flow. Extract helpers when they clarify a repeated or substantial responsibility.
- Keep related behavior together, but do not create abstractions for hypothetical future needs.
- Remove unused imports and dead code when the task includes cleanup. Do not make unrelated formatting-only changes.

## Javadocs and comments
- Write concise Javadocs that describe a type or method’s purpose and public contract at a high level.
- Do not document the current algorithm, data layout, control flow, or other implementation details that may change during optimization.
- Always include `@param`, `@return`, and `@throws` annotations.
- Write simple one-line comments for all private methods.
- Write Javadocs for all public methods. For constructors, briefly describe the purpose of each parameter.
- Keep comments limited to rationale or non-obvious domain context. Do not narrate code that is already clear.
- Update comments when behavior or contracts change; do not leave comments that describe old behavior.

## Exception design and preconditions
- Use preconditions for internal invariants, programmer errors, and sanity checks that should be prevented by correct design.
- Give preconditions clear, useful messages that identify the failed condition and where it occurred. Preserve helpful existing precondition messages during exception cleanup.
- Use custom exceptions for expected failures that a caller or layer is meant to handle.
- Give each custom exception a name that identifies one specific failure context. Do not reuse one exception type for unrelated failures just to carry extra context.
- Custom exceptions must be parameterless: no message, cause, or other contextual data passed through constructors, and no extra fields.
- Do not attach extra strings to custom exceptions, directly or indirectly. Do not use exception messages as a protocol or parse them to determine the failure type; handle failures by exception type.
- Use standard exceptions when they already express the failure clearly and no project-specific handling is needed.
- Catch exceptions only where the code can handle them meaningfully. Do not swallow failures or convert programming errors into recoverable I/O errors.

## Test design
- Add tests in the testing phase or when the task explicitly requests them.
- Test externally observable behavior and contracts, including normal cases, boundary cases, and expected failure cases.
- Keep unit tests focused: each test should make one behavior clear and avoid duplicating the same assertion across multiple tests.
- Prefer tests that remain valid across implementation and optimization changes. Avoid testing private methods or incidental implementation details.
- Use representative data for performance-sensitive code. For CSV processing, include large-input validation in the testing phase while preserving the optimized production approach.
- Tests for custom exceptions should assert the exception type, not its message.
- Keep test names descriptive and test setup proportionate to the behavior being tested.
- `RunAllTests`, if used, should run each test suite once, report failures through the test framework, and contain no duplicate test logic.

## Simplicity and maintainability
- Prefer the smallest design that clearly satisfies the requirements.
- Avoid duplicate logic, redundant state, unnecessary wrappers, speculative configuration, and abstractions with only one trivial use.
- Do not add comments or tests merely to increase their count.
- Avoid broad rewrites when a focused change is enough.
- Keep formatting, naming, validation, and error handling consistent across related classes.

## SOLID principles and good practices
- Give each class a clear responsibility and keep dependencies flowing in the appropriate direction.
- Depend on stable interfaces where they simplify real substitution or testing; do not add interfaces solely to satisfy a pattern.
- Keep classes open to targeted extension without making ordinary changes harder to understand.
- Avoid coupling domain rules to UI, storage, or database implementation details.
- Keep methods and types small enough to review and audit. Make state changes explicit.
- Prefer immutable values and defensive copies for domain data.
- Avoid global mutable state and hidden dependencies. Pass required configuration and collaborators explicitly.

## Persistence and database guidance
- Keep persistence behind the I/O or persistence layer. Domain and logic classes should work with domain values, not JDBC connections or SQL.
- If SQLite with JDBC is adopted, use parameterized statements and close connections, statements, and result sets reliably.
- Keep transaction boundaries in the persistence layer. Save related matrices and their freshness metadata in one transaction so readers do not observe a partial update.
- Let SQLite provide atomic commit and rollback for grouped database changes; do not duplicate database transactions with file-manifest machinery.
- Keep schema creation or migration logic centralized and versioned. Avoid scattering table names and SQL across the application.
- Load configuration such as epsilon profiles once during application startup and pass the resulting immutable values to the classes that need them. Do not make domain operations query the database.
- If the agreed epsilon profiles are persisted, use `strict = 1e-9`, `mediocre = 1e-6`, and `loose = 1e-3`. Production code should use the agreed profile consistently rather than maintaining scattered tolerance constants.
- Preserve the existing CSV reader for patient input unless a separate task calls for changing that format.

## Review and verification
- Before finishing, review the diff for unintended files, unrelated edits, formatting problems, stale imports, and comments that expose implementation details.
- Run only the checks requested for the task or appropriate to its scope.
- Report any unverified behavior or known limitation plainly.