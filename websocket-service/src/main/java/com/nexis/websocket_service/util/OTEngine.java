package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.OperationType;
import com.nexis.websocket_service.payload.CodeOperation;

public class OTEngine {

    public static CodeOperation transform(CodeOperation incoming, CodeOperation historical) {
        if (historical.getOperationType() == OperationType.RETAIN) {
            return incoming;
        }

        if (historical.getOperationType().equals(OperationType.INSERT)) {
            // RULE 1: historical = INSERT, incoming = INSERT
            if (incoming.getOperationType().equals(OperationType.INSERT)) {
                return insert_insert(incoming, historical);
            }
            // RULE 2: historical = INSERT, incoming = DELETE
            else {
                return insert_delete(incoming, historical);
            }
        } else {
            // RULE 3: historical = DELETE, incoming = INSERT
            if (incoming.getOperationType().equals(OperationType.INSERT)) {
                return delete_insert(incoming, historical);
            }
            // RULE 4: historical = DELETE, incoming = DELETE
            else {
                return delete_delete(incoming, historical);
            }
        }
    }

    private static CodeOperation delete_delete(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() <= incoming.getPosition()) {
            int histEnd = historical.getPosition() + historical.getLength();
            int incEnd = incoming.getPosition() + incoming.getLength();

            if (histEnd >= incEnd) {
                incoming.setOperationType(OperationType.RETAIN);
                incoming.setLength(0);
            } else {
                if (incoming.getPosition() < histEnd) {
                    int overlap = histEnd - incoming.getPosition();
                    incoming.setLength(incoming.getLength() - overlap);
                }
                int newPosition = Math.max(historical.getPosition(), incoming.getPosition() - historical.getLength());
                incoming.setPosition(newPosition);
            }
        }
        return incoming;
    }

    // FIXED: historical = INSERT, incoming = DELETE
    private static CodeOperation insert_delete(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() <= incoming.getPosition()) {
            // An insertion happened before our deletion. Shift the deletion RIGHT.
            int shiftAmount = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + shiftAmount);
        }
        return incoming;
    }

    // FIXED: historical = DELETE, incoming = INSERT
    private static CodeOperation delete_insert(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() <= incoming.getPosition()) {
            // A deletion happened before our insertion. Shift the insertion LEFT.
            if (incoming.getPosition() < historical.getPosition() + historical.getLength()) {
                // Insertion falls inside the deleted chunk. Clamp it to the start of the deletion.
                incoming.setPosition(historical.getPosition());
            } else {
                // Deletion was entirely before our insertion.
                incoming.setPosition(incoming.getPosition() - historical.getLength());
            }
        }
        return incoming;
    }

    private static CodeOperation insert_insert(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() < incoming.getPosition()) {
            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + codeLength);

        } else if (historical.getPosition() == incoming.getPosition()) {
            // TIE-BREAKER: Lexicographical UUID comparison to guarantee identical state
            int userComparison = incoming.getUserId().toString().compareTo(historical.getUserId().toString());
            if (userComparison > 0) {
                int codeLength = historical.getCode().length();
                incoming.setPosition(incoming.getPosition() + codeLength);
            }
        }
        return incoming;
    }
}