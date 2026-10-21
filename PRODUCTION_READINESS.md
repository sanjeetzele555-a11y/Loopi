# Verification and release checklist

## Verified in this workspace

- `testDebugUnitTest` passes.
- `assembleDebug` passes and the debug APK installs and launches on the `LoopiQA` emulator.
- Reproduced and fixed the Review Settings back-arrow hit-target overlap with the Android status bar. Tapping the visible arrow returns to the review topic list. System Back also works on the tested review route.
- Walked through a three-question Operating Systems review. Correctness, confidence, answer timing, and session results were saved and appeared in Home, Stats, and Profile.
- Home counts distinct saved flashcards and shows a separate daily-goal count. The tested state showed 3/52 overall and Today 3/20.
- Profile follows the Flow 6 layout pattern (identity, streak, profile actions, packs, settings, help, logout). The Study Packs shortcut opens the saved pack list.
- Profile name/student ID edits saved. The Settings and Help routes opened, the Study Packs shortcut worked, and logout presents a confirmation. A saved Weak cards only default propagated to Review Settings; the QA preference was restored to All cards. Settings are stored in local preferences.
- The app has four starter topics and 52 starter cards; unit tests cover the starter bank and reviewed-card counting.

## Not certified by this pass

- The reference set contains many individual states across Flows 1–6; only the documented paths have been manually exercised. The layouts are responsive Compose implementations, not pixel-perfect copies of every reference image.
- File selection, external link handling, sharing, accessibility, rotation, offline/reinstall behavior, and Room upgrade paths need device-specific testing.
- There is no server or cloud synchronization. Local account storage is suitable for a course demo, not a production identity system. Do not use real or reused passwords.
- No signed release build has been produced. The app still needs a team-owned signing key, chosen permanent application ID, privacy disclosures, and release configuration.
- Verify course AI-use and authorship rules before submission.

## Suggested final QA before a public release

1. Install a clean build and walk each screen and back path on the minimum and target Android versions.
2. Create an account, sign in/out, edit profile, and verify data survives app restarts as intended.
3. Add topics and each resource type; exercise invalid fields, cancel, edit, delete, and external file/link handoffs.
4. Complete correct and incorrect reviews, weak-card practice, confidence selection, and different session sizes; compare every displayed metric with saved results.
5. Test Room schema migrations from each supported prior version and backup/restore behavior.
6. Run accessibility checks, privacy/security review, and signed release validation on a physical device.
