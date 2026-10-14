# News placement consistency — DEV

Two rendering paths undermined explicit CMS placement: category tabs promoted their first article to lead regardless of placement, and the no-lead layout stretched the sidebar to full width. Live categories and Latest now use the same saved LEAD/SIDEBAR_1/SIDEBAR_2/MORE assignments. A category filters the publication's stories, without promoting any story. A category with only a sidebar renders a compact card aligned to the right (maximum 420px); it does not need a lead story first. More stories remain below the featured section. Mock design previews retain their sample arrangement.

The CMS now explains that positions apply to Latest and the article's category, with one published article per featured position across the publication. It labels the selector/receipt Display position. Returning to a section or using the reader's Back button clears a stale article query parameter, so refreshing a section does not reopen a previous full article.

Initial read-only DEV metadata showed the U.S. Church article in SIDEBAR_1 and the Vatican pope article in MORE. The latter must be set to LEAD to appear as the main story; fixing rendering alone must not silently reinterpret its saved placement. Article text and photos are outside this fix.

Validation: 34 web tests passed, including category-specific placement, missing lead/More-only DOM rendering, and stale deep-link clearing. Production build passed with the existing bundle/CommonJS warnings. Browser inspection against the actual published DEV articles confirmed the lone U.S. sidebar is 420px wide with a 210px image and no lead card. Desktop/web version 1.0.18; no backend/schema, native app code/version, or production infrastructure changes.
