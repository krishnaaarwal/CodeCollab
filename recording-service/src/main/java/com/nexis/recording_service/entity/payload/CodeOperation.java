package com.nexis.recording_service.entity.payload;

import com.nexis.recording_service.config.type.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CodeOperation {
    private Integer version;
    private UUID userId;
    private OperationType operationType;
    private Integer position;          // Kis position par insert karna ha
    private String code;               // Insert karne ka content
    private Integer length;           // How many characters to DELETE or RETAIN
}


/*
Cursors vs. Operational Transform (The 1D vs 2D Problem)

You are correct about the cursor: a frontend code editor (like Monaco or CodeMirror) treats the document as a 2D grid.
Line 10, Character 15. That is why your CursorPayload is perfectly designed.

However, reusing CursorPayload for your CodeOperation position is a trap.
Operational Transform algorithms (like ot.js) almost never process documents as a 2D grid.
They flatten the entire document into a single, massive 1D string.

    Frontend Cursor: Line 2, Character 5.

    OT Algorithm: Character index 145 (counting every character and newline \n from the very beginning of the document).
*/


/*
In Operational Transform, efficiency is everything. When you are blasting millions of operations through your WebSocket architecture, you want the payload to be as small as physically possible.

Think about the three operations: INSERT, DELETE, and RETAIN.

    INSERT: Needs the actual characters being typed. If you type "hello", the payload includes text: "hello". The length is obvious (5).

    DELETE: If a user highlights 5,000 lines of code and hits backspace, you do not want to send 150,000 characters of deleted text over your WebSockets. That would choke your network. Instead, you send operationType: DELETE, absoluteIndex: 100, and length: 15000. The server knows exactly what to chop out without needing the string itself.

    RETAIN: This tells the cursor to skip forward without changing anything. If you move your cursor 20 spaces to the right, you send operationType: RETAIN, length: 20.

The length field makes DELETE and RETAIN operations incredibly lightweight.
 */