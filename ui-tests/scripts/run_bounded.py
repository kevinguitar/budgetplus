"""Run a device/setup command with a portable deadline (no coreutils required)."""

import argparse
import subprocess
import sys

from run_maestro import duration, terminate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("timeout", type=duration)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if not args.command:
        parser.error("a command is required")
    try:
        process = subprocess.Popen(args.command, start_new_session=True)
    except OSError as error:
        print(error, file=sys.stderr)
        return 1
    try:
        result = process.wait(timeout=args.timeout)
    except subprocess.TimeoutExpired:
        terminate(process)
        print(f"Command exceeded {args.timeout:g}s: {args.command[0]}", file=sys.stderr)
        return 124
    except KeyboardInterrupt:
        terminate(process)
        return 130
    return result if 0 <= result <= 255 else 1


if __name__ == "__main__":
    sys.exit(main())
