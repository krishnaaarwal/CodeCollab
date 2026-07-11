package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.OperationType;
import com.nexis.websocket_service.payload.CodeOperation;

public class OTEngine {

    public static CodeOperation transform(CodeOperation incoming, CodeOperation historical) {
        if (historical.getOperationType() == OperationType.RETAIN) {
            return incoming;
        }

        if (historical.getOperationType().equals(OperationType.INSERT)) {
            //RULE 1: INSERT VS INSERT
            if (incoming.getOperationType().equals(OperationType.INSERT)) {
                return insert_insert(incoming, historical);
            }//RULE 2: INSERT VS DELETE
            else {
                return insert_delete(incoming, historical);
            }
        } else {//RULE 3: DELETE VS INSERT
            if (incoming.getOperationType().equals(OperationType.INSERT)) {
                return delete_insert(incoming, historical);
            }//RULE 4: DELETE VS DELETE
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
                // The historical deletion completely swallowed the incoming deletion.
                incoming.setOperationType(OperationType.RETAIN);
                incoming.setLength(0);
            } else {
                // CLAUDE'S FIX: Shrink the incoming length by the overlap amount
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

    private static CodeOperation delete_insert(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() < incoming.getPosition()) {
            int newPosition = Math.max(historical.getPosition(), incoming.getPosition() - historical.getLength());
            incoming.setPosition(newPosition);

                        /*
            Logic:  Agar historical highlight index 2 to 8 and press backspace and income insert at 5
            Then historic.getpostion(2) < incoming.getPostion(5)
            calculates incoming.setPosition(5 - 6) = -1    -> IndexOutOfBoundException

             */
        }
        return incoming;
    }

    private static CodeOperation insert_delete(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() <= incoming.getPosition()) {
            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + codeLength);
        }
        return incoming;
    }

    private static CodeOperation insert_insert(CodeOperation incoming, CodeOperation historical) {
        if (historical.getPosition() < incoming.getPosition()) {
            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + codeLength);

        } else if (historical.getPosition() == incoming.getPosition()) {
            // TIE-BREAKER FIX: Lexicographical UUID comparison to guarantee identical state
            int userComparison = incoming.getUserId().toString().compareTo(historical.getUserId().toString());

            if (userComparison > 0) {
                int codeLength = historical.getCode().length();
                incoming.setPosition(incoming.getPosition() + codeLength);
            }
        }
        return incoming;
    }
}
