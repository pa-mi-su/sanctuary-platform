# Unified mobile Me and Account navigation

- Android Settings → Account embeds the account chooser directly when signed out, matching iOS. The redundant Login row that closed Settings is removed.
- An original vector character uses Sanctuary teal, a gold halo, a hood and a small gold cross. The same geometry and colors render natively on Android and iOS; no Hallow artwork is included.
- Signed-out Me shows the character above the existing login/register chooser.
- Signed-in Me uses a compact character/name header between the existing announcement bell and settings gear. Existing reminders, progress and favorites remain available.
- Signed-in Settings starts with a tappable avatar/name/email profile card opening Account, followed by existing General, Notifications, Support & Legal, About and Logout actions. No subscription, family or streak features were added from the reference screenshots.
- Release metadata is aligned at 1.0.35.

Verification:
- Android DEV debug app build and 34 unit tests passed.
- iOS DEV simulator build passed, including normal simulator ad-hoc signing for Keychain access.
- UI checked in isolated Android and iOS simulators: signed-out Me, Settings → Account chooser, signed-in Me header, signed-in Settings profile card and profile → Account navigation.
- Signed-in UI checks used a synthetic localhost fixture, not a real account. No real credentials were saved.
- Existing Spanish/Polish labels are reused. No new untranslated user-facing text was introduced.

No backend, notification-delivery, database settings, backups or pipeline configuration changes.
