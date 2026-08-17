# ML/Pocket-Detection Asset Snapshot

This document previously claimed a completed asset-optimization cleanup (removing training
residue from `ml/`, unifying models into `merged_pocket_detector_final*` files). That cleanup
never actually happened — the files it named don't exist, and the "removed" training residue is
still present in the repo. This is a corrected, current snapshot instead.

## Shipped runtime asset

The only ML asset actually bundled with the app is the merged detector consumed by
`MergedTFLiteDetector` (`app/src/main/java/com/hereliesaz/cuedetat/data/MergedTFLiteDetector.kt`),
which implements the `PocketDetector` interface (`app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/PocketDetector.kt`).
There is no `TfLitePocketDetector` class.

| Asset | Path | Size |
| :--- | :--- | :--- |
| `MASTER_POOL_MODEL.tflite` | `feature_mlmodel/src/main/assets/ml/` | ~23.8 MB |
| `MASTER_POOL_MODEL.tflite.meta` | `feature_mlmodel/src/main/assets/ml/` | < 1 KB |

`MASTER_POOL_MODEL.tflite` is a single binary concatenating several YOLOv8n sub-model heads
(FP16 weights, in-graph NMS); `MergedTFLiteDetector` only loads the heads it actually uses. It's
delivered as an on-demand dynamic feature module (`:feature_mlmodel`) for Play builds and bundled
directly into the FOSS APK (see `README.md`).

## `ml/` directory — training residue, not removed

The repo-root `ml/` directory (~77 MB) is training scratch space, not a runtime asset source, and
none of it ships in the app. It has **not** been cleaned up; as of this writing it still contains:

* PyTorch/ONNX/TFLite export copies: `best.pt`, `best.onnx`, `best_float16.tflite`,
  `best_float32.tflite`, `pocket_detector_final.onnx`, `pocket_detector_fp16.tflite`,
  `pocket_detector_fp16 (2).tflite`
* SavedModel export residue: `saved_model.pb`, `fingerprint.pb`, `variables.data-00000-of-00001`,
  `variables.index`
* Training notebooks/scripts: `Copy_of_pocket_detector_training.ipynb`, `cuedetatai.ipynb`,
  `cuedetat_pocket_detector_kaggle.ipynb`, `cuedetat_pocket_detector_kaggle.py`,
  `notebook8de5066986.ipynb`, `pocket_detector_training.ipynb`, `_scripts/py_to_ipynb.py`
* Misc training artifacts: `args.yaml`, `metadata.yaml`, `results.csv`, `labels.jpg`,
  `training_report-pocket_detector.md`, `calibration_image_sample_data_20x128x128x3_float32.npy`,
  `_archive/myriad_server.py`

None of these filenames (including `merged_pocket_detector_final.onnx` and
`merged_pocket_detector_final_float16.tflite`, which this document previously claimed existed)
correspond to real files in the repo. If a genuine cleanup of `ml/` is done in the future, this
document should be updated again to reflect what was actually removed — not what was intended.
