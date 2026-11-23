package com.dafit.infrastructure.adapter.out.persistence;

/** Puerto de salida: chequeo de conectividad a la base de datos. */
public interface DatabaseConnectivityCheck {
    void verify();
}