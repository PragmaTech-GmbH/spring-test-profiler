# JReleaser Quick Start

## What JReleaser Does

JReleaser handles the **entire release process** for you:

✅ **Version Management** - Sets release version automatically  
✅ **Build & Test** - Compiles and runs tests  
✅ **Artifact Signing** - Signs with GPG for security  
✅ **Maven Central** - Deploys to central repository  
✅ **GitHub Release** - Creates release with changelog  
✅ **Git Tagging** - Tags the release commit  

## Super Simple Setup

### 1. Create GPG Key
```bash
gpg --full-generate-key
# Choose RSA, 4096 bits, 2 years expiry
```

### 2. Add GitHub Secrets
Go to **Settings** → **Secrets and variables** → **Actions**:

```bash
# Export your keys first
gpg --list-secret-keys --keyid-format LONG  # Get YOUR_KEY_ID
gpg --armor --export YOUR_KEY_ID            # → MAVEN_GPG_PUBLIC_KEY
gpg --armor --export-secret-keys YOUR_KEY_ID # → MAVEN_GPG_SECRET_KEY
```

Add these secrets:
- `MAVEN_GPG_PUBLIC_KEY` = Your public key
- `MAVEN_GPG_SECRET_KEY` = Your private key  
- `MAVEN_GPG_PASSPHRASE` = Your key passphrase
- `MAVEN_CENTRAL_USERNAME` = Sonatype username
- `MAVEN_CENTRAL_PASSWORD` = Sonatype password

### 3. Release
Go to **Actions** → **Release** → **Run workflow**
- Enter version like `1.0.0`
- Click **Run workflow**

**Done!** 🎉

## What Happens During Release

1. **Maven** builds and tests your project
2. **Official JReleaser GitHub Action** takes over and:
   - Updates `pom.xml` to release version
   - Builds the project
   - Runs tests
   - Signs artifacts with GPG
   - Deploys to Maven Central
   - Creates GitHub release
   - Generates changelog from commits
   - Tags the release

## Manual Release (Local)

```bash
# One command does everything!
./mvnw -Prelease jreleaser:full-release -Djreleaser.project.version=1.0.0
```

## Test First (Dry Run)

```bash
# See what JReleaser will do without actually doing it
./mvnw jreleaser:full-release -Djreleaser.dry.run=true
```

## Key Benefits

- **Official GitHub Action** - Uses `jreleaser/release-action@v2` for best practices
- **Zero manual version management** - JReleaser handles it
- **Automated changelog** - Generated from Git commits  
- **Safe releases** - Dry run mode to test first
- **Complete process** - From build to Maven Central in one step
- **GitHub integration** - Automatic releases and tags
- **Built-in logging** - JReleaser logs are automatically uploaded for debugging

## Why Use the Official Action?

✅ **Maintained by JReleaser team** - Always up-to-date with latest features  
✅ **Automatic GPG handling** - No manual GPG configuration needed  
✅ **Better error handling** - Improved logging and debugging  
✅ **Optimized performance** - Faster execution than Maven plugin  
✅ **Artifact uploads** - Automatic log collection for troubleshooting  

That's it! JReleaser makes releasing as simple as clicking a button. 🚀