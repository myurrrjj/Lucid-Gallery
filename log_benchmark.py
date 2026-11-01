#!/usr/bin/env python3
import argparse
import glob
import json
import os
import subprocess
import sys
from datetime import datetime

import openpyxl
from openpyxl.styles import Font
from openpyxl.utils import column_index_from_string, get_column_letter

DEFAULT_EXCEL = "gallery_macrobenchmark_tracker.xlsx"
SHEET_NAME = "Benchmark Logs"

JSON_PATTERNS = [
    "benchmark/build/outputs/connected_android_test_additional_output/**/*benchmarkData.json",
    "benchmark/build/outputs/**/*benchmarkData.json",
    "**/*benchmarkData.json",
]

COLUMNS = [
    ("Run #", 7),
    ("Date", 12),
    ("Commit", 11),
    ("Note", 38),
    ("P50 (ms)", 10),
    ("P90 (ms)", 10),
    ("P95 (ms)", 10),
    ("P99 (ms)", 10),
    ("Overrun P95 (ms)", 16),
    ("Peak Heap (KB)", 15),
    ("Peak Heap (MB)", 15),
    ("Frames", 9),
    ("Peak RSS (KB)", 15),
    ("Iterations", 11),
    ("Peak GPU (KB)", 15),
    ("Peak GPU (MB)", 15),
    ("RSS File (KB)", 15),
    ("RSS File (MB)", 15),
]

LATENCY_COLUMNS = ["E", "F", "G", "H", "I"]
SUM_COLUMNS = ["E", "F", "G", "H", "I", "J", "K", "L", "M", "O", "P", "Q", "R"]

BEST_LABEL = "Best (min)"
AVG_LABEL = "Average"
LEGACY_SUMMARY_LABELS = {BEST_LABEL, AVG_LABEL, "Best / Avg", "Best/Avg"}


def get_git_commit():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except Exception:
        return "local-dev"


def find_latest_benchmark_json(explicit=None):
    if explicit:
        if not os.path.isfile(explicit):
            sys.exit(f"--json path does not exist: {explicit}")
        return explicit

    files = []
    for pattern in JSON_PATTERNS:
        files.extend(glob.glob(pattern, recursive=True))
    files = sorted(set(os.path.abspath(path) for path in files))
    if not files:
        sys.exit(
            "No *benchmarkData.json found under the current directory.\n"
            "Run the macrobenchmark first, or pass --json <path>.\n"
            "Hint: adb pull "
            "/sdcard/Android/media/com.example.jetpackcomposegalleryapp.benchmark/"
            "additional_test_output ./benchmark-results"
        )
    return max(files, key=os.path.getmtime)


def _block(bench, name):
    for section in ("sampledMetrics", "metrics"):
        block = (bench.get(section) or {}).get(name)
        if isinstance(block, dict):
            return block
    return {}


def _num(block, key, default=0.0):
    value = block.get(key)
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return default
    return float(value)


def parse_benchmarks(json_path):
    with open(json_path, "r") as handle:
        data = json.load(handle)

    context = data.get("context", {}) or {}
    build = context.get("build", {}) or {}
    device = build.get("model") or build.get("device") or "unknown device"

    rows = []
    for bench in data.get("benchmarks", []):
        frame = _block(bench, "frameDurationCpuMs")
        overrun = _block(bench, "frameOverrunMs")
        frames = _block(bench, "frameCount")

        heap_kb = _num(_block(bench, "memoryHeapSizeMaxKb"), "maximum")
        if heap_kb == 0.0:
            heap_kb = _num(_block(bench, "memoryRssAnonMaxKb"), "maximum")
        rss_kb = _num(_block(bench, "memoryRssAnonMaxKb"), "maximum")
        gpu_kb = _num(_block(bench, "memoryGpuMaxKb"), "maximum")
        rss_file_kb = _num(_block(bench, "memoryRssFileMaxKb"), "maximum")

        iterations = bench.get("repeatIterations")
        if iterations is None:
            iterations = len(frame.get("runs") or []) or None

        rows.append(
            {
                "name": bench.get("name") or bench.get("className") or "benchmark",
                "device": device,
                "p50": _num(frame, "P50"),
                "p90": _num(frame, "P90"),
                "p95": _num(frame, "P95"),
                "p99": _num(frame, "P99"),
                "overrun_p95": _num(overrun, "P95"),
                "heap_kb": int(round(heap_kb)),
                "frames": int(round(_num(frames, "median"))) or 0,
                "rss_kb": int(round(rss_kb)),
                "iterations": iterations or 0,
                "gpu_kb": int(round(gpu_kb)),
                "rss_file_kb": int(round(rss_file_kb)),
            }
        )

    if not rows:
        sys.exit(f"{json_path} contains no benchmark entries.")
    return rows


def _load_or_create_workbook(path):
    if os.path.isfile(path):
        workbook = openpyxl.load_workbook(path)
        if SHEET_NAME in workbook.sheetnames:
            sheet = workbook[SHEET_NAME]
        else:
            sheet = workbook.create_sheet(SHEET_NAME)
    else:
        workbook = openpyxl.Workbook()
        sheet = workbook.active
        sheet.title = SHEET_NAME
    _ensure_headers(sheet)
    return workbook, sheet


def _ensure_headers(sheet):
    headers = [header for header, _ in COLUMNS]
    current = [sheet.cell(row=1, column=i + 1).value for i in range(len(headers))]
    if current == headers:
        return
    for index, (header, width) in enumerate(COLUMNS, start=1):
        cell = sheet.cell(row=1, column=index, value=header)
        cell.font = Font(bold=True)
        sheet.column_dimensions[get_column_letter(index)].width = width
    sheet.freeze_panes = "A2"


def _drop_summary_rows(sheet):
    for row in range(sheet.max_row, 1, -1):
        value = sheet.cell(row=row, column=1).value
        if isinstance(value, str) and value.strip() in LEGACY_SUMMARY_LABELS:
            sheet.delete_rows(row)


def _data_row_numbers(sheet):
    return [
        row
        for row in range(2, sheet.max_row + 1)
        if isinstance(sheet.cell(row=row, column=1).value, int)
    ]


def _write_summary_rows(sheet):
    data_rows = _data_row_numbers(sheet)
    if not data_rows:
        return
    first, last = data_rows[0], data_rows[-1]
    for offset, (label, function) in enumerate(((BEST_LABEL, "MIN"), (AVG_LABEL, "AVERAGE"))):
        row = last + 1 + offset
        sheet.cell(row=row, column=1, value=label).font = Font(bold=True)
        for column in SUM_COLUMNS:
            formula = f"={function}({column}{first}:{column}{last})"
            cell = sheet.cell(row=row, column=column_index_from_string(column), value=formula)
            cell.font = Font(bold=True)
            if column in LATENCY_COLUMNS:
                cell.number_format = "0.00"


def append_runs(note, rows, excel_path):
    workbook, sheet = _load_or_create_workbook(excel_path)
    _drop_summary_rows(sheet)

    existing = _data_row_numbers(sheet)
    next_run_id = 1
    if existing:
        next_run_id = int(sheet.cell(row=existing[-1], column=1).value) + 1

    date_str = datetime.now().strftime("%Y-%m-%d")
    commit = get_git_commit()
    single = len(rows) == 1

    for row_data in rows:
        row = max(sheet.max_row + 1, 2)
        label = note if single else f"{note} [{row_data['name']}]"

        values = [
            next_run_id,
            date_str,
            commit,
            label,
            round(row_data["p50"], 2),
            round(row_data["p90"], 2),
            round(row_data["p95"], 2),
            round(row_data["p99"], 2),
            round(row_data["overrun_p95"], 2),
            row_data["heap_kb"],
            f"=ROUND(J{row}/1024,1)",
            row_data["frames"],
            row_data["rss_kb"],
            row_data["iterations"],
            row_data["gpu_kb"],
            f"=ROUND(O{row}/1024,1)",
            row_data["rss_file_kb"],
            f"=ROUND(Q{row}/1024,1)",
        ]
        for column_index, value in enumerate(values, start=1):
            sheet.cell(row=row, column=column_index, value=value)

        print(
            f"Logged Run #{next_run_id} ({commit}) on {row_data['device']}"
            f" -> P95 {row_data['p95']:.2f} ms | Overrun P95 {row_data['overrun_p95']:.2f} ms"
            f" | Heap {row_data['heap_kb'] / 1024:.1f} MB"
            f" | GPU {row_data['gpu_kb'] / 1024:.1f} MB"
        )
        next_run_id += 1

    _write_summary_rows(sheet)
    workbook.save(excel_path)
    print(f"Saved -> {os.path.abspath(excel_path)}")


def main():
    parser = argparse.ArgumentParser(description="Log a macrobenchmark run into Excel.")
    parser.add_argument("note", nargs="?", default="Combined Scroll + Pinch Run")
    parser.add_argument("--json", dest="json_path", default=None)
    parser.add_argument("--excel", dest="excel_path", default=DEFAULT_EXCEL)
    args = parser.parse_args()

    json_path = find_latest_benchmark_json(args.json_path)
    print(f"Reading {json_path}")
    rows = parse_benchmarks(json_path)
    append_runs(args.note, rows, args.excel_path)


if __name__ == "__main__":
    main()
