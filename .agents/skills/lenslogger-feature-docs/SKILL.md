---
name: lenslogger-feature-docs
description: "Document LensLogger feature additions and updates in its GitHub Pages site. Use when implementing a new feature in this repository or when its project documentation needs to change."
---

# LensLogger Feature Documentation

When a user asks to add or change a LensLogger feature, update the relevant GitHub Pages documentation as part of the same task unless the user asks to skip docs. For documentation-only requests, make the requested docs change without altering feature code.

## Choose the documentation shape

- Add a focused section to an existing page when the feature is a small extension of an existing workflow.
- Add a standalone guide when the feature has its own setup, multi-step workflow, configuration, or troubleshooting. Follow the structure of `docs/mocking.html` and `docs/datastore.html`.
- Use `docs/index.html` as the root Setup & API page. The canonical guides are `docs/network.html`, `docs/mocking.html`, and `docs/datastore.html`; `docs/examples.html` is the screenshot-led mock showcase. `docs/reference.html` is retained as a redirect to the root page.

## Preserve the reading experience

- Read the implementation and existing documentation before writing. Describe behavior supported by the code and keep code examples consistent with the public API.
- Use the shared documentation shell: `.reference-page`, `.reference-layout`, `.outline`, `.reference-content`, and the shared styles in `docs/styles.css`, `docs/reference.css`, and `docs/navigation.css`.
- Keep the sticky top navigation consistent across `index.html`, `network.html`, `mocking.html`, `datastore.html`, and `examples.html`: Setup & API, Network, Mocking, DataStore, and the GitHub link.
- Keep the shared light/dark theme toggle on every live documentation page. Load `theme-init.js` in the document head, `themes.css` after the shared navigation stylesheet, and `theme.js` after `icons.js`; keep the selected theme and mobile header behavior consistent across pages.
- Keep the collapsible, grouped outline on reference-style pages. Include the new page or section there, set `aria-current="page"` on its active navigation item, and keep page-section links aligned with real element IDs.
- For a new standalone guide, update the global navigation and relevant guide groups on all existing documentation pages, the landing/showcase links, and the root `README.md`. Use relative links so the static site works at the GitHub Pages project path.
- Keep screenshots and other page assets under `docs/assets/`. The Pages workflow publishes `docs/` as a static site; avoid adding a build step unless the user asks for one.
- Use Lucide icons for navigation and small visual cues. Each page loads the pinned Lucide `1.77.0` UMD build, then initializes it with `docs/icons.js`. Keep text labels visible, mark decorative icons `aria-hidden="true"`, and verify icon names against Lucide. Do not add emoji or a second icon library for the same UI.
- Preserve mobile behavior: the top navigation can scroll horizontally, and outline groups collapse on compact screens through `docs/reference.js`.

## Finish the update

- Check cross-links, heading anchors, and referenced assets, then run `git diff --check`.
- Do not run application tests solely for a documentation change. Do not publish, push, or otherwise deploy changes unless the user explicitly asks; the existing Pages workflow deploys when the docs reach `main`.
