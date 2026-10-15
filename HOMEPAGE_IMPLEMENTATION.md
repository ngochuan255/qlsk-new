# EventHub Pro homepage implementation ledger

Approved plan: the user's 2026-10-15 EventHub Pro homepage upgrade plan in this chat.

- Baseline: c1cceb5; isolated worktree on codex/eventhub-homepage.
- Scope: homepage markup, scoped styles, data/rendering, motion, browser tests, and UTF-8 response configuration.
- Ruling: retain native vertical scrolling and use GSAP/ScrollTrigger for visual interpolation, as approved; no smooth-scroll input interception.
- Ruling: test fixtures simulate multiple events and roles without changing the user's database.
- Pre-flight: auth.js initializes on DOMContentLoaded; the homepage uses a separate home-auth element and the existing session helpers, rendering account fields safely without altering shared page authentication.
- Pre-flight: only the initial unfiltered event response controls hero/featured content; a monotonic request counter protects filtered results.
- Task 1: complete — initial RED confirmed against the old homepage; acceptance checks cover the new behavior and failure modes.
- Task 2: complete — layout, scoped design system, safe API rendering and account navigation; 29 scripts, 123 local references, 0 static check failures.
- Task 3: complete — motion, gallery, responsive menu and bfcache cleanup verified in browser scenarios.
- Ruling: serve the existing GSAP 3.12.5 libraries locally with their original license headers, avoiding a runtime CDN dependency.
- Regression RED: account fixture containing HTML creates an image through the previous shared navbar renderer. Homepage now builds account controls with textContent and normalizes both role name formats; shared auth.js is unchanged.
- Task 4: packaged app active at http://127.0.0.1:8080/; live checks pass at 375, 768, 1024 and 1440px using real public API data. Sources integrated into D:\QLSK on codex/eventhub-homepage-local, left uncommitted for review. Original JAR preserved as target/qlsk-system-1.0.0.pre-homepage.jar.
- Browser acceptance: 12 groups passed, including role menus, unsafe text, failed API recovery, drag and the three scroll scenes.
- Backend baseline: all 8 KnownIssuesTest failures reproduced unchanged on c1cceb5 in a separate baseline directory. Full upgraded suite: 54 tests, 46 passed, the same 8 KnownIssuesTest failures. No Java source changes.
- Final review: fresh independent read-only reviewer found one Important lifecycle issue (pageshow handlers multiplied on bfcache restores). Regression RED observed ScrollTrigger counts 1,2,5,12; lifecycle handlers now register once and dispose contexts before rebuilding.
- Review scope rulings: live API availability and production activation will be checked on the packaged local app; real device touch and non-Chromium browsers cannot be established by the automated Chromium tests and will be reported as coverage limits. Existing backend defects remain outside the homepage upgrade.
- Final browser suite: 14 groups GREEN, including lifecycle regression and native touch gallery/vertical scrolling. Maven ApplicationIntegrationTest: 46/46 GREEN; JAR packaged successfully.
- Packaged-app regression RED: /vendor/gsap scripts return 403 to guests because static resources are allowed under /js. Ruling: move the exact vendor files to /js/vendor/gsap rather than changing application security configuration. Live checks explicitly require the real packaged GSAP runtime.
- Packaged-app regression GREEN: public CSS, homepage JavaScript, GSAP and ScrollTrigger all return HTTP 200. HTML response is UTF-8. Native touch scrolling works in mobile Chromium emulation. No page JavaScript errors during live checks.
- Additional gallery acceptance GREEN: touch swipes move horizontally and a vertical swipe beginning inside the gallery still scrolls the page; no unintended navigation.
- Performance: unthrottled headless Chrome against localhost, four fresh page contexts; observed LCP 576–860ms and layout-shift sum 0–0.00456 before interaction. These are local laboratory measurements, not production field metrics; INP and physical-device/browser coverage remain unmeasured.
- Independent review: one Important finding fixed with a failing-then-passing regression test; no deferred minor findings. The existing eight backend KnownIssuesTest failures are unrelated and reproduced on baseline c1cceb5.

## Verification commands

Use Node.js 22+ and an installed playwright-core module. Set PLAYWRIGHT_MODULE_PATH to its absolute module path; optionally set CHROME_PATH and EVENTHUB_POSTER_PATH (an existing local event poster). No application database writes are made by the browser tests.

- `node test-homepage.cjs` — isolated HTTP fixture server, browser UI/role/filter/error/drag/touch/motion checks.
- `node test-homepage-live.cjs` — read-only verification of the running JAR at port 8080; EVENTHUB_BASE_URL can override the base URL.
- `node test-frontend.cjs` — offline JavaScript parsing and local asset checks.
- `mvn -B -Dtest=ApplicationIntegrationTest package` — 46 passing integration tests and packaging.
- `mvn -B verify` — full suite currently reports the eight pre-existing KnownIssuesTest failures; do not interpret the full suite as green.

Screenshots and live measurement report: `target/homepage-checks/`. Physical iOS/Safari and Firefox checks are not part of the completed Chromium automation.

## Scroll typography and hover follow-up

- Requested behavior: a few text lines rise from the lower viewport and transition continuously from regular to bold, with smooth GSAP hover.
- Applied to the existing two-line journey heading and the two-line final CTA; content and semantics retained.
- Each block uses a ScrollTrigger from top 96% to top 52%, scrub 0.65s, line stagger 0.14, translateY 92% to 0, opacity 0.16 to 1 and font-weight 400 to 750. Scrolling upward reverses the effect.
- Local Plus Jakarta Sans variable WOFF2 subsets (Latin, Vietnamese, Latin extended), licensed under OFL, are served from the existing public /css path. Fixed line wrapping prevents weight changes from changing layout.
- Initial RED: no scroll lines/trigger existed. An exact-start regression showed the CSS bold state at progress zero; explicit gsap.set followed by gsap.to fixes the flash.
- Hover uses reusable scaleX/scaleY and arrow quickTo tweens, pointer and keyboard focus, plus a subtle button highlight. Fine-pointer/no-reduced-motion media conditions own listener cleanup.
- Narrow-screen RED: 320px text clipping was reproduced and fixed with responsive heading sizing. Reduced-motion/no-GSAP defaults remain bold, visible, readable text.
- Follow-up verification: 18/18 UI acceptance groups GREEN, independent delta review found no Important/Critical issues, and the rebuilt JAR passed live checks at 375/768/1024/1440px with real API data. Local font HTTP 200 and fractional intermediate font weight verified in the running application; 0 browser JavaScript errors. No backend Java changes or backend retest claim in this follow-up.
