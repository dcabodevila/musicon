# Skill Registry — musicon

## Project conventions
- `CLAUDE.md` — build/run, architecture, env and workflow guidance.
- `arquitectura/contexto-global.md` — global architecture and stack summary.
- `doc/PIPELINE_AGENTES.md` — agent pipeline and role conventions.
- `skills-lock.json` — pinned/known skills in this workspace.

## Active skills

| Skill | Location | Trigger / Use |
|---|---|---|
| `feature-pipeline` | `.claude/skills/feature-pipeline/SKILL.md` | End-to-end feature pipeline (analysis → spec → implementation → verify → browser QA). |
| `frontend-design` | `.agents/skills/frontend-design/SKILL.md` | High-quality UI/UX work for web pages/components. |
| `find-skills` | `.agents/skills/find-skills/SKILL.md` | Discover/install skills when the user asks for capabilities. |
| `java-springboot` | `.claude/skills/java-springboot/SKILL.md` | Spring Boot implementation guidance. |
| `product-business-festia` | `.claude/skills/product-business-festia/SKILL.md` | Product/business prioritization for Musicon/Festia. |
| `sector-musical-galicia` | `.claude/skills/sector-musical-galicia/SKILL.md` | Galicia/Spain musical-events domain context. |
| `sdd-init` | `~/.config/opencode/skills/sdd-init/SKILL.md` | Initialize SDD context and persistence. |
| `sdd-explore` | `~/.config/opencode/skills/sdd-explore/SKILL.md` | Research and exploration phase. |
| `sdd-propose` | `~/.config/opencode/skills/sdd-propose/SKILL.md` | Proposal phase. |
| `sdd-spec` | `~/.config/opencode/skills/sdd-spec/SKILL.md` | Specification phase. |
| `sdd-design` | `~/.config/opencode/skills/sdd-design/SKILL.md` | Technical design phase. |
| `sdd-tasks` | `~/.config/opencode/skills/sdd-tasks/SKILL.md` | Task breakdown phase. |
| `sdd-apply` | `~/.config/opencode/skills/sdd-apply/SKILL.md` | Implementation phase. |
| `sdd-verify` | `~/.config/opencode/skills/sdd-verify/SKILL.md` | Validation phase. |
| `sdd-archive` | `~/.config/opencode/skills/sdd-archive/SKILL.md` | Archive completed change. |

## Notes
- Project stack is Spring Boot / Maven / Thymeleaf / JPA / PostgreSQL with optional MariaDB legacy support.
- Prefer SDD phases for substantial changes; use the feature pipeline when the work needs full product-to-code flow.
