# Cue d'Etat: Training Analysis Report
Generated: 2026-03-27 07:33:59

## 1. Dataset Inventory
- **Project Root:** `/content/drive/MyDrive/billiards_training`
- **Combined Dataset:** `/content/drive/MyDrive/billiards_training/datasets/combined_dataset`
- **Source Datasets Detected:**
  - combined_dataset
- **Kaggle Source:** `N/A`

## 2. Model Configuration
- **Architecture:** YOLOv8n
- **Target Classes:** ["pool-table", "pool-table-hole", "pool-table-side"]
- **Input Resolution:** 640x640
- **Training Schedule:** 100 Epochs / Batch 32

## 3. Artifact Locations
- **Final Weights:** `/content/drive/MyDrive/billiards_training/pocket_detector/weights/best.pt`
- **TFLite Export:** `/content/drive/MyDrive/billiards_training/exports/pocket_detector_fp16.tflite`

## 4. Automated Review & Analysis
### Model Performance Critique
- **mAP50:** 0.8123
- **Status:** High confidence model. Ready for edge deployment.

### Optimization Suggestions
1. **Class Balancing:** Check if 'pool-table-side' is over-represented vs 'pool-table-hole'.
2. **TFLite Quantization:** For mobile deployment, consider INT8 quantization if FP16 latency is > 50ms.
3. **Synthetic Data:** Add motion-blurred frames to improve robustness against fast cue shots.
