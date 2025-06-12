# Quick GPG Setup for GitHub Actions

## TL;DR - GPG Setup Steps

### 1. Generate GPG Key
```bash
# Interactive method
gpg --full-generate-key
# Choose: RSA and RSA, 4096 bits, 2 years expiry

# OR Automated method
gpg --batch --gen-key <<EOF
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: Your Full Name
Name-Email: your.email@example.com
Expire-Date: 2y
Passphrase: your-secure-passphrase
%commit
EOF
```

### 2. Export Keys
```bash
# Get your key ID
gpg --list-secret-keys --keyid-format LONG

# Export for GitHub Secrets
gpg --armor --export YOUR_KEY_ID        # → MAVEN_GPG_PUBLIC_KEY
gpg --armor --export-secret-keys YOUR_KEY_ID  # → MAVEN_GPG_SECRET_KEY
```

### 3. Add to GitHub Secrets
Go to **Settings** → **Secrets and variables** → **Actions**:

- `MAVEN_GPG_PUBLIC_KEY` = Complete public key (with -----BEGIN/END----- lines)
- `MAVEN_GPG_SECRET_KEY` = Complete private key (with -----BEGIN/END----- lines)  
- `MAVEN_GPG_PASSPHRASE` = Your key passphrase
- `MAVEN_CENTRAL_USERNAME` = Sonatype username
- `MAVEN_CENTRAL_PASSWORD` = Sonatype password/token

### 4. Test Locally
```bash
# Verify your setup works
echo "test" | gpg --clearsign --armor --pinentry-mode loopback
```

## What GitHub Actions Does

The workflow automatically:
1. Imports your GPG keys into the runner
2. Configures GPG for headless operation (no GUI)
3. Sets up Maven to use GPG for artifact signing
4. Signs and deploys your artifacts to Maven Central

## Common Issues

**"No such file or directory"** → Check that secrets contain complete keys with headers
**"Inappropriate ioctl for device"** → GPG pinentry issue, workflow should handle this
**"Invalid armor header"** → Key wasn't copied completely to GitHub Secrets

## Quick Test

Add this debug step to your workflow to test GPG import:
```yaml
- name: Debug GPG
  run: |
    gpg --list-secret-keys
    echo "test signing" | gpg --clearsign --armor --pinentry-mode loopback
```

That's it! The GitHub Actions workflow handles all the GPG complexity for you.