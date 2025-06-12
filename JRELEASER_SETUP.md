# JReleaser Setup for Spring Test Insight

This document provides instructions for setting up JReleaser to release the Spring Test Insight extension to Maven Central.

## Prerequisites

Before you can use the release workflow, you need to configure the following:

### 1. Maven Central Account

1. **Create a Sonatype Account**: Register at [Sonatype JIRA](https://issues.sonatype.org/)
2. **Request a Namespace**: Create a ticket to claim the `digital.pragmatech` groupId
3. **Verify Domain Ownership**: You may need to prove ownership of the domain or use an alternative namespace

### 2. GPG Key Setup

Generate a GPG key pair for signing artifacts:

#### Option 1: Interactive Generation
```bash
# Generate a new GPG key interactively
gpg --full-generate-key

# Choose:
# - RSA and RSA (default)
# - 4096 bits
# - Key expires in 2 years (recommended)
# - Enter your real name and email
# - Create a secure passphrase
```

#### Option 2: Batch Generation (Automated)
```bash
# Generate GPG key in batch mode
gpg --batch --gen-key <<EOF
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: Your Full Name
Name-Email: your.email@example.com
Expire-Date: 2y
Passphrase: your-very-secure-passphrase
%commit
EOF
```

#### Export Keys for GitHub Secrets
```bash
# List your keys to get the key ID (8-character or 16-character ID)
gpg --list-secret-keys --keyid-format LONG

# Example output:
# sec   rsa4096/ABCD1234EFGH5678 2025-01-01 [SC] [expires: 2027-01-01]
# uid                 [ultimate] Your Name <your.email@example.com>
# ssb   rsa4096/12345678ABCDEFGH 2025-01-01 [E] [expires: 2027-01-01]

# Use the key ID (ABCD1234EFGH5678 in this example)
export GPG_KEY_ID="ABCD1234EFGH5678"

# Export PUBLIC key (for MAVEN_GPG_PUBLIC_KEY secret)
gpg --armor --export $GPG_KEY_ID

# Export PRIVATE key (for MAVEN_GPG_SECRET_KEY secret) - KEEP THIS SECURE!
gpg --armor --export-secret-keys $GPG_KEY_ID

# Test your key
echo "test" | gpg --clearsign --armor --local-user $GPG_KEY_ID
```

#### Verify GPG Setup
```bash
# Test signing
echo "test message" | gpg --clearsign --armor --pinentry-mode loopback

# List keys in different formats
gpg --list-keys
gpg --list-secret-keys
```

### 3. GitHub Repository Secrets

Configure the following secrets in your GitHub repository (Settings → Secrets and variables → Actions):

#### Required Secrets:

- **`MAVEN_CENTRAL_USERNAME`**: Your Sonatype JIRA username
- **`MAVEN_CENTRAL_PASSWORD`**: Your Sonatype JIRA password or token
- **`MAVEN_GPG_PASSPHRASE`**: The passphrase for your GPG key
- **`MAVEN_GPG_PUBLIC_KEY`**: Your GPG public key (from `gpg --armor --export YOUR_KEY_ID`)
- **`MAVEN_GPG_SECRET_KEY`**: Your GPG private key (from `gpg --armor --export-secret-keys YOUR_KEY_ID`)

#### How to Set GitHub Secrets:

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret with the exact name and value:

```bash
# For MAVEN_GPG_PUBLIC_KEY, copy the ENTIRE output including headers:
-----BEGIN PGP PUBLIC KEY BLOCK-----

mQINBGXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
[... many lines of base64 encoded data ...]
=XXXX
-----END PGP PUBLIC KEY BLOCK-----

# For MAVEN_GPG_SECRET_KEY, copy the ENTIRE output including headers:
-----BEGIN PGP PRIVATE KEY BLOCK-----

lQdGBGXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
[... many lines of base64 encoded data ...]
=XXXX
-----END PGP PRIVATE KEY BLOCK-----
```

⚠️ **Important**: Copy the keys exactly as exported, including the `-----BEGIN/END-----` lines.

#### Optional Secrets:
- **`GITHUB_TOKEN`**: Automatically provided by GitHub Actions (no setup needed)

## GitHub Actions GPG Handling

The release workflow automatically:

1. **Imports your GPG keys** into the GitHub Actions runner
2. **Configures GPG for headless operation** (no GUI prompts)
3. **Sets up pinentry-mode loopback** for automated signing
4. **Configures Maven GPG plugin** to use the imported key

The workflow includes these GPG setup steps:

```yaml
- name: Import GPG key
  run: |
    echo "${{ secrets.MAVEN_GPG_SECRET_KEY }}" | gpg --batch --import
    echo "${{ secrets.MAVEN_GPG_PUBLIC_KEY }}" | gpg --batch --import
    
- name: Configure GPG
  run: |
    echo "use-agent" >> ~/.gnupg/gpg.conf
    echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
    echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
    echo RELOADAGENT | gpg-connect-agent
```

This ensures GPG signing works in the automated CI environment without manual intervention.

**Note:** The official JReleaser GitHub Action handles GPG import and configuration automatically, so you don't need to manually configure GPG in your workflow!

### 4. POM.xml Configuration

The `pom.xml` in the `spring-test-insight-extension` directory needs to be updated with:

#### Required Information:
```xml
<groupId>digital.pragmatech</groupId>
<artifactId>spring-test-insight-extension</artifactId>
<name>Spring Test Insight Extension</name>
<description>JUnit Jupiter extension for Spring Test insights and context caching visualization</description>
<url>https://github.com/youruser/spring-test-insight</url>

<licenses>
    <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
    </license>
</licenses>

<developers>
    <developer>
        <id>youruser</id>
        <name>Your Name</name>
        <email>your.email@example.com</email>
    </developer>
</developers>

<scm>
    <connection>scm:git:https://github.com/youruser/spring-test-insight.git</connection>
    <developerConnection>scm:git:https://github.com/youruser/spring-test-insight.git</developerConnection>
    <url>https://github.com/youruser/spring-test-insight</url>
</scm>
```

#### Distribution Management:
```xml
<distributionManagement>
    <snapshotRepository>
        <id>central-portal</id>
        <url>https://central.sonatype.com/</url>
    </snapshotRepository>
    <repository>
        <id>central-portal</id>
        <url>https://central.sonatype.com/</url>
    </repository>
</distributionManagement>
```

### 5. JReleaser Maven Plugin

Add the JReleaser plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>org.jreleaser</groupId>
    <artifactId>jreleaser-maven-plugin</artifactId>
    <version>1.13.1</version>
    <configuration>
        <jreleaser>
            <project>
                <name>spring-test-insight-extension</name>
                <description>JUnit Jupiter extension for Spring Test insights</description>
                <longDescription>
                    Spring Test Insight is a JUnit Jupiter extension that provides detailed insights 
                    into Spring Test execution, focusing on context caching statistics and performance optimization.
                </longDescription>
                <website>https://github.com/youruser/spring-test-insight</website>
                <authors>
                    <author>Your Name</author>
                </authors>
                <license>MIT</license>
                <inceptionYear>2025</inceptionYear>
            </project>
            <release>
                <github>
                    <owner>youruser</owner>
                    <name>spring-test-insight</name>
                    <tagName>v{{projectVersion}}</tagName>
                    <releaseName>Release {{projectVersion}}</releaseName>
                    <milestone>
                        <name>{{projectVersion}}</name>
                    </milestone>
                </github>
            </release>
            <deploy>
                <maven>
                    <nexus2>
                        <maven-central>
                            <active>ALWAYS</active>
                            <url>https://central.sonatype.com/api/v1/publisher</url>
                            <closeRepository>true</closeRepository>
                            <releaseRepository>true</releaseRepository>
                            <stagingRepositories>target/staging-deploy</stagingRepositories>
                        </maven-central>
                    </nexus2>
                </maven>
            </deploy>
        </jreleaser>
    </configuration>
</plugin>
```

### 6. Release Profile

Add a release profile to your `pom.xml`:

```xml
<profiles>
    <profile>
        <id>release</id>
        <properties>
            <maven.test.skip>true</maven.test.skip>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>3.3.1</version>
                    <executions>
                        <execution>
                            <id>attach-sources</id>
                            <goals>
                                <goal>jar-no-fork</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>3.6.0</version>
                    <executions>
                        <execution>
                            <id>attach-javadocs</id>
                            <goals>
                                <goal>jar</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-gpg-plugin</artifactId>
                    <version>3.2.7</version>
                    <executions>
                        <execution>
                            <id>sign-artifacts</id>
                            <phase>verify</phase>
                            <goals>
                                <goal>sign</goal>
                            </goals>
                            <configuration>
                                <gpgArguments>
                                    <arg>--pinentry-mode</arg>
                                    <arg>loopback</arg>
                                </gpgArguments>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## How to Release

### 1. Using GitHub Actions (Recommended)

1. Go to your repository on GitHub
2. Navigate to **Actions** → **Release**
3. Click **Run workflow**
4. Enter the release version (e.g., `1.0.0`)
5. Click **Run workflow**

The workflow will:
- Build and test with Maven (using the release profile)
- Use the **official JReleaser GitHub Action** to handle:
  - Version management
  - Artifact signing with GPG
  - Deploy to Maven Central
  - Create GitHub release with changelog
  - Git tagging
- Upload JReleaser logs for debugging

### 2. Manual Release

If you prefer to release manually:

```bash
# Release with JReleaser (it handles everything!)
./mvnw -Prelease jreleaser:full-release -Djreleaser.project.version=1.0.0
```

That's it! JReleaser will:
- Set the release version
- Build and test the project
- Sign artifacts
- Deploy to Maven Central
- Create GitHub release with changelog
- Tag the release

## Troubleshooting

### Common Issues:

#### 1. GPG Signing Fails in GitHub Actions
```
Error: gpg: signing failed: No such file or directory
Error: gpg: signing failed: Inappropriate ioctl for device
```

**Solutions:**
- Ensure `MAVEN_GPG_SECRET_KEY` includes the complete private key with headers
- Verify `MAVEN_GPG_PASSPHRASE` matches the key's passphrase exactly
- Check that the GPG key hasn't expired
- Make sure the key was imported correctly in the workflow

**Debug Steps:**
```bash
# Test locally first
echo "test" | gpg --clearsign --armor --pinentry-mode loopback

# In GitHub Actions, add debug step
- name: Debug GPG
  run: |
    gpg --list-secret-keys
    echo "test" | gpg --clearsign --armor --pinentry-mode loopback
```

#### 2. Maven Central Authentication Issues
```
Error: Access denied to repository
Error: 401 Unauthorized
```

**Solutions:**
- Verify `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` are correct
- Use a Sonatype user token instead of your password (recommended)
- Ensure your account has permissions for the groupId

#### 3. Namespace/GroupId Issues
```
Error: You are not authorized to deploy to digital.pragmatech
```

**Solutions:**
- Request namespace access via [Sonatype JIRA](https://issues.sonatype.org/)
- Use a groupId you own (e.g., `io.github.yourusername`)
- Verify domain ownership for custom namespaces

#### 4. GPG Key Import Issues
```
Error: gpg: invalid armor header
Error: gpg: no valid OpenPGP data found
```

**Solutions:**
- Ensure you copied the complete key including `-----BEGIN/END-----` lines
- Check for extra whitespace or line breaks in the secret
- Re-export and re-add the key to GitHub Secrets

### Useful Commands:

```bash
# Test JReleaser configuration
./mvnw jreleaser:config

# Dry run release
./mvnw jreleaser:full-release -Djreleaser.dry.run=true

# Check GPG setup locally
gpg --list-secret-keys
gpg --list-keys

# Test GPG signing locally
echo "test" | gpg --clearsign --armor --pinentry-mode loopback

# Verify key format
gpg --armor --export YOUR_KEY_ID | head -5
gpg --armor --export-secret-keys YOUR_KEY_ID | head -5

# Re-export keys if needed
gpg --armor --export YOUR_KEY_ID > public.key
gpg --armor --export-secret-keys YOUR_KEY_ID > private.key
```

### Testing the Release Workflow

Before doing a real release, you can test with a dry run:

1. **Local Dry Run:**
```bash
cd spring-test-insight-extension
./mvnw jreleaser:full-release -Djreleaser.dry.run=true
```

2. **GitHub Actions Dry Run:**
- Temporarily add `-Djreleaser.dry.run=true` to the release workflow
- Run the workflow to test GPG and Maven configuration
- Remove the dry run flag for the actual release

## Repository Structure After Setup

```
spring-test-insight/
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── release.yml
├── spring-test-insight-extension/
│   ├── pom.xml                    # Updated with release configuration
│   └── src/
├── demo/
│   ├── spring-boot-3.4-maven/
│   ├── spring-boot-3.5-maven/
│   └── spring-boot-3.5-gradle/
└── JRELEASER_SETUP.md             # This file
```

Once configured, your project will be ready for automated releases to Maven Central!