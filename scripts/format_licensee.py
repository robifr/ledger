#
# Copyright 2025 Robi
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

import argparse
import json
import os
import re
from dataclasses import dataclass, field
from typing import List, Tuple, Optional

def main():
  parser = argparse.ArgumentParser("Convert Licensee artifacts JSON data to formatted text")
  parser.add_argument("--input-file", type = str, help = "Path to the input JSON file")
  parser.add_argument("--output-file", type = str, help = "Path to the output TXT file")
  args = parser.parse_args()

  with open(args.input_file, "r") as infile:
    data = json.load(infile)

  with open(args.output_file, "w") as outfile:
    for index, artifact in enumerate(data):
      outfile.write(Artifact.with_artifacts(artifact).format())
      if index < len(data) - 1:
        outfile.write("\n\n")

    # Add unlisted libraries to the formatted data.
    libraries_to_add = unlisted_libraries()
    if (len(libraries_to_add) > 0):
      outfile.write("\n\n")
    for index, artifact in enumerate(libraries_to_add):
      outfile.write(artifact.format())
      if index < len(libraries_to_add) - 1:
        outfile.write("\n\n")

@dataclass
class Artifact:
  artifact_id: str
  version: Optional[str] = None
  scm_url: Optional[str] = None
  spdx_licenses: List[Tuple[str, str]] = field(default_factory = list)
  unknown_licenses: List[Tuple[str, str]] = field(default_factory = list)

  @staticmethod
  def with_artifacts(artifacts_json: str) -> "Artifact":
    """
    Format generated Licensee `artifact.json` to a plain text.
    The original JSON type is based from `app.cash.licensee.ArtifactDetail.kt`.
    ```kt
    ArtifactDetail {
      groupId: String,
      artifactId: String,
      version: String,
      name: String?,
      spdxLicenses: Set<SpdxLicense> =
        SpdxLicense {
          identifier: String,
          name: String,
          url: String
        },
      unknownLicenses: Set<UnknownLicense> =
        UnknownLicense {
          name: String?,
          url: String?
        },
      scm: ArtifactScm? =
        ArtifactScm {
          url: String
        }
    }
    ```
    """
    return Artifact(
      artifact_id = artifacts_json.get("artifactId", ""),
      version = artifacts_json.get("version", ""),
      scm_url = artifacts_json.get("scm", {}).get("url", None),
      spdx_licenses = [
        (license.get("name", ""), license.get("url", ""))
        for license in artifacts_json.get("spdxLicenses", [])
      ],
      unknown_licenses = [
        (license.get("name", ""), license.get("url", ""))
        for license in artifacts_json.get("unknownLicenses", [])
      ]
    )

  def format(self) -> str:
    result = f"{self.artifact_id} v{self.version}" if self.version else self.artifact_id
    if self.scm_url:
      result += f"\n{self.scm_url}"

    for name, url in self.spdx_licenses:
      result += f"\n * {name} ({url})"
    for name, url in self.unknown_licenses:
      result += f"\n * {name} ({url})"
    return result

def obtain_versions_from_version_catalog() -> List[Tuple[str, str]]:
  """
  Load versions from the `libs.versions.toml` file without using any libraries.
  Returns a dictionary of `artifact_name: version`.
  """
  script_dir = os.path.dirname(os.path.realpath(__file__))
  file_path = os.path.join(script_dir, "./../gradle", "libs.versions.toml")

  versions = {}
  section_pattern = re.compile(r"^\[.*\]") # Match any section header like `[this]`.
  with open(file_path) as infile:
    for line in infile:
      line = line.strip()
      if section_pattern.match(line):
        if line != "[versions]":
          break
      # Add all artifacts found in the `[versions]` section.
      if "=" in line:
        artifact_name, version = map(str.strip, line.split("=", 1))
        versions[artifact_name] = version.strip('"')
  return versions

def unlisted_libraries() -> List[Artifact]:
  """
  Return a list of libraries that aren't covered in the `artifacts.json`.
  Visit https://spdx.org/licenses for reference to license names and their URLs.
  """
  return [
    Artifact(
      artifact_id = "d3",
      version = obtain_versions_from_version_catalog().get("d3", None),
      scm_url = "https://github.com/d3/d3",
      spdx_licenses = [("ISC License", "https://www.isc.org/licenses")]
    ),
    Artifact(
      artifact_id = "roboto",
      scm_url = "https://fonts.google.com/specimen/Roboto",
      spdx_licenses = [("SIL Open Font License 1.1", "https://openfontlicense.org")]
    )
  ]

if __name__ == "__main__":
  main()