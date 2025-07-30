#!/bin/bash

# Script to resize images for the slide deck and reduce PDF file size
# Requires ImageMagick: brew install imagemagick

# Check if ImageMagick is installed
if ! command -v magick &> /dev/null; then
    echo "Error: ImageMagick not found. Please install it first:"
    echo "  brew install imagemagick   # macOS"
    echo "  apt-get install imagemagick   # Ubuntu/Debian"
    exit 1
fi

# Configuration
ASSETS_DIR="./assets"
GENERATED_DIR="./assets/generated"
MAX_WIDTH=1200  # Maximum width for resized images
QUALITY=80     # JPEG quality (0-100)

# Create generated directory if it doesn't exist
mkdir -p "$GENERATED_DIR"

# Process each image in the assets directory
for img in "$ASSETS_DIR"/*; do
  # Skip directories and SVG files (vector graphics don't need resizing)
  if [ -d "$img" ] || [[ "$img" == *.svg ]]; then
    continue
  fi

  filename=$(basename "$img")

  # Skip if it's already in the generated folder
  if [[ "$img" == *"/generated/"* ]]; then
    continue
  fi

  echo "Processing: $filename"

  # Resize the image while maintaining aspect ratio
 magick "$img" -resize "${MAX_WIDTH}x>" -quality "$QUALITY" "$GENERATED_DIR/$filename"

  # Get file sizes for comparison
  original_size=$(du -h "$img" | cut -f1)
  new_size=$(du -h "$GENERATED_DIR/$filename" | cut -f1)

  echo "  Original: $original_size, Resized: $new_size"
done

echo "Image resizing complete. Resized images are in $GENERATED_DIR"
