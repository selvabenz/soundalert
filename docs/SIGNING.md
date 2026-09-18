# Signing before public distribution

GitHub Actions currently produces installable debug-signed prototype APKs. This is convenient for testing, and the phone/watch pair built in the same run has matching identity for Wear OS Data Layer communication.

For stable upgrades across releases, create one private Android release keystore and keep it outside the public repository. Store the keystore and passwords in GitHub Actions secrets, then change the release workflow to sign both phone and watch APKs with the same certificate.

Do not publish the private signing key in the repository.

Because v0.1.x prototype APKs were debug-signed on ephemeral runners, their private signing key is not available. A one-time uninstall/reinstall may therefore be necessary when moving testers to the first persistently signed SoundAlert build.
