package com.balugaq.buildingstaff.compat;

/** One synchronous custom placement, with payment reserved before provider callbacks. */
final class PlacementTransaction {
    enum Result { NO_PAYMENT, PLACED, ROLLED_BACK, RECOVERY_REQUIRED }
    interface Operations {
        boolean reserve();
        boolean place();
        boolean rollback();
        void refund();
        void report(Throwable failure);
    }
    private PlacementTransaction() {}
    static Result execute(Operations operations) {
        try {
            if (!operations.reserve()) return Result.NO_PAYMENT;
        } catch (RuntimeException | LinkageError failure) {
            report(operations, failure);
            // A nonstandard inventory setter may have partially applied its write.
            // Never guess whether a refund would duplicate the held item.
            return Result.RECOVERY_REQUIRED;
        }
        try {
            if (operations.place()) return Result.PLACED;
        } catch (RuntimeException | LinkageError failure) {
            report(operations, failure);
        }
        try {
            if (!operations.rollback()) return Result.RECOVERY_REQUIRED;
        } catch (RuntimeException | LinkageError failure) {
            report(operations, failure);
            return Result.RECOVERY_REQUIRED;
        }
        try {
            operations.refund();
            return Result.ROLLED_BACK;
        } catch (RuntimeException | LinkageError failure) {
            // Refunds must never be retried blindly after an exceptional setter.
            report(operations, failure);
            return Result.RECOVERY_REQUIRED;
        }
    }
    private static void report(Operations operations, Throwable failure) {
        try { operations.report(failure); }
        catch (RuntimeException | LinkageError ignored) { /* Logging cannot change payment/rollback ordering. */ }
    }
}
