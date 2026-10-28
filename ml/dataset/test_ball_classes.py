"""Run: python3 -m unittest test_ball_classes   (from ml/dataset; no third-party packages)."""
import os
import tempfile
import unittest

import ball_classes as bc
import make_ball_dataset as mk


class TypeMapping(unittest.TestCase):
    def test_labels_are_the_ones_ls_to_yolo_writes(self):
        # cue first, 1..15 in order, then the unknown-number class: the index is the YOLO id.
        self.assertEqual(bc.BALL_LABELS[0], "cue")
        self.assertEqual(bc.BALL_LABELS[1:16], [str(n) for n in range(1, 16)])
        self.assertEqual(bc.BALL_LABELS[16], "ball")
        self.assertEqual(len(bc.BALL_LABELS), 17)

    def test_numbers_collapse_the_way_the_rules_of_pool_say(self):
        # Independent reference: 1-7 solids, 8 the eight ball, 9-15 stripes.
        for n in range(1, 8):
            self.assertEqual(bc.type_name(str(n)), "solid", n)
        self.assertEqual(bc.type_name("8"), "eight")
        for n in range(9, 16):
            self.assertEqual(bc.type_name(str(n)), "stripe", n)
        self.assertEqual(bc.type_name("cue"), "cue")
        self.assertIsNone(bc.type_name("ball"))

    def test_type_names_match_the_apps_balltype_order(self):
        self.assertEqual(bc.TYPE_NAMES, ["cue", "eight", "solid", "stripe"])


class Remap(unittest.TestCase):
    def line(self, cls, *rest):
        return " ".join([str(cls), *rest])

    def test_type_scheme_rewrites_ids_and_keeps_boxes(self):
        lines = [
            "0 0.1 0.2 0.05 0.05",    # cue
            "3 0.3 0.4 0.05 0.05",    # 3: solid
            "8 0.5 0.6 0.05 0.05",    # 8
            "12 0.7 0.8 0.05 0.05",   # 12: stripe
        ]
        out, unknown = bc.remap_lines(lines, bc.SCHEME_TYPE)
        self.assertEqual(unknown, 0)
        self.assertEqual([l.split()[0] for l in out], ["0", "2", "1", "3"])
        self.assertEqual(out[1].split()[1:], ["0.3", "0.4", "0.05", "0.05"])

    def test_unknown_number_has_no_type_and_is_counted(self):
        out, unknown = bc.remap_lines(["16 0.5 0.5 0.1 0.1", "0 0.2 0.2 0.1 0.1"], bc.SCHEME_TYPE)
        self.assertEqual(unknown, 1)
        self.assertEqual(len(out), 1)

    def test_number_scheme_keeps_everything(self):
        lines = ["16 0.5 0.5 0.1 0.1", "4 0.2 0.2 0.1 0.1"]
        out, unknown = bc.remap_lines(lines, bc.SCHEME_NUMBER)
        self.assertEqual(out, lines)
        self.assertEqual(unknown, 1)

    def test_blank_lines_are_ignored_and_a_bad_scheme_refused(self):
        self.assertEqual(bc.remap_lines(["", "  "], bc.SCHEME_TYPE), ([], 0))
        with self.assertRaises(ValueError):
            bc.remap_lines([], "nope")


class MakeDataset(unittest.TestCase):
    def build(self, root):
        for split in ("train", "val"):
            os.makedirs(os.path.join(root, "images", split))
            os.makedirs(os.path.join(root, "labels", split))

        def add(split, stem, labels):
            with open(os.path.join(root, "images", split, stem + ".jpg"), "wb") as f:
                f.write(b"x")
            with open(os.path.join(root, "labels", split, stem + ".txt"), "w") as f:
                f.write("\n".join(labels) + ("\n" if labels else ""))

        add("train", "a", ["0 0.1 0.1 0.1 0.1", "9 0.2 0.2 0.1 0.1"])   # cue + stripe
        add("train", "b", ["16 0.1 0.1 0.1 0.1", "1 0.2 0.2 0.1 0.1"])  # has an unknown ball
        add("val", "c", [])                                             # a real negative

    def test_images_with_an_unknown_ball_are_dropped_whole_by_default(self):
        with tempfile.TemporaryDirectory() as src, tempfile.TemporaryDirectory() as out:
            self.build(src)
            counts, per_class = mk.convert(src, out, bc.SCHEME_TYPE, "drop-image")
            self.assertEqual(counts["images"], 2)           # a and c
            self.assertEqual(counts["dropped_images"], 1)   # b
            self.assertFalse(os.path.exists(os.path.join(out, "images", "train", "b.jpg")))
            self.assertEqual(per_class, {"cue": 1, "eight": 0, "solid": 0, "stripe": 1})

    def test_keep_policy_keeps_the_image_and_drops_only_the_box(self):
        with tempfile.TemporaryDirectory() as src, tempfile.TemporaryDirectory() as out:
            self.build(src)
            counts, per_class = mk.convert(src, out, bc.SCHEME_TYPE, "keep")
            self.assertEqual(counts["images"], 3)
            self.assertEqual(counts["dropped_boxes"], 1)
            self.assertEqual(per_class["solid"], 1)  # b's ball 1

    def test_negatives_survive_and_data_yaml_names_the_classes(self):
        with tempfile.TemporaryDirectory() as src, tempfile.TemporaryDirectory() as out:
            self.build(src)
            mk.convert(src, out, bc.SCHEME_TYPE, "drop-image")
            self.assertTrue(os.path.exists(os.path.join(out, "labels", "val", "c.txt")))
            with open(os.path.join(out, "labels", "val", "c.txt")) as f:
                self.assertEqual(f.read(), "")
            with open(os.path.join(out, "data.yaml")) as f:
                text = f.read()
            for i, n in enumerate(bc.TYPE_NAMES):
                self.assertIn(f"  {i}: '{n}'", text)


if __name__ == "__main__":
    unittest.main()
