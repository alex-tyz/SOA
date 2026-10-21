package ru.itmo.soa.starship;

final class ServiceFailure extends RuntimeException {
    private final int status;

    ServiceFailure(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() {
        return status;
    }
}
