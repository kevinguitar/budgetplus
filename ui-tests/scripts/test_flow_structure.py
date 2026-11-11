"""Dependency-free guards for visibility-selector indentation in Maestro flows.

This checks a specific structural regression, not the full Maestro schema. Use
`maestro check-syntax <flow>` for full syntax validation without a device.
"""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]


def empty_visibility_conditions(source):
    lines = [(number, line) for number, line in enumerate(source.splitlines(), 1)
             if line.strip() and not line.lstrip().startswith("#")]
    errors = []
    for index, (number, line) in enumerate(lines):
        match = re.fullmatch(r"( *)(?:visible|notVisible):\s*(?:#.*)?", line)
        if not match:
            continue
        indent = len(match.group(1))
        following = lines[index + 1][1] if index + 1 < len(lines) else ""
        if not following or len(following) - len(following.lstrip()) <= indent:
            errors.append(number)
    return errors


class FlowStructureTests(unittest.TestCase):
    def test_all_flow_visibility_conditions_have_nested_selectors(self):
        errors = []
        for path in sorted((ROOT / "ui-tests").rglob("*.yml")):
            errors.extend(f"{path.relative_to(ROOT)}:{line}: visibility condition has no nested selector"
                          for line in empty_visibility_conditions(path.read_text()))
        self.assertEqual([], errors, "\n".join(errors))

    def test_rejects_dedented_visible_selector_in_nested_commands(self):
        source = """- retry:
    commands:
      - extendedWaitUntil:
          visible:
      text: "Missing book"
          timeout: 15000
"""
        self.assertEqual([4], empty_visibility_conditions(source))

    def test_rejects_not_visible_selector_as_a_sibling(self):
        source = """- extendedWaitUntil:
    notVisible:
    text: "Past Year"
    timeout: 15000
"""
        self.assertEqual([2], empty_visibility_conditions(source))

    def test_accepts_nested_and_inline_conditions_with_comments(self):
        source = """- extendedWaitUntil:
    visible: # The selector can follow a comment or blank line.

      # Use the visible label.
      text: "Missing book"
    timeout: 15000
- extendedWaitUntil:
    notVisible:
      text: "Past Year"
- runFlow:
    when:
      visible: "Past Month"
    commands:
      - tapOn: "Past Month"
"""
        self.assertEqual([], empty_visibility_conditions(source))

    def test_rejects_empty_condition_at_end_of_file(self):
        self.assertEqual([2], empty_visibility_conditions("- extendedWaitUntil:\n    visible:\n"))


if __name__ == "__main__":
    unittest.main()
