#!/usr/bin/env bash
# List upstream Grupetto commits we don't have yet, grouped by how much they matter
# for Pelo. Commits already cherry-picked onto pelo are left out.
#
#   scripts/upstream-check.sh          commits not yet in pelo
#   scripts/upstream-check.sh -v       also list the files each commit touches
#
# To take a fix:  git cherry-pick <sha>   (while on the pelo branch)
set -euo pipefail
cd "$(dirname "$0")/.."

verbose=false
[[ "${1:-}" == "-v" ]] && verbose=true

UPSTREAM_BRANCH=upstream/develop
BASE="${BASE:-pelo}" # compare against another ref (e.g. to test this script)
SRC=app/src/main/java/com/spop/poverlay

git fetch --quiet upstream

# Commits on upstream that aren't in pelo, ignoring ones whose change is already here.
mapfile -t commits < <(git log --cherry-pick --right-only --no-merges --format='%h' "$BASE...$UPSTREAM_BRANCH")

declare -A groups=([important]="" [overlay]="" [other]="")
count=0
for sha in "${commits[@]}"; do
    files="$(git show --format= --name-only "$sha")"
    subject="$(git log -1 --format='%ad  %s' --date=short "$sha")"
    # Skip upstream's automatic version bumps.
    [[ "$subject" == *"Auto-update version"* ]] && continue
    count=$((count + 1))

    if grep -qE "^$SRC/(sensor|ble|dircon)/" <<<"$files"; then
        group=important
    elif grep -qE "^$SRC/(overlay|ui)/|^$SRC/(ConfigurationPage|MainActivity)\.kt" <<<"$files"; then
        group=overlay
    else
        group=other
    fi
    line="  $sha  $subject"
    if $verbose; then
        line+=$'\n'"$(sed 's/^/        /' <<<"$files")"
    fi
    groups[$group]+="$line"$'\n'
done

if [[ $count -eq 0 ]]; then
    echo "Up to date: no upstream commits to review."
    exit 0
fi

echo "$count upstream commit(s) not in $BASE:"
if [[ -n "${groups[important]}" ]]; then
    echo
    echo "Sensor, Bluetooth, heart rate: worth reviewing and usually safe to cherry-pick"
    printf '%s' "${groups[important]}"
fi
if [[ -n "${groups[overlay]}" ]]; then
    echo
    echo "Overlay and settings UI: Pelo replaced these, so expect conflicts; usually skip"
    printf '%s' "${groups[overlay]}"
fi
if [[ -n "${groups[other]}" ]]; then
    echo
    echo "Other (build, docs, other Peloton models)"
    printf '%s' "${groups[other]}"
fi
echo
echo "Take a fix with: git cherry-pick <sha>   (use -v to see which files each commit touches)"
