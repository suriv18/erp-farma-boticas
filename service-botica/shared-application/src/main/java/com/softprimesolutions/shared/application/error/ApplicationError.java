package com.softprimesolutions.shared.application.error;

import java.util.Map;

/** Contrato estable de error entre Application y los adaptadores de entrada. */
public interface ApplicationError {

    String code();

    String message();

    ErrorCategory category();

    Map<String, Object> metadata();
}
