r"""
 By Dylon Edwards

 * Copyright (C) 2023 GSI Technology, Inc. All rights reserved.
 *
 * This software source code is the sole property of GSI Technology, Inc.
 * and is proprietary and confidential.
"""

import logging
import logging.handlers
import sys
from importlib.util import spec_from_file_location, module_from_spec
from pathlib import Path
from signal import signal, SIGINT
from typing import Optional, Sequence

import click

from belex.utils.log_utils import LogLevel, init_logger
from belex.utils.script_utils import (collect_config, collect_log_level,
                                      optimizations_by_level)

import belex_dbg.server as debug_server
from belex_dbg.io import StderrEventIO, StdoutEventIO

SCRIPT_NAME = "belex-dbg"

LOGGER = logging.getLogger()

SCRIPT_ARGS: Optional[Sequence[str]] = None


@click.command()
@click.option("-p", "--port", "port",
              help="Specifies the port to launch the debug server",
              type=int, default=9803)
@click.option("--host-name", "host_name", default="0.0.0.0",
              help="Host name of the debug server (default: localhost)")
@click.option("--config", "config",
              help="Specifies path to the BELEX config YAML file.",
              callback=collect_config,
              required=False)
@click.option("--log-level", "log_level",
              help="Specifies the verbosity of output from the compiler.",
              type=click.Choice(LogLevel.names()),
              default=LogLevel.DEFAULT.name,
              callback=collect_log_level,
              required=False)
@click.option("--debug/--release", "debug", default=False,
              help="Starts the server in debug mode")
@click.option("--capture-output/--no-capture-output", "capture_output",
              default=True,
              help=("Capture stdout/stderr (default: True; disabling this "
                    "feature is intended for development only)."))
@click.argument("belex_script")
def belex_dbg(**kwargs) -> None:
    """Launches a debug session for a Belex application."""

    log_level = kwargs["log_level"]
    init_logger(LOGGER, SCRIPT_NAME,
                log_level=log_level,
                log_to_console=False)

    belex_script = Path(kwargs["belex_script"])
    if not belex_script.exists():
        raise ValueError(f"belex_script does not exist: {belex_script}")
    if not belex_script.is_file():
        raise ValueError(f"belex_script is not a regular file: {belex_script}")

    # Only enable debug transformations
    optimizations = optimizations_by_level(0)

    host_name = kwargs["host_name"]
    port = kwargs["port"]
    debug = kwargs["debug"]

    sys.path.append(str(belex_script.parent.absolute()))
    sys.argv = [kwargs["belex_script"]]
    if SCRIPT_ARGS is not None:
        sys.argv += SCRIPT_ARGS

    signal(SIGINT, debug_server.stop)
    debug_server.start(host_name=host_name,
                       port=port,
                       debug=debug,
                       use_reloader=False)

    debug_server.script_spec = \
        spec_from_file_location("belex_script", kwargs["belex_script"])
    debug_server.script_module = module_from_spec(debug_server.script_spec)

    if kwargs["capture_output"]:
        sys.stdout = StdoutEventIO(debug_server.emit_event, sys.stdout)
        sys.stderr = StderrEventIO(debug_server.emit_event, sys.stderr)

    debug_server.start_app()
    # debug_server.join()


def main() -> None:
    global SCRIPT_ARGS
    try:
        # Separate the debug args from the script args
        if "--" in sys.argv:
            pivot = sys.argv.index("--")
            SCRIPT_ARGS = sys.argv[pivot + 1:]
            sys.argv = sys.argv[:pivot]
        belex_dbg()
    except Exception:
        LOGGER.exception("An unexpected exception has occurred.")
        sys.exit(1)


if __name__ == "__main__":
    main()
