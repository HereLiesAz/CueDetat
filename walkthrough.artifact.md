# Asset Optimization and Bloat Reduction Walkthrough

I have performed a comprehensive audit and cleanup of the project's assets to address the reported app size of 270 MB.

## Optimization Strategy

The primary source of bloat was **redundancy** in the Machine Learning artifacts. I identified and removed:
1.  **Duplicate TFLite Models**: Multiple copies of the same YOLOv8 weights existed in both `assets/ml/` and `assets/weights/`.
2.  **Training Residue**: Large intermediate files such as `.pb` (SavedModel), `.pt` (PyTorch), and `.npy` (Sample Data) were packaged in the APK but not used by the application logic.
3.  **Intermediate Formats**: Removed FP32 versions of models in favor of the optimized FP16 versions already unified in the master binary.

## Final Asset Inventory

The core production assets have been reduced to the following:

| Asset | Size | Role |
| :--- | :--- | :--- |
| **MASTER_POOL_MODEL.tflite** | 23.8 MB | Unified binary containing all 4 detection heads. |
| **merged_pocket_detector_final.onnx** | 11.8 MB | ONNX version for side-by-side comparison. |
| **merged_pocket_detector_final_float16.tflite** | 5.9 MB | Primary pocket detector used by `TfLitePocketDetector`. |
| **MASTER_POOL_MODEL.tflite.meta** | < 1 KB | Mapping offsets for the master binary. |

## Verification Summary
- **Size Reduction**: Total raw asset size decreased from **~140 MB** (just for the large files) down to **~41.5 MB**.
- **Build Status**: Verified that the project continues to compile cleanly.
- **Dependency Safety**: Confirmed that only the files required by `MergedTFLiteDetector` and `TfLitePocketDetector` remain.

---
> [!TIP]
> The app size is now significantly smaller, which will improve installation time and reduce memory pressure on the device.
