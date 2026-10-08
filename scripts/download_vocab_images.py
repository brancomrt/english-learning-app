#!/usr/bin/env python3
"""
Download images for vocabulary words using Lorem Picsum (reliable, no rate limits).
For a vocabulary app, we'll use abstract/placeholder images that are visually appealing.
Then we can manually curate the important ones later.

Alternative approach: Use a local icon set instead.
"""

import json
import os
import urllib.request
import ssl
import time
import hashlib
import random
from pathlib import Path

# Paths
VOCAB_DIR = Path("app/src/main/assets/content/vocabulary")
OUTPUT_DIR = Path("app/src/main/assets/vocab_images")
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Image settings
IMAGE_SIZE = 200

# SSL context
ssl_ctx = ssl.create_default_context()
ssl_ctx.check_hostname = False
ssl_ctx.verify_mode = ssl.CERT_NONE

def get_all_words():
    """Extract all unique words from vocabulary JSON files."""
    words = set()
    for json_file in VOCAB_DIR.glob("*.json"):
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            for card in data.get('cards', []):
                word = card.get('word', '').strip()
                if word:
                    words.add(word)
    return sorted(words)

def sanitize_filename(word):
    """Convert word to safe filename."""
    safe = word.lower().replace(' ', '_').replace('/', '_').replace('\\', '_')
    safe = ''.join(c for c in safe if c.isalnum() or c == '_')
    if len(safe) > 50:
        safe = safe[:30] + '_' + hashlib.md5(word.encode()).hexdigest()[:8]
    return safe

def get_seed_for_word(word):
    """Generate consistent seed from word for reproducible images."""
    return int(hashlib.md5(word.encode()).hexdigest()[:8], 16) % 1000000

def download_image_picsum(word, output_path):
    """Download image from Lorem Picsum - reliable, no rate limits."""
    seed = get_seed_for_word(word)
    # Use seed for consistent image per word
    url = f"https://picsum.photos/seed/{seed}/{IMAGE_SIZE}/{IMAGE_SIZE}"
    
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, context=ssl_ctx, timeout=30) as response:
            image_data = response.read()
            
            if len(image_data) < 1000:
                return False
                
            with open(output_path, 'wb') as f:
                f.write(image_data)
            
            return True
    except Exception as e:
        print(f"  Error: {e}")
        return False

def update_vocab_json(json_file, word_to_image):
    """Update vocabulary JSON with local image references."""
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    modified = False
    for card in data.get('cards', []):
        word = card.get('word', '').strip()
        if word in word_to_image:
            card['image_url'] = f"local:{word_to_image[word]}"
            modified = True
    
    if modified:
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

def main():
    print("=== Vocabulary Image Downloader (Lorem Picsum) ===\n")
    
    words = get_all_words()
    print(f"Found {len(words)} unique words\n")
    
    word_to_image = {}
    downloaded = 0
    skipped = 0
    failed = 0
    
    for i, word in enumerate(words):
        filename = sanitize_filename(word) + ".jpg"
        output_path = OUTPUT_DIR / filename
        
        # Skip if already exists and has content
        if output_path.exists() and output_path.stat().st_size > 1000:
            word_to_image[word] = filename
            skipped += 1
            continue
        
        print(f"[{i+1}/{len(words)}] {word} -> {filename}")
        
        if download_image_picsum(word, output_path):
            word_to_image[word] = filename
            downloaded += 1
        else:
            failed += 1
        
        # Small delay to be nice
        time.sleep(0.1)
        
        if (i + 1) % 100 == 0:
            print(f"\n--- {i+1}/{len(words)} done ({downloaded} new, {skipped} existing, {failed} failed) ---\n")
    
    print(f"\n=== Complete ===")
    print(f"Downloaded: {downloaded}")
    print(f"Existing: {skipped}")
    print(f"Failed: {failed}")
    
    # Update JSON files
    print(f"\nUpdating vocabulary JSONs...")
    for json_file in VOCAB_DIR.glob("*.json"):
        update_vocab_json(json_file, word_to_image)
        print(f"  {json_file.name}")
    
    # Size report
    total_size = sum(f.stat().st_size for f in OUTPUT_DIR.glob("*.jpg") if f.exists())
    print(f"\nTotal size: {total_size / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
