package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.OperationType;
import com.nexis.websocket_service.payload.CodeOperation;

public class OTEngine {

    public static CodeOperation transform(CodeOperation incoming, CodeOperation historical) {
        if (historical.getOperationType() == OperationType.RETAIN) {
            return incoming;
        }

        if (historical.getOperationType() == OperationType.INSERT) {
            if (incoming.getOperationType() == OperationType.INSERT) {
                return insert_insert(incoming, historical);
            } else {
                return delete_insert(incoming, historical); // Incoming=DELETE, Hist=INSERT
            }
        } else {
            if (incoming.getOperationType() == OperationType.INSERT) {
                return insert_delete(incoming, historical); // Incoming=INSERT, Hist=DELETE
            } else {
                return delete_delete(incoming, historical);
            }
        }
    }

    private static CodeOperation insert_insert(CodeOperation inc, CodeOperation hist) {
        if (hist.getPosition() < inc.getPosition() ||
                (hist.getPosition().equals(inc.getPosition()) && inc.getUserId().compareTo(hist.getUserId()) > 0)) {
            // Historical text was inserted before us. Shift our insert to the right.
            inc.setPosition(inc.getPosition() + hist.getCode().length());
        }
        return inc;
    }

    private static CodeOperation insert_delete(CodeOperation inc, CodeOperation hist) {
        // Hist is DELETE. Inc is INSERT.
        if (hist.getPosition() <= inc.getPosition()) {
            if (inc.getPosition() < hist.getPosition() + hist.getLength()) {
                // We are trying to insert inside a region that was just deleted. Shift to the start of the deletion.
                inc.setPosition(hist.getPosition());
            } else {
                // Historical deletion happened entirely before us. Shift left by the deleted amount.
                inc.setPosition(inc.getPosition() - hist.getLength());
            }
        }
        return inc;
    }

    private static CodeOperation delete_insert(CodeOperation inc, CodeOperation hist) {
        // Hist is INSERT. Inc is DELETE.
        if (hist.getPosition() <= inc.getPosition()) {
            // Hist inserted text BEFORE our deletion. Shift our deletion right.
            inc.setPosition(inc.getPosition() + hist.getCode().length());
        } else if (hist.getPosition() < inc.getPosition() + inc.getLength()) {
            // Hist inserted text INSIDE the range we are trying to delete.
            // Expand our deletion length to swallow the newly inserted text too.
            inc.setLength(inc.getLength() + hist.getCode().length());
        }
        return inc;
    }

    private static CodeOperation delete_delete(CodeOperation inc, CodeOperation hist) {
        // Both are DELETE.
        if (hist.getPosition() <= inc.getPosition()) {
            if (inc.getPosition() < hist.getPosition() + hist.getLength()) {
                // Deletions overlap. Shrink our deletion length by the overlapping amount.
                int overlap = Math.min(inc.getPosition() + inc.getLength(), hist.getPosition() + hist.getLength()) - inc.getPosition();
                inc.setLength(inc.getLength() - overlap);
                inc.setPosition(hist.getPosition());
            } else {
                // Hist deletion happened entirely before us. Shift left.
                inc.setPosition(inc.getPosition() - hist.getLength());
            }
        } else if (hist.getPosition() < inc.getPosition() + inc.getLength()) {
            // Hist starts INSIDE our deletion range.
            int overlap = Math.min(inc.getPosition() + inc.getLength(), hist.getPosition() + hist.getLength()) - hist.getPosition();
            inc.setLength(inc.getLength() - overlap);
        }
        return inc;
    }
}