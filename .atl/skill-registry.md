# Skill Registry

**Delegator use only.** Any agent that launches sub-agents reads this registry to resolve compact rules, then injects them directly into sub-agent prompts. Sub-agents do NOT read this registry or individual SKILL.md files.

See `_shared/skill-resolver.md` for the full resolution protocol.

## User Skills

| Trigger | Skill | Path |
|---------|-------|------|
| User asks to find or install a skill, or asks whether a skill exists for a capability | find-skills | C:/Users/Sir Nolimit/IdeaProjects/musicon/.agents/skills/find-skills/SKILL.md |
| User asks to build or beautify a web UI, page, component, dashboard, or visual artifact | frontend-design | C:/Users/Sir Nolimit/IdeaProjects/musicon/.agents/skills/frontend-design/SKILL.md |
| Cuando se menciona SEO, SEM, indexación, Search Console, Analytics, sitemap, structured data o rendimiento orgánico | seo-sem | C:/Users/Sir Nolimit/IdeaProjects/musicon/.agents/skills/seo-sem/SKILL.md |
| User asks for caveman mode, less tokens, or very brief replies | caveman | C:/Users/Sir Nolimit/.config/opencode/skills/caveman/SKILL.md |
| When creating a pull request, opening a PR, or preparing changes for review | branch-pr | C:/Users/Sir Nolimit/.config/opencode/skills/branch-pr/SKILL.md |
| When writing Go tests, using teatest, or adding test coverage | go-testing | C:/Users/Sir Nolimit/.config/opencode/skills/go-testing/SKILL.md |
| When creating a GitHub issue, reporting a bug, or requesting a feature | issue-creation | C:/Users/Sir Nolimit/.config/opencode/skills/issue-creation/SKILL.md |
| When user says judgment day / dual review / adversarial review | judgment-day | C:/Users/Sir Nolimit/.config/opencode/skills/judgment-day/SKILL.md |
| When user asks to create a new skill or document reusable AI patterns | skill-creator | C:/Users/Sir Nolimit/.config/opencode/skills/skill-creator/SKILL.md |

## Compact Rules

Pre-digested rules per skill. Delegators copy matching blocks into sub-agent prompts as `## Project Standards (auto-resolved)`.

### find-skills
- Check the skills.sh leaderboard before CLI search.
- Use `npx skills find <query>` with specific domain keywords.
- Verify quality before recommending: installs, source reputation, GitHub stars.
- Present skill name, what it does, source/install count, install command, and skills.sh link.
- If no match exists, say so, help directly, and suggest `npx skills init` for a custom skill.

### frontend-design
- Choose a bold, explicit aesthetic direction before coding.
- Build production-grade UI, not mockups; code must work.
- Avoid generic AI aesthetics: no bland fonts, cliché purple gradients, or cookie-cutter layouts.
- Use strong typography, cohesive color variables, memorable motion, and intentional spatial composition.
- Match implementation complexity to the visual concept: maximalism needs rich effects; minimalism needs precision.

### seo-sem
- Public pages under `/eventos/**`, `/baja/**`, and `robots.txt` must stay stateless and without CSRF.
- Never use `header-css` on public templates; use `header-css-public`.
- Public templates must include title, description, robots, canonical, OG tags, and JSON-LD.
- Filter/pagination/empty listings should use `noindex,follow` to avoid duplicate indexing.
- Validate sitemap, robots.txt, canonical URLs, and structured data before shipping SEO-critical changes.

### caveman
- Respond tersely; remove filler, hedging, and pleasantries.
- Keep technical terms exact; code blocks remain unchanged.
- Default intensity is full unless user asks for another level.
- Drop caveman mode for safety warnings or steps where clarity matters more than compression.
- Resume terse mode after the critical clarification.

### branch-pr
- Every PR must link an approved issue and have exactly one `type:*` label.
- Branch names must match `type/description` with lowercase `a-z0-9._-` only.
- Use conventional commits only; no AI attribution or co-author trailers.
- PR body must include linked issue, summary bullets, file changes table, and test plan.
- Shell scripts changed in the PR must pass shellcheck before merge.

### go-testing
- Prefer table-driven tests for pure logic and multi-case coverage.
- Test Bubbletea state transitions directly via `Model.Update()`.
- Use teatest for interactive TUI flows and golden files for view output.
- Mock side effects and test both success and error paths.
- Use `t.TempDir()` for filesystem cases and skip heavy integration paths under `--short` when needed.

### issue-creation
- Never create blank issues; always use the repo template.
- Search for duplicates before opening a new issue.
- New issues get `status:needs-review`; implementation waits for `status:approved`.
- Questions belong in Discussions, not Issues.
- Fill all required fields and use conventional-commit-style titles.

### judgment-day
- Resolve relevant project skills from the registry before launching judges.
- Run two blind judges in parallel with the same scope and standards.
- Classify warnings as real vs theoretical; theoretical warnings are INFO only.
- Fix only confirmed issues, then re-judge; ask the user before the first fix round.
- After two fix iterations, escalate to the user if confirmed issues remain.

### skill-creator
- Create a skill only for reusable, non-trivial patterns or workflows.
- Use the standard `skills/{skill-name}/SKILL.md` structure with complete frontmatter.
- Put critical patterns first; keep examples minimal and focused.
- Prefer local `references/` over duplicating documentation or linking web URLs.
- Register the new skill in AGENTS.md after creation.

## Project Conventions

| File | Path | Notes |
|------|------|-------|
| — | — | No supported root convention files detected (`AGENTS.md`, `agents.md`, `CLAUDE.md`, `.cursorrules`, `GEMINI.md`, `copilot-instructions.md`). |

Read the convention files listed above for project-specific patterns and rules. All referenced paths have been extracted — no need to read index files to discover more.


