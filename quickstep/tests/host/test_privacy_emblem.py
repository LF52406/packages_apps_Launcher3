"""Pure-source regression checks for the subtle privacy indicator in Recents.

Run from Launcher3 root: python3 -m unittest discover -s quickstep/tests/host -v
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "quickstep/src/com/android/quickstep/task/thumbnail/TaskThumbnailView.kt"
LAYOUT = ROOT / "quickstep/res/layout/task_thumbnail.xml"
VECTOR = ROOT / "quickstep/res/drawable/ic_recents_privacy_emblem.xml"
A = "{http://schemas.android.com/apk/res/android}"


class PrivacyEmblemTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.code = SRC.read_text()
        cls.root = ET.parse(LAYOUT).getroot()
        cls.vector = ET.parse(VECTOR).getroot()

    def test_only_one_centered_noninteractive_emblem(self):
        matching = [
            e for e in cls_children(self.root)
            if e.attrib.get(A + "id") == "@+id/task_thumbnail_privacy_emblem"
        ]
        self.assertEqual(len(matching), 1)
        v = matching[0]
        self.assertEqual(v.attrib[A + "layout_gravity"], "center")
        self.assertEqual(v.attrib[A + "visibility"], "invisible")
        self.assertEqual(v.attrib[A + "importantForAccessibility"], "no")
        self.assertEqual(v.attrib[A + "clickable"], "false")
        self.assertEqual(v.attrib[A + "focusable"], "false")
        self.assertLessEqual(float(v.attrib[A + "alpha"]), 0.60)

    def test_draw_order_above_privacy_scrim_not_touchable(self):
        children = cls_children(self.root)
        ids = [v.attrib.get(A + "id") for v in children]
        self.assertLess(ids.index("@+id/task_thumbnail_privacy_scrim"),
                        ids.index("@+id/task_thumbnail_privacy_emblem"))
        self.assertLess(ids.index("@+id/task_thumbnail_scrim"),
                        ids.index("@+id/task_thumbnail_privacy_emblem"))

    def test_state_scoped_and_reset_on_recycle(self):
        source = self.code
        self.assertIn("is PrivacyBlur -> drawPrivacyBlur(state)", source)
        reset = source.split("private fun resetViews()", 1)[1].split("private fun drawBackground", 1)[0]
        self.assertIn("privacyEmblemView.isInvisible = true", reset)
        blur = source.split("private fun drawPrivacyBlur(state: PrivacyBlur)", 1)[1].split("private fun drawAppLocked", 1)[0]
        self.assertIn("privacyEmblemView.isInvisible = false", blur)
        self.assertIn("override fun onRecycle()", source)
        self.assertIn("resetViews()", source)
        self.assertNotIn("privacyEmblemView.isInvisible = false", source.split("private fun drawAppLocked", 1)[1])

    def test_vector_is_original_two_facet_no_backplate(self):
        self.assertEqual(self.vector.tag, "vector")
        paths = [c for c in self.vector if c.tag == "path"]
        self.assertEqual(len(paths), 2)
        self.assertNotIn("rectangle", VECTOR.read_text())


def cls_children(root):
    return list(root)


if __name__ == "__main__":
    unittest.main(verbosity=2)
