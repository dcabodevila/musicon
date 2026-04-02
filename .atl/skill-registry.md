# Skill Registry — musicon

## Project conventions

- `CLAUDE.md` — minimal project instructions; delegates to global setup.
- `arquitectura/contexto-global.md` — authoritative stack and architecture summary.
- `doc/PIPELINE_AGENTES.md` — agent pipeline roles and flow.
- `.claude/agents/*.md` — sub-agent definitions (bosco, susan, hackerman, travis, seymour, draymond).

## Active skills

| Skill | Location | Trigger / Use |
|---|---|---|
| `feature-pipeline` | `.claude/skills/feature-pipeline/SKILL.md` | End-to-end feature pipeline: business analysis → spec → implementation → validation → browser QA. |
| `frontend-design` | `.agents/skills/frontend-design/SKILL.md` | High-quality UI/UX for web pages and components. |
| `find-skills` | `.agents/skills/find-skills/SKILL.md` | Discover/install skills when user asks for new capabilities. |
| `java-springboot` | `.claude/skills/java-springboot/SKILL.md` | Spring Boot implementation guidance and best practices. |
| `product-business-festia` | `.claude/skills/product-business-festia/SKILL.md` | Product/business prioritization for Musicon/Festia platform. |
| `sector-musical-galicia` | `.claude/skills/sector-musical-galicia/SKILL.md` | Galicia/Spain musical-events domain context (verbena orchestras, agencies). |
| `sdd-init` | `~/.config/opencode/skills/sdd-init/SKILL.md` | Initialize SDD context and persistence. |
| `sdd-explore` | `~/.config/opencode/skills/sdd-explore/SKILL.md` | Research and exploration phase. |
| `sdd-propose` | `~/.config/opencode/skills/sdd-propose/SKILL.md` | Proposal phase. |
| `sdd-spec` | `~/.config/opencode/skills/sdd-spec/SKILL.md` | Specification phase. |
| `sdd-design` | `~/.config/opencode/skills/sdd-design/SKILL.md` | Technical design phase. |
| `sdd-tasks` | `~/.config/opencode/skills/sdd-tasks/SKILL.md` | Task breakdown phase. |
| `sdd-apply` | `~/.config/opencode/skills/sdd-apply/SKILL.md` | Implementation phase. |
| `sdd-verify` | `~/.config/opencode/skills/sdd-verify/SKILL.md` | Validation phase. |
| `sdd-archive` | `~/.config/opencode/skills/sdd-archive/SKILL.md` | Archive completed change. |
| `go-testing` | `~/.config/opencode/skills/go-testing/SKILL.md` | Go test patterns (not primary stack — use only if Go code is added). |
| `skill-creator` | `~/.config/opencode/skills/skill-creator/SKILL.md` | Creating new agent skills. |

## Notes

- SDD skills live at `~/.config/opencode/skills/sdd-*/SKILL.md` (canonical source, mirrored to `~/.claude/skills/`).
- Project stack: Spring Boot 3.2.5 / Java 17 / Maven / Thymeleaf / JPA / PostgreSQL (+ optional MariaDB legacy).
- Use `feature-pipeline` skill for full product-to-code flows. Use SDD phases for spec-driven technical changes.
- Agent pipeline: bosco (Opus) → susan (Sonnet) → hackerman (Sonnet) → travis (Haiku) → seymour (Haiku).
