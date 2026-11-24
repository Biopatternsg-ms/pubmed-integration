#!/usr/bin/env bash
#
# Copyright © 2026 biopatternsg (biopatternsg@gmail.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


set -e

SCRIPT_DIR="$(CDPATH="" cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

JSON_MODE=false
TEMPLATE_NAME=""

for arg in "$@"; do
    case "$arg" in
        --json) JSON_MODE=true ;;
        --help|-h)
            echo "Usage: $0 <template-name> [--json]"
            exit 0
            ;;
        -*)
            echo "ERROR: Unknown option '$arg'" >&2
            exit 1
            ;;
        *)
            if [[ -n "$TEMPLATE_NAME" ]]; then
                echo "ERROR: Unexpected argument '$arg'" >&2
                exit 1
            fi
            TEMPLATE_NAME="$arg"
            ;;
    esac
done

if [[ -z "$TEMPLATE_NAME" ]]; then
    echo "ERROR: Template name is required" >&2
    exit 1
fi

REPO_ROOT=$(get_repo_root)
if TEMPLATE_CONTENT=$(resolve_template_content "$TEMPLATE_NAME" "$REPO_ROOT"; status=$?; printf x; exit "$status"); then
    TEMPLATE_CONTENT="${TEMPLATE_CONTENT%x}"
else
    echo "ERROR: Could not resolve required $TEMPLATE_NAME from the template override stack for $REPO_ROOT" >&2
    exit 1
fi

if $JSON_MODE; then
    if has_jq; then
        jq -cn \
            --arg template_name "$TEMPLATE_NAME" \
            --arg template_content "$TEMPLATE_CONTENT" \
            '{TEMPLATE_NAME:$template_name,TEMPLATE_CONTENT:$template_content}'
    else
        printf '{"TEMPLATE_NAME":"%s","TEMPLATE_CONTENT":"%s"}\n' \
            "$(json_escape "$TEMPLATE_NAME")" "$(json_escape "$TEMPLATE_CONTENT")"
    fi
else
    printf '%s' "$TEMPLATE_CONTENT"
fi
