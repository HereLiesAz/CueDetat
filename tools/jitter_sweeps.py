#!/usr/bin/env python3
"""
Synthetic-signal sweeps behind the constants in the stabilisation filters.

These are Python ports of three Kotlin classes, used to choose and to check their constants on signals
whose truth is known. They are NOT the shipped code: if a Kotlin class changes, change its port here
and run this again. Real sensor and tracking noise is not white and not Gaussian, so the *feel* of
every filter is still to be checked on a device.

Ports:
  - OrientationFilter  (app/.../domain/OrientationFilter.kt)       one-euro per axis, time based
  - ScreenCornerSmoother (app/.../domain/ScreenCornerSmoother.kt)  one speed per quad, noise floor, hold
  - TablePull          (app/.../domain/TableSnapPolicy.kt)         smoothed target, hysteresis, time based

"original" below is the corner filter as first shipped (per-coordinate speed from the filter's lag over dt);
"legacy-like" is the same constants on the raw-difference speed, which is what the Kotlin test's legacy() builds.

Run:  python3 tools/jitter_sweeps.py
"""
import math
import random
import statistics


def alpha(cutoff_hz, dt):
    tau = 1 / (2 * math.pi * cutoff_hz)
    return dt / (dt + tau)


# --------------------------------------------------------------------------------------------------
# OrientationFilter
# --------------------------------------------------------------------------------------------------
class Axis:
    def __init__(self, wraps):
        self.wraps, self.started = wraps, False
        self.value = self.prev = self.speed = 0.0
        self.t = 0

    def diff(self, a, b):
        d = a - b
        if self.wraps:
            d %= 360
            if d > 180:
                d -= 360
            if d < -180:
                d += 360
        return d


def orientation_step(ax, raw, t, minc=0.1, beta=0.25, maxc=8.0, dcut=1.0):
    dt = (t - ax.t) / 1e9
    if ax.started and dt <= 0:
        return ax.value
    if not ax.started or dt > 1.0:
        ax.started, ax.value, ax.prev, ax.speed, ax.t = True, raw, raw, 0.0, t
        return raw
    ax.speed += alpha(dcut, dt) * (ax.diff(raw, ax.prev) / dt - ax.speed)
    cutoff = min(maxc, minc + beta * abs(ax.speed))
    ax.value += alpha(cutoff, dt) * ax.diff(raw, ax.value)
    if ax.wraps:
        if ax.value > 180:
            ax.value -= 360
        if ax.value <= -180:
            ax.value += 360
    ax.prev, ax.t = raw, t
    return ax.value


def orientation_report():
    def run(signal, hz, secs, noise, seed=1, beta=0.25):
        r, ax, out = random.Random(seed), Axis(False), []
        for i in range(int(secs * hz)):
            truth = signal(i / hz)
            out.append((i / hz, truth, orientation_step(ax, truth + r.gauss(0, noise), int(i * 1e9 / hz), beta=beta)))
        return out

    print("OrientationFilter: lag (deg) and rest residual (deg) for 0.05 deg of noise")
    for beta in (0.1, 0.25, 0.4):
        rest = [statistics.fmean([(o - 30) ** 2 for t, _, o in run(lambda t: 30.0, hz, 60, 0.05, beta=beta) if t > 10]) ** 0.5
                for hz in (16, 50)]
        lags = {v: [round(statistics.fmean([tr - o for t, tr, o in run(lambda t: v * t, hz, 12, 0.05, beta=beta) if t > 6]), 2)
                    for hz in (16, 50)] for v in (5, 20, 60)}
        print(f"  beta={beta}: rest(16Hz,50Hz)={[round(x, 4) for x in rest]} lag@5,20,60 deg/s (16Hz,50Hz)={lags}")


# --------------------------------------------------------------------------------------------------
# ScreenCornerSmoother
# --------------------------------------------------------------------------------------------------
class Corners:
    def __init__(self, minc=0.4, beta=0.1, dcut=1.5, floor=12.0, hold=0.15, maxc=60.0, jump=150.0, lag_speed=False):
        self.p = (minc, beta, dcut, floor, hold, maxc, jump)
        self.lag_speed = lag_speed  # True: the speed the first version used, the filter's lag over dt
        self.x = None

    def filter(self, pts, t):
        minc, beta, dcut, floor, hold, maxc, jump = self.p
        n = len(pts) * 2
        flat = [c for p in pts for c in p]
        if self.x is not None:
            dt = (t - self.t) / 1e9
            if dt <= 0:
                return self.out
        if self.x is None or dt > 0.5 or any(abs(flat[i] - self.x[i]) > jump for i in range(n)):
            self.x, self.prev, self.dx, self.t, self.out = flat[:], flat[:], [0.0] * n, t, pts
            return pts
        self.t = t
        da = alpha(dcut, dt)
        for i in range(n):
            reference = self.x[i] if self.lag_speed else self.prev[i]
            self.dx[i] += da * ((flat[i] - reference) / dt - self.dx[i])
        self.prev = flat[:]
        speed = sum(math.hypot(self.dx[2 * i], self.dx[2 * i + 1]) for i in range(len(pts))) / len(pts)
        a = alpha(min(maxc, minc + beta * max(0.0, speed - floor)), dt)
        for i in range(n):
            self.x[i] += a * (flat[i] - self.x[i])
        res = [(self.x[2 * i], self.x[2 * i + 1]) for i in range(len(pts))]
        if max(math.hypot(res[i][0] - self.out[i][0], res[i][1] - self.out[i][1]) for i in range(len(pts))) < hold:
            return self.out
        self.out = res
        return res


def quad(x, y):
    return [(x, y), (x + 300, y), (x + 300, y + 200), (x, y + 200)]


def corner_run(make, fps, secs, truth, noise, seed=1):
    r, f, out = random.Random(seed), make(), []
    for i in range(int(secs * fps)):
        t = i / fps
        tx, ty = truth(t)
        q = quad(tx, ty)
        raw = [(a + r.gauss(0, noise), b + r.gauss(0, noise)) for a, b in q]
        out.append((t, q, f.filter(raw, int(t * 1e9))))
    return out


def corner_metrics(make, fps):
    o = corner_run(make, fps, 30, lambda t: (100, 200), 1.0)
    rest = statistics.fmean([(c - tc) ** 2 for t, q, oo in o if t > 5 for (a, b), (ta, tb) in zip(oo, q)
                             for c, tc in ((a, ta), (b, tb))]) ** 0.5

    def pan(t):
        if t < 1:
            return 100, 200
        s = t - 1
        return 100 + (0.5 * 5000 * s * s if s < 0.2 else 100 + 1000 * (s - 0.2)), 200

    o = corner_run(make, fps, 4, pan, 0.3)
    onset = max(q[0][0] - oo[0][0] for t, q, oo in o if t > 1)
    steady = statistics.fmean([q[0][0] - oo[0][0] for t, q, oo in o if t > 3])
    o = corner_run(make, fps, 10, lambda t: (100 + 30 * t, 200), 1.0)
    creep = statistics.fmean([q[0][0] - oo[0][0] for t, q, oo in o if t > 3])
    return rest, onset, steady, creep


def corner_report():
    print("ScreenCornerSmoother: rest residual, onset lag, steady lag (1000 px/s), creep lag (30 px/s), px, 1 px noise")
    shipped = lambda: Corners()
    legacy = lambda: Corners(minc=1.2, beta=0.02, dcut=1.0, floor=0.0, hold=0.0)  # the test's legacy()
    original = lambda: Corners(minc=1.2, beta=0.02, dcut=1.0, floor=0.0, hold=0.0, lag_speed=True)  # as first shipped
    for name, make in (("original", original), ("legacy-like", legacy), ("shipped", shipped)):
        for fps in (15, 30, 60, 120):
            r = corner_metrics(make, fps)
            print(f"  {name:11s} {fps:3d} fps  rest={r[0]:.3f} onset={r[1]:5.1f} steady={r[2]:4.1f} creep={r[3]:.2f}")


# --------------------------------------------------------------------------------------------------
# TablePull
# --------------------------------------------------------------------------------------------------
REF = 300.0


def share(per_ref, dt):
    return 1 - (1 - per_ref) ** (dt / REF)


class Pull:
    def __init__(self):
        self.release()

    def release(self):
        self.target, self.engaged, self.last = None, False, 0

    def step(self, cur, fit, now, iou=0.95, since=10_000):
        if iou < 0.85 or since < 1500:
            self.release()
            return None
        gap = now - self.last
        if self.target is not None and self.last != 0 and gap <= 0:
            return None
        stale = self.target is None or self.last == 0 or gap > 2000
        if stale:
            self.engaged = False
        dt = REF if stale else gap
        self.last = now
        sm = fit if stale else self.target + (fit - self.target) * share(0.3, dt)
        self.target = sm
        d = sm - cur
        if not self.engaged:
            if abs(d) > 1.5:
                self.engaged = True
            else:
                return None
        elif abs(d) < 0.5:
            self.engaged = False
            return None
        return cur + d * min(0.5, share(0.15, dt))


def old_pull(cur, fit):
    err = fit - cur
    return cur + 0.15 * err if abs(err) >= 1.0 else cur


def pull_report():
    print("TablePull: 50 px away, 300 fits at 300 ms, 3 px fit noise, 300 seeds")
    for name in ("old", "new"):
        conv, wander, moves = [], [], []
        for seed in range(300):
            r, p, cur, hist, steps, settled = random.Random(seed), Pull(), 0.0, [], 0, None
            for k in range(300):
                fit = 50 + r.gauss(0, 3)
                n = old_pull(cur, fit) if name == "old" else p.step(cur, fit, 1000 + k * 300)
                if n is not None and n != cur:
                    steps += k >= 150
                    cur = n
                if settled is None and abs(cur - 50) < 2:
                    settled = k * 0.3
                if k >= 150:
                    hist.append(cur)
            conv.append(settled if settled is not None else 90)
            wander.append(statistics.pstdev(hist))
            moves.append(steps / 150)
        print(f"  {name}: settle {statistics.fmean(conv):.2f} s, wander {statistics.fmean(wander):.2f} px, "
              f"fraction of fits that step {statistics.fmean(moves):.2f}")


if __name__ == "__main__":
    orientation_report()
    print()
    corner_report()
    print()
    pull_report()
