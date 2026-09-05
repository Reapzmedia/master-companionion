import subprocess
import json
import urllib.request
import urllib.error
import os
import sys

def get_git_token():
    proc = subprocess.Popen(['git', 'credential', 'fill'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    out, _ = proc.communicate(input="protocol=https\nhost=github.com\n")
    token = None
    for line in out.splitlines():
        if line.startswith("password="):
            token = line.split("=", 1)[1].strip()
            break
    return token

def main():
    token = get_git_token()
    if not token:
        print("ERROR: Could not retrieve GitHub token from git credentials.", file=sys.stderr)
        sys.exit(1)

    repo = "Reapzmedia/master-companionion"
    tag = "v1.0.1"
    name = "Master Companion v1.0.1 - Hotfix & Telemetry"

    notes_file = os.path.join(os.path.dirname(__file__), "..", "..", "RELEASE_NOTES.md")
    body = "Release " + tag
    if os.path.exists(notes_file):
        with open(notes_file, "r", encoding="utf-8") as f:
            body = f.read()

    print(f"Creating GitHub release {tag} on {repo}...")
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "MasterCompanion-ReleaseScript",
        "Content-Type": "application/json"
    }

    payload = {
        "tag_name": tag,
        "name": name,
        "body": body,
        "draft": False,
        "prerelease": False
    }

    req = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/releases",
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST"
    )

    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            release_id = data["id"]
            html_url = data.get("html_url", "")
            print(f"Release created successfully (ID: {release_id})")
            print(f"Release URL: {html_url}")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        print(f"HTTP Error creating release: {e.code} - {err_msg}", file=sys.stderr)
        # Check if release already exists
        if e.code == 422:
            print("Fetching existing release for tag...")
            req_get = urllib.request.Request(
                f"https://api.github.com/repos/{repo}/releases/tags/{tag}",
                headers=headers,
                method="GET"
            )
            with urllib.request.urlopen(req_get) as resp_get:
                data = json.loads(resp_get.read().decode("utf-8"))
                release_id = data["id"]
                html_url = data.get("html_url", "")
        else:
            sys.exit(1)

    apk_path = os.path.join(os.path.dirname(__file__), "..", "..", "app", "build", "outputs", "apk", "release", "app-release.apk")
    if not os.path.exists(apk_path):
        print(f"ERROR: APK not found at {apk_path}", file=sys.stderr)
        sys.exit(1)

    size = os.path.getsize(apk_path)
    print(f"Uploading app-release.apk ({size:,} bytes) to release {release_id}...")

    upload_url = f"https://uploads.github.com/repos/{repo}/releases/{release_id}/assets?name=app-release.apk"
    upload_headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "MasterCompanion-ReleaseScript",
        "Content-Type": "application/vnd.android.package-archive"
    }

    with open(apk_path, "rb") as apk_f:
        apk_bytes = apk_f.read()

    upload_req = urllib.request.Request(upload_url, data=apk_bytes, headers=upload_headers, method="POST")
    try:
        with urllib.request.urlopen(upload_req) as upload_resp:
            upload_data = json.loads(upload_resp.read().decode("utf-8"))
            print("Asset uploaded successfully!")
            print(f"Browser download URL: {upload_data.get('browser_download_url')}")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        print(f"HTTP Error uploading asset: {e.code} - {err_msg}", file=sys.stderr)
        sys.exit(1)

    print("\nSUCCESS: Master Companion v1.0.1 is published on GitHub with app-release.apk attached!")

if __name__ == "__main__":
    main()
