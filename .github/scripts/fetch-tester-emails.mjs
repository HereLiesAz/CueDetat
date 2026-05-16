#!/usr/bin/env node
//
// Scrapes the tester Google Group's member list and emits SHA-256 hex hashes
// of every member email, one per line. Designed to be invoked by a Gradle task
// so it runs as part of `:app:assemblePlayRelease`.
//
// Reads from env:
//   GG_LINK    — full URL to the group's members page
//                (e.g. https://groups.google.com/g/<group>/members)
//   GG_SESSION — JSON dump of a Playwright `storageState` object
//                (cookies + localStorage) from a logged-in account that can
//                view the member list
//
// On any failure (missing env, scrape error, no emails extracted) it exits 0
// and prints nothing — the gradle task treats an empty output as "no tester
// licenses for this build" rather than failing the build.
//
// Caveats: this is automated access to Google Groups, which is brittle and
// against Google's TOS. Account flagging, cookie expiry, and DOM changes are
// known failure modes. Treat as best-effort.

import { chromium } from "playwright";
import crypto from "node:crypto";

const ggLink = process.env.GG_LINK ?? "";
const ggSession = process.env.GG_SESSION ?? "";

if (!ggLink || !ggSession) {
  console.error("[fetch-tester-emails] GG_LINK or GG_SESSION not set; emitting empty list.");
  process.exit(0);
}

let storageState;
try {
  storageState = JSON.parse(ggSession);
} catch (e) {
  console.error("[fetch-tester-emails] GG_SESSION is not valid JSON; emitting empty list.", e.message);
  process.exit(0);
}

const EMAIL_RE = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g;

function sha256Hex(input) {
  return crypto.createHash("sha256").update(input).digest("hex");
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ storageState });
  const page = await context.newPage();

  try {
    await page.goto(ggLink, { waitUntil: "networkidle", timeout: 60_000 });
    // The members panel renders client-side; wait for at least one row.
    await page.waitForSelector('[role="row"], [data-email], [data-member-email]', { timeout: 30_000 }).catch(() => {});

    // Try several extraction strategies; the first one that finds anything wins.
    const emails = await page.evaluate(() => {
      const set = new Set();
      const collect = (s) => { if (s) set.add(s); };

      document.querySelectorAll("[data-email]").forEach((el) =>
        collect(el.getAttribute("data-email"))
      );
      document.querySelectorAll("[data-member-email]").forEach((el) =>
        collect(el.getAttribute("data-member-email"))
      );
      document.querySelectorAll('a[href^="mailto:"]').forEach((el) =>
        collect(decodeURIComponent(el.getAttribute("href").replace(/^mailto:/, "")))
      );
      // Last-resort regex sweep over visible text.
      const text = document.body ? document.body.innerText : "";
      const m = text.match(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g);
      if (m) m.forEach((s) => collect(s));
      return Array.from(set);
    });

    const hashes = new Set();
    for (const raw of emails) {
      const normalized = String(raw).trim().toLowerCase();
      if (EMAIL_RE.test(normalized)) {
        EMAIL_RE.lastIndex = 0;
        hashes.add(sha256Hex(normalized));
      }
    }

    if (hashes.size === 0) {
      console.error("[fetch-tester-emails] No emails extracted from the page.");
    } else {
      console.error(`[fetch-tester-emails] Extracted ${hashes.size} tester emails.`);
    }
    // stdout is the consumed channel — one hash per line for easy grep / read.
    for (const h of hashes) {
      process.stdout.write(h + "\n");
    }
  } catch (e) {
    console.error("[fetch-tester-emails] scrape failed:", e.message);
  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error("[fetch-tester-emails] unexpected:", e);
  process.exit(0); // never break the gradle build
});
