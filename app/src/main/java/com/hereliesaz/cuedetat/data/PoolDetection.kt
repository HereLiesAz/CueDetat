package com.hereliesaz.cuedetat.data

import android.graphics.RectF

/**
 * One YOLO detection from the pocket/pool TFLite model.
 *
 * Class IDs come from the trained model's `data.yaml` (see also ml/metadata.yaml
 * and ml/training_report-pocket_detector.md):
 *   0 = pool-table   1 = pool-table-hole   2 = pool-table-side
 *
 * This is the ONLY class scheme this model was ever trained on. Both heads baked
 * into the master TFLite binary (segment 0 / HEAD_POCKET_50E, read by
 * [MergedTFLiteDetector.detect]; and segment 2 / HEAD_POOL_PIVOT, read by
 * [MergedTFLiteDetector.detectPool]) emit these same three classes — there is no
 * separate ball/cue-trained head. ml/cuedetat_pocket_detector_kaggle.py merges
 * ball/cue source datasets against `MASTER_CLASSES = ["pool-table",
 * "pool-table-hole", "pool-table-side"]` and always skips them for having no
 * class-name overlap, so no ball/cue class was ever actually trained. Do not
 * reinterpret classId 1/2 as "ball"/"cue" — VisionRepository previously did this
 * and it was a bug (fixed; see the notes there). Real per-frame ball detection
 * comes from the generic ML object detector plus [CvBallDetector]'s classical-CV
 * Hough-circle refinement.
 */
data class PoolDetection(
    val rect: RectF,
    val confidence: Float,
    val classId: Int,
)
