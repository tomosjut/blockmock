# BlockMock UI/UX Specialist Memory

## Project Overview
BlockMock is a multi-protocol mock server admin UI. Single-page app, vanilla JS + CSS, no framework.
Language: Dutch UI labels, English code/class names.

## Design System

### Colors (CSS custom variables NOT used — raw hex)
- Primary / brand: `#667eea` (purple-blue gradient with `#764ba2`)
- Success: `#10b981` / light: `#d1fae5` text `#065f46`
- Error / danger: `#ef4444` / light: `#fee2e2` text `#991b1b`
- Info: `#dbeafe` text `#1e40af`
- Secondary (gray): `#6b7280`
- Background: `#f5f5f5`, card bg: `white`
- Border: `#e5e7eb`, `#d1d5db`
- Text primary: `#333` / `#1f2937` / `#374151`
- Text muted: `#6b7280` / `#9ca3af`

### Typography
- System font stack (`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, …`)
- Base: `1em / 1.6` line-height
- Form labels: `font-weight: 600; color: #374151`
- Section titles: `color: #667eea; font-size: 1.2em`

### Spacing / Sizing
- Container max-width: 1400px, padding 20px
- Cards: `padding: 20px–30px`, `border-radius: 10px`, `box-shadow: 0 2px 10px rgba(0,0,0,0.1)`
- Buttons: `padding: 10px 20px`, small variant `5px 15px`
- Form groups: `margin-bottom: 20px`
- Form rows: `grid-template-columns: 1fr 1fr` (fixed 2-col)

### Components
- `.btn` classes: `btn-primary`, `btn-secondary`, `btn-danger`, `btn-success`, `btn-small`
- `.badge` classes: `success`, `error`, `info`
- `.modal` + `.modal-content` (700px max-width, activated via `.classList.add('active')`)
- `.table` wraps `<table>` for styled data tables
- `.block-card` — left-border color, block-specific
- `.scenario-card` — used for both Scenarios AND Test Suites (reuse, not dedicated class)
- `.endpoints-selection` / `.endpoint-checkbox-item` — scrollable checkbox list
- `.empty-state` — centered empty content placeholder
- `.header-row` — flex space-between title + action buttons

### Interaction Patterns
- Tab switching: `data-tab` attribute, `.active` class toggle, data loaded on tab click
- Modal open/close: `.classList.add/remove('active')` — EXCEPT Scenario modal (uses `.style.display = 'block/none'` — inconsistency)
- Inline run panels: `style.display = 'block/none'` (not CSS-class-driven)
- Error handling: `alert()` for errors throughout — no toast/notification system
- Confirmation: `confirm()` for destructive actions

## Recurring Issues / Anti-patterns Found
1. XSS risk: user-controlled strings (suite.name, suite.description, r.failureReason, block.name) interpolated directly into innerHTML without escaping
2. Modal inconsistency: Scenario modal uses `.style.display`, all others use `.classList.add('active')`
3. No ARIA roles/labels on modals, no `role="dialog"`, no `aria-modal`, no focus trapping
4. Close buttons lack `aria-label` (just `&times;` character)
5. Color-only indicators (no text label for suite color in card)
6. `form-row` is hardcoded 1fr 1fr — placing 3 children in it causes layout overflow
7. No loading states shown during async operations
8. Run state uses global `activeRunId` / `window._activeSuiteId` — only one run supported at a time
9. Empty state in Test Suites missing the SVG icon used in other empty states

## Files
- `/home/thomas/git/blockmock/src/main/resources/META-INF/resources/index.html`
- `/home/thomas/git/blockmock/src/main/resources/META-INF/resources/css/style.css`
- `/home/thomas/git/blockmock/src/main/resources/META-INF/resources/js/app.js`
