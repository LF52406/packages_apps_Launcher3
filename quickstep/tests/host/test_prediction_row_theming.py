"""Host source regression tests for All Apps prediction icon theme/shape parity.

Run from the Launcher3 checkout root:
    python3 -B -m unittest discover -s quickstep/tests/host -p 'test_prediction_row_theming.py' -v

These tests verify source integration; Android runtime rendering needs device testing.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
BUBBLE = ROOT / "src/com/android/launcher3/BubbleTextView.java"
PREDICTION = (
    ROOT / "quickstep/src/com/android/launcher3/appprediction/PredictionRowView.java"
)
PREDICTION_LAYOUT = ROOT / "res/layout/all_apps_prediction_row_icon.xml"
ALL_APPS_LAYOUT = ROOT / "res/layout/all_apps_icon.xml"
NS = "{http://schemas.android.com/apk/res-auto}"


class PredictionRowThemeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.bubble = BUBBLE.read_text()
        cls.pred = PREDICTION.read_text()

    def test_prediction_layout_retains_all_apps_style_and_display(self):
        pred = ET.parse(PREDICTION_LAYOUT).getroot()
        allapps = ET.parse(ALL_APPS_LAYOUT).getroot()
        self.assertEqual(pred.attrib["style"], allapps.attrib["style"])
        self.assertEqual(pred.attrib[NS + "iconDisplay"], "prediction_row")
        self.assertEqual(allapps.attrib[NS + "iconDisplay"], "all_apps")

    def test_theme_preference_loaded_for_prediction_icons(self):
        initialization = self.bubble.split(
            "mDisplay == DISPLAY_PREDICTION_ROW", 1
        )[1].split("} else if (mDisplay == DISPLAY_FOLDER)", 1)[0]
        self.assertIn("mThemeAllAppsIcons = ALLAPPS_THEMED_ICONS.get(context)", initialization)

    def test_prediction_and_all_apps_share_monet_theme_flag(self):
        block = self.bubble.split("protected boolean shouldUseTheme()", 1)[1].split(
            "\n    }", 1
        )[0]
        self.assertIn("mThemeAllAppsIcons", block)
        self.assertIn("mDisplay == DISPLAY_ALL_APPS", block)
        self.assertIn("mDisplay == DISPLAY_PREDICTION_ROW", block)
        self.assertIn("shouldUseTheme() ? FLAG_THEMED : 0", self.bubble)

    def test_custom_icon_shape_uses_shared_item_pipeline(self):
        self.assertIn("info.newIcon(getContext(), getIconCreationFlagsForInfo(info))", self.bubble)
        self.assertIn("setStandardIcon(info)", self.bubble)
        self.assertIn("icon.applyFromWorkspaceItem(predictedItem)", self.pred)

    def test_prediction_loads_matching_themed_cached_bitmap(self):
        bind = self.pred.split(
            "icon.applyFromWorkspaceItem(predictedItem);", 1
        )[1].split("} else {", 1)[0]
        self.assertIn("icon.verifyHighRes();", bind)
        verify = self.bubble.split("public void verifyHighRes()", 1)[1].split(
            "\n    }", 1
        )[0]
        self.assertIn("DEFAULT_LOOKUP_FLAG.withThemeIcon(shouldUseTheme())", verify)
        self.assertIn("updateIconInBackground(", verify)

    def test_prediction_notification_dots_use_all_apps_geometry(self):
        self.assertIn(
            "if (mDisplay == DISPLAY_ALL_APPS || mDisplay == DISPLAY_PREDICTION_ROW)",
            self.bubble,
        )
        self.assertIn("getAllAppsProfile().getIconSizePx()", self.bubble)

    def test_no_prediction_only_monet_colors_or_fixed_shape(self):
        # Keep palette and shapes sourced from the existing theme/cache pipeline.
        self.assertNotIn("Color.parseColor(", self.pred)
        self.assertNotIn("setClipToOutline(", self.pred)


if __name__ == "__main__":
    unittest.main(verbosity=2)
