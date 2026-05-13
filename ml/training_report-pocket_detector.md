# Pocket Detector Training Report

Generated: 2026-05-13 08:05:34.507584

## Hyperparameters
- Model: YOLOv8n
- Epochs: 100 (early stop patience 30)
- Batch: 32
- Image size: 640
- AMP: True

## Datasets
- hereliesaz/cue-detat
- diveshcrazy/pool-table-balls-classification
- vedester/pool-table-v3i-yolov8 (YOLO training split)
- akaliutau/dynavisr (benchmark reference)
- alizaib001/pool-balls-on-table (smoke-test image)

## Pre-trained model (warm-start)
- hereliesaz/pocket-detector (TFLite + ONNX previous weights)

## Classes
['pool-table', 'pool-table-hole', 'pool-table-side']

## Validation metrics
- mAP50:    0.9908
- mAP50-95: 0.8130
- Precision: 0.9631
- Recall:    0.9781

## Exports
- /kaggle/working/exports/pocket_detector_fp16.tflite
- /kaggle/working/exports/pocket_detector_final.onnx

## Deploy locations (in CueDetat repo)
- app/src/main/assets/ml/merged_pocket_detector_final_float16.tflite
- app/src/main/assets/ml/merged_pocket_detector_final.onnx
