r"""
 By Dylon Edwards

 * Copyright (C) 2023 GSI Technology, Inc. All rights reserved.
 *
 * This software source code is the sole property of GSI Technology, Inc.
 * and is proprietary and confidential.
"""

import logging
from io import StringIO
from pathlib import Path
from queue import Queue
from signal import SIGTERM, pthread_kill
from threading import Condition, RLock, Thread
from typing import Any, Optional, Sequence, Tuple, Union

import numpy as np

from pygments import highlight
from pygments.lexers import PythonLexer
from pygments.formatters import HtmlFormatter

from flask import Flask, request, send_from_directory
from flask_socketio import SocketIO, disconnect, emit

from transit.writer import Writer as TransitWriter

from belex.bleir.interpreters import BLEIRInterpreter
from belex.bleir.types import (CallerMetadata, Fragment, FragmentCallerCall,
                               FragmentMetadata, StatementMetadata)
from belex.bleir.walkables import is_bleir
from belex.diri.half_bank import DIRI
from belex.common.rsp_fifo import ApucRspFifo
from belex.common.rsp_fifo import RspFifoMsg
from belex.common.seu_layer import SEULayer
from belex.common.types import Integer

LOGGER = logging.getLogger()

root_dir = Path(__file__).parent / "../../.."
static_folder = root_dir / "resources" / "public"

app = Flask(__name__,
            static_url_path="",
            static_folder=str(static_folder))
socketio = SocketIO(app)

events = Queue(128)
event_lock = RLock()
event_condition = Condition(lock=event_lock)

alive = True

running = False
running_lock = RLock()
running_condition = Condition(lock=running_lock)

server_proc: Optional[Thread] = None

script_spec: '_frozen_importlib.ModuleSpec' = None
script_module: 'module' = None


def emit_event(event: Sequence[Any]) -> None:
    with event_lock:
        while events.full():
            event_condition.wait()
        events.put(event)
        event_condition.notify_all()


seu_layer = SEULayer.push_context()
apuc_rsp_fifo = ApucRspFifo.push_context()
diri = DIRI.push_context(apuc_rsp_fifo=apuc_rsp_fifo)
interpreter = BLEIRInterpreter.push_context(diri=diri)

seu_layer.subscribe(emit_event)
apuc_rsp_fifo.subscribe(emit_event)
diri.subscribe(emit_event)
interpreter.subscribe(emit_event)


def await_event() -> Optional[Sequence[Any]]:
    while alive and events.empty():
        with event_lock:
            if alive and events.empty():
                event_condition.wait()
    if alive and not events.empty():
        with event_lock:
            if alive and not events.empty():
                return events.get_nowait()


@app.route("/")
def render_app():
    return send_from_directory(static_folder, "index.html")


@socketio.on("connect")
def handle_connect(auth) -> None:
    LOGGER.debug("Connecting to client_id: %s", request.sid)


@socketio.on("disconnect")
def handle_disconnect() -> None:
    LOGGER.debug("Disconnecting from client_id: %s", request.sid)
    stop()


def start_app() -> None:
    global running

    while alive:
        while running:
            with running_lock:
                if running:
                    running_condition.wait()

        running = True

        with event_lock:
            if not alive:
                break

        LOGGER.debug("Starting application.")
        emit_event(["app::start"])
        script_spec.loader.exec_module(script_module)
        emit_event(["app::stop"])


@socketio.on("restart")
def handle_restart() -> None:
    LOGGER.debug("Stopping application.")

    while not events.empty():
        with event_lock():
            if not events.empty():
                events.get_nowait()
                events.task_done()

    with running_lock:
        running = False
        running_condition.notify_all()


def jsonify_bool_list(xss: Sequence[Union[Sequence[bool], bool]]) \
        -> Sequence[Union[Sequence[bool], bool]]:
    if xss.ndim == 1:
        xs = xss
        return [bool(x) for x in xs]
    else:
        return [jsonify_bool_list(xs) for xs in xss]


def jsonify_int_list(xss: Sequence[Union[Sequence[Integer], Integer]]) \
        -> Sequence[Union[Sequence[int], int]]:
    if np.ndim(xss) == 1:
        xs = xss
        return [int(x) for x in xs]
    else:
        return[jsonify_list(xs) for xs in xss]


def jsonify_bleir(bleir: Any) -> Tuple[Optional[str], Optional[int]]:
    if isinstance(bleir, FragmentCallerCall):
        file_path_key = CallerMetadata.FILE_PATH
        line_number_key = CallerMetadata.LINE_NUMBER
    elif isinstance(bleir, Fragment):
        file_path_key = FragmentMetadata.FILE_PATH
        line_number_key = FragmentMetadata.LINE_NUMBER
    else:
        file_path_key = StatementMetadata.FILE_PATH
        line_number_key = StatementMetadata.LINE_NUMBER
    file_path = bleir.get_metadata(file_path_key, default_value=None)
    line_number = bleir.get_metadata(line_number_key, default_value=-1)
    return [file_path, line_number]


def jsonify_event(event: Sequence[Any]) -> Sequence[Any]:
    json = []
    for component in event:
        if is_bleir(component):
            json.append(jsonify_bleir(component))
        elif isinstance(component, np.ndarray):
            json.append(jsonify_bool_list(component))
        elif isinstance(component, list):
            json.append(jsonify_int_list(component))
        elif isinstance(component, RspFifoMsg):
            json.append([int(component.rsp32k),
                         jsonify_int_list(component.rsp2k)])
        else:
            json.append(component)
    return json


def emit_json(event_nym: str, event_json: Any) -> None:
    io = StringIO()
    writer = TransitWriter(io, "json")
    writer.write(event_json)
    data = io.getvalue()
    emit(event_nym, data, broadcast=True)


def emit_app_event(event_json: Sequence[Any]) -> None:
    emit_json("app_event", event_json)


in_multi_statement = False
in_statement = False


@socketio.on("await_app_event")
def handle_await_app_event() -> None:
    global in_multi_statement
    global in_statement

    batch = []
    done = False

    while alive and not done:
        event = await_event()

        if event is None and alive:
            continue

        if not alive:
            # FIXME: Uncomment this when we can join on the events queue
            # while not events.empty():
            #     with event_lock:
            #         if not events.empty():
            #             events.get_nowait()
            #             events.task_done()
            disconnect()
            break

        if event[0] == "fragment::enter":
            emit_app_event(jsonify_event(event))
            done = True
        elif event[0] == "fragment::exit":
            continue
        elif event[0] == "multi_statement::enter":
            in_multi_statement = True
            emit_app_event(jsonify_event(event))
            done = True
        elif event[0] == "multi_statement::exit":
            in_multi_statement = False
            emit_app_event(("diri::batch", batch))
            batch.clear()
            done = True
        elif event[0] == "statement::enter":
            in_statement = True
            if not in_multi_statement:
                emit_app_event(jsonify_event(event))
                done = True
        elif event[0] == "statement::exit":
            in_statement = False
            if not in_multi_statement:
                emit_app_event(("diri::batch", batch))
                batch.clear()
                done = True
        elif (event[0].startswith("seu::")
              or event[0] in ["stdout", "stderr"]):
            emit_app_event(jsonify_event(event))
            done = True
        elif not (in_multi_statement or in_statement):
            emit_app_event(jsonify_event(event))
            done = True
        else:
            batch.append(jsonify_event(event))

        events.task_done()
        with event_lock:
            event_condition.notify_all()


@socketio.on("load_file")
def handle_load_file(file_path: str) -> None:
    with open(file_path) as fh:
        file_text = fh.read()
    lexer = PythonLexer()
    formatter = HtmlFormatter(linenos="inline",
                              lineanchors="line",
                              style="monokai")
    file_html = highlight(file_text, lexer, formatter)
    emit_json("file_load", [file_path, file_html])


@socketio.on_error_default
def handle_error(error):
    LOGGER.debug("An error occurred: %s", error)
    stop()


def start(host: str = "0.0.0.0",
          port: int = 9803,
          debug: bool = True,
          use_reloader: bool = False) -> None:
    global server_proc
    if server_proc is None or True:
        LOGGER.debug("Initializing debug server.")
        server_proc = Thread(target=socketio.run,
                             args=[app],
                             kwargs={"host": host,
                                     "port": port,
                                     "debug": debug,
                                     "use_reloader": use_reloader})
        server_proc.start()


def stop(*args, **kwargs) -> None:
    global server_proc
    if server_proc is not None:
        LOGGER.debug("Terminating debug server.")

        with event_lock:
            alive = False
            event_condition.notify_all()

        with running_lock:
            running_condition.notify_all()

        pthread_kill(server_proc.ident, SIGTERM)
        server_proc.join()
        server_proc = None

# FIXME: Why doesn't events.join() work as expected when I call
# events.task_done() each time I process an event?

# def join() -> None:
#     while not events.empty():
#         with event_lock:
#             if not events.empty():
#                 event_condition.wait()
#     stop()

# join = events.join
