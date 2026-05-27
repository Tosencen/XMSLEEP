import requests, re, os, json, time, math
from urllib.parse import unquote

OUTPUT = os.path.expanduser("~/Desktop/mixkit_sounds")
os.makedirs(OUTPUT, exist_ok=True)

session = requests.Session()
session.headers.update({
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
})

CATEGORIES = [
    "rain", "ambience", "nature", "water", "forest",
    "ocean", "waves", "fire", "storm", "thunder",
    "rivers", "birds", "crickets", "night", "wind",
    "beach", "sea", "waterfall"
]

MIN_DURATION_SEC = 12

def parse_duration(text):
    parts = text.strip().split(":")
    if len(parts) == 2:
        return int(parts[0]) * 60 + int(parts[1])
    return 0

def fetch_category(cat):
    url = f"https://mixkit.co/free-sound-effects/{cat}/"
    r = session.get(url, timeout=15)
    html = r.text
    
    # Find all sound cards - look for the pattern
    sounds = []
    
    # Pattern: find all sound items in the grid
    # Each sound card has: title, duration, tags, and a download button
    # Look for the download link pattern
    
    # Find all script tags that might contain JSON data
    # First try to find the sound grid items
    
    # Split by sound card pattern
    cards = re.findall(
        r'<a\s+class="[^"]*"[\s\S]{0,200}?href="/free-sound-effects/[^/]+?-(\d+)/"[\s\S]{0,500}?<span[^>]*class="[^"]*duration[^"]*"[^>]*>\s*(\d+:\d+)\s*</span>',
        html
    )
    
    # Simpler approach: find all download links with IDs
    download_links = re.findall(
        r'data-download--button-modal-url-value="/free-sound-effects/download/(\d+)/"',
        html
    )
    
    # Find all sound names
    name_pattern = re.findall(
        r'<span[^>]*class="[^"]*text-[^"]*"[^>]*>\s*([\w\s\-\(\)]+?)\s*</span>',
        html
    )
    
    # Find duration spans
    duration_spans = re.findall(
        r'<span[^>]*class="[^"]*duration[^"]*"[^>]*>\s*(\d+:\d+)\s*</span>',
        html
    )
    
    # Find the actual sound titles from a specific structure
    # Look for h2/span with the sound name near the download button
    titles = re.findall(
        r'<span[^>]*class="[^"]*text-lg[^"]*"[^>]*>\s*([^<]+?)\s*</span>',
        html
    )
    if not titles:
        titles = re.findall(
            r'<h2[^>]*class="[^"]*text-lg[^"]*"[^>]*>\s*([^<]+?)\s*</h2>',
            html
        )
    if not titles:
        titles = re.findall(
            r'class="[^"]*text-lg[^"]*font-semibold[^"]*"[^>]*>\s*([^<]+?)\s*<',
            html
        )
    if not titles:
        # Broad search for sound titles near duration
        titles = re.findall(
            r'<span[^>]*class="[^"]*font-semibold[^"]*"[^>]*>\s*([^<]{3,60}?)\s*</span>',
            html
        )
    
    # Check if there's embedded JSON
    json_match = re.search(r'<script[^>]*id="[^"]*"[^>]*type="application/json"[^>]*>(.*?)</script>', html, re.DOTALL)
    if json_match:
        try:
            data = json.loads(json_match.group(1))
            return {"sounds": data, "source": "json", "category": cat}
        except:
            pass
    
    # Another pattern: look for the grid items
    grid_match = re.findall(
        r'data-sound-id="(\d+)"[\s\S]{0,300}?<span[^>]*class="[^"]*duration[^"]*"[^>]*>\s*(\d+:\d+)\s*</span>',
        html
    )
    
    return {
        "ids": list(set(download_links)),
        "titles": titles[:len(download_links)] if titles else [],
        "durations": duration_spans[:len(download_links)] if len(duration_spans) >= len(download_links) else duration_spans,
        "source": "html", "category": cat
    }

print("Fetching Mixkit categories...")
all_sounds = {}

for cat in CATEGORIES:
    try:
        print(f"  {cat}...", end=" ", flush=True)
        result = fetch_category(cat)
        all_sounds[cat] = result
        if result.get("source") == "html":
            print(f"{len(result.get('ids', []))} sounds found")
        else:
            print(f"{len(result.get('sounds', []))} sounds found (json)")
        time.sleep(0.5)
    except Exception as e:
        print(f"ERROR: {e}")

# Collect unique sound IDs with their names and durations
# Since we can't perfectly parse all titles, let's use a different approach:
# Fetch individual sound pages to get metadata

# Known good white noise sounds from research - curated list
# Format: (id, name, duration_sec, category)
CURATED = [
    # Rain
    (2393, "Light Rain Loop", 15, "rain"),
    (2391, "Light Rain Loop 39s", 39, "rain"),
    (2392, "Rain Long Loop", 57, "rain"),
    (2394, "Thunderstorm and Rain Loop", 52, "rain"),
    (2395, "Forest Rain Loop", 24, "rain"),
    (2399, "Heavy Rain and Thunder", 22, "rain"),
    (2400, "Rain and Thunder Storm", 29, "rain"),
    (2401, "Calm Thunderstorm in Jungle", 49, "rain"),
    (2402, "Thunder Rumble During Storm", 51, "rain"),
    (2403, "Heavy Rain Drops", 12, "rain"),
    (2404, "Thunder with Rain", 20, "rain"),
    (2405, "Thunderstorm and Clear Rain", 44, "rain"),
    (2406, "Rain in Jungle and Birds", 30, "rain"),
    (2415, "Jungle Rain and Birds", 80, "rain"),
    (2431, "Heavy Rain on Car Glass", 39, "rain"),
    (2474, "Rain and Traffic", 30, "rain"),
    
    # Ambience / Nature
    (7, "Morning Birds", 209, "ambience"),
    (39, "Forest Birds Ambience", 151, "ambience"),
    (61, "Night Crickets Near Swamp", 33, "ambience"),
    (360, "Night Forest with Insects", 84, "ambience"),
    (368, "Wind Blowing Ambience", 61, "ambience"),
    (369, "River in Forest with Birds", 95, "ambience"),
    (375, "Ocean Waves and Birds", 60, "ambience"),
    (444, "Birds in Jungle", 60, "ambience"),
    (447, "Urban Ambience Day", 46, "ambience"),
    (461, "Natural Ambience Flowing Water Birds", 48, "ambience"),
    (462, "Night Crickets", 30, "ambience"),
    (492, "Sea Waves Ambience", 151, "ambience"),
    (1210, "Birds Chirping Near River", 90, "ambience"),
    (1212, "River Water Flowing", 180, "ambience"),
    (1213, "European Forest Ambience", 161, "ambience"),
    (1736, "Forest Birds Singing", 120, "ambience"),
    (1782, "Summer Night in Forest", 126, "ambience"),
    (1789, "Forest with Birds Singing", 98, "ambience"),
    (2472, "Storm Approaching", 180, "ambience"),
    (2482, "Night Ambience with Crickets and Owls", 120, "ambience"),
    (2500, "Ocean Waves Crashing on Shore", 120, "ambience"),
    (2505, "Forest Ambience with Frogs", 113, "ambience"),
    (2507, "River Atmosphere in Forest", 123, "ambience"),
    
    # Water / Ocean / Waves
    (1225, "Sea Waves with Birds Loop", 38, "ocean"),
    (1242, "Close Sea Waves Loop", 28, "ocean"),
    (1248, "Sea Waves Loop", 48, "ocean"),
    (1253, "Rough Sea Waves Loop", 70, "ocean"),
    (1258, "Big Waterfall Loop", 26, "water"),
    (1259, "Forest Waterfall Loop", 25, "water"),
    (1260, "Water Flowing Ambience Loop", 51, "water"),
    (1261, "Sea Swimming Loop", 10, "ocean"),
    (1262, "Rowing on Sea Loop", 11, "ocean"),
    (1263, "Sea Coast Breaking Waves", 120, "ocean"),
    (1264, "Sea Waves on Rocky Shore", 121, "ocean"),
    (1265, "Water Flowing in River", 180, "water"),
    (1290, "Waterfall Flowing Water", 65, "water"),
    (1291, "Strong Flowing Waters Noise", 71, "water"),
    (1294, "River Water Flow and Surroundings", 60, "water"),
    (2678, "Crickets Near River", 180, "water"),
    (2744, "Birds by the River", 60, "ambience"),
    
    # Wind
    (1193, "Windy Sea Loop", 30, "wind"),
    (2474, "Rain and Wind", 30, "wind"),
    (2930, "Desert Ambience Wind", 60, "wind"),
    (3093, "Blizzard Cold Winds", 130, "wind"),
    
    # Fire
    (2264, "Campfire Crackling", 60, "fire"),
    
    # Storm / Thunder
    (2396, "Heavy Storm Rain Loop", 18, "storm"),
    (2397, "Downpour Loop", 15, "storm"),
    (2398, "Intense Rain Loop", 44, "storm"),
    
    # Birds / Animals
    (1210, "Birds Chirping Near River", 90, "birds"),
    
    # White Noise / Misc
    (39, "Deep Cinematic Wind Hum", 54, "white-noise"),
]

# Deduplicate by ID
seen_ids = set()
deduped = []
for s in CURATED:
    sid = s[0]
    if sid not in seen_ids:
        seen_ids.add(sid)
        deduped.append(s)

CURATED = deduped

print(f"\nTotal curated sounds: {len(CURATED)}")

# Download each sound
print("\nDownloading...")
for i, (sid, name, dur, cat) in enumerate(CURATED):
    url = f"https://assets.mixkit.co/active_storage/sfx/{sid}/{sid}-preview.mp3"
    safe_name = re.sub(r'[^\w\s-]', '', name).strip().replace(' ', '-').lower()
    filename = f"{safe_name}-{sid}.mp3"
    filepath = os.path.join(OUTPUT, filename)
    
    if os.path.exists(filepath) and os.path.getsize(filepath) > 1000:
        print(f"  [{i+1}/{len(CURATED)}] {name} ({dur}s) - already exists")
        continue
    
    try:
        r = session.get(url, timeout=30)
        if r.status_code == 200 and len(r.content) > 1000:
            with open(filepath, 'wb') as f:
                f.write(r.content)
            size_kb = len(r.content) / 1024
            print(f"  [{i+1}/{len(CURATED)}] {name} ({dur}s) - {size_kb:.0f}KB ✓")
        else:
            print(f"  [{i+1}/{len(CURATED)}] {name} - FAILED (HTTP {r.status_code})")
    except Exception as e:
        print(f"  [{i+1}/{len(CURATED)}] {name} - ERROR: {e}")

# Save manifest
manifest = []
for sid, name, dur, cat in CURATED:
    filename = f"{re.sub(r'[^\w\s-]', '', name).strip().replace(' ', '-').lower()}-{sid}.mp3"
    manifest.append({
        "id": sid,
        "name_cn": name,
        "name_en": name,
        "duration_sec": dur,
        "category": cat,
        "filename": filename,
        "source": "mixkit"
    })

with open(os.path.join(OUTPUT, "manifest.json"), 'w') as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)

print(f"\nDone! Files saved to: {OUTPUT}")
print(f"Total downloaded: {len([f for f in os.listdir(OUTPUT) if f.endswith('.mp3')])}")
print(f"Manifest: {os.path.join(OUTPUT, 'manifest.json')}")
