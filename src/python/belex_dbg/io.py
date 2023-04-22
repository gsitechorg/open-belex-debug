r"""
 By Dylon Edwards

 * Copyright (C) 2023 GSI Technology, Inc. All rights reserved.
 *
 * This software source code is the sole property of GSI Technology, Inc.
 * and is proprietary and confidential.
"""

from typing import Any, Callable, Sequence
from io import IOBase


EmitEventFn = Callable[[Any], None]

class EventIO:
    stream_nym: str
    emit_event: EmitEventFn
    ostream: IOBase
    buffer: Sequence[str]

    def __init__(self: "EventIO",
                 stream_nym: str,
                 emit_event: EmitEventFn,
                 ostream: IOBase) -> None:
        self.stream_nym = stream_nym
        self.emit_event = emit_event
        self.ostream = ostream
        self.buffer = []

    def write(self: "EventIO", line: str) -> None:
        print(line, file=self.ostream, end="")
        self.buffer.append(line)
        if "\n" in line:
            line = "".join(self.buffer)
            self.buffer.clear()
            self.emit_event([self.stream_nym, line])

    def writelines(self: "EventIO", lines: Sequence[str]) -> None:
        for line in lines:
            self.write(line)


class StdoutEventIO(EventIO):

    def __init__(self: "StdoutEventIO", emit_event: EmitEventFn, ostream: IOBase) -> None:
        super().__init__("stdout", emit_event, ostream)


class StderrEventIO(EventIO):

    def __init__(self: "StderrEventIO", emit_event: EmitEventFn, ostream: IOBase) -> None:
        super().__init__("stderr", emit_event, ostream)
