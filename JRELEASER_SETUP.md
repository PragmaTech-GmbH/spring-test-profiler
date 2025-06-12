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

```bash
# Generate a new GPG key
gpg --gen-key

# List your keys to get the key ID
gpg --list-secret-keys --keyid-format LONG

# Export your public key
gpg --armor --export YOUR_KEY_ID

# Export your private key (keep this secure!)
gpg --armor --export-secret-keys YOUR_KEY_ID
```

### 3. GitHub Repository Secrets

Configure the following secrets in your GitHub repository (Settings → Secrets and variables → Actions):

#### Required Secrets:

- **`MAVEN_CENTRAL_USERNAME`**: Your Sonatype JIRA username
- **`MAVEN_CENTRAL_PASSWORD`**: Your Sonatype JIRA password or token
- **`MAVEN_GPG_PASSPHRASE`**: The passphrase for your GPG key
- **`MAVEN_GPG_PUBLIC_KEY`**: Your GPG public key (from `gpg --armor --export YOUR_KEY_ID`)
- **`MAVEN_GPG_SECRET_KEY`**: Your GPG private key (from `gpg --armor --export-secret-keys YOUR_KEY_ID`)

#### Optional Secrets:
- **`GITHUB_TOKEN`**: Automatically provided by GitHub Actions (no setup needed)

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
5. Optionally enter the next development version (e.g., `1.1.0-SNAPSHOT`)
6. Click **Run workflow**

The workflow will:
- Update the version in `pom.xml`
- Build and test the project
- Deploy to Maven Central staging repository
- Create a GitHub release with JReleaser
- Update to next development version (if specified)

### 2. Manual Release

If you prefer to release manually:

```bash
# Set the release version
./mvnw versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false

# Build and deploy
./mvnw clean deploy -Prelease

# Release with JReleaser
./mvnw jreleaser:full-release

# Set next development version
./mvnw versions:set -DnewVersion=1.1.0-SNAPSHOT -DgenerateBackupPoms=false
```

## Troubleshooting

### Common Issues:

1. **GPG Signing Fails**: Ensure your GPG key is properly configured and the passphrase is correct
2. **Maven Central Sync Issues**: Check that your Sonatype account has the necessary permissions
3. **Namespace Issues**: Verify that you own the `digital.pragmatech` groupId or use an alternative

### Useful Commands:

```bash
# Test JReleaser configuration
./mvnw jreleaser:config

# Dry run release
./mvnw jreleaser:full-release -Djreleaser.dry.run=true

# Check GPG setup
gpg --list-secret-keys
```

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