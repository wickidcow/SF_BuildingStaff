package com.balugaq.buildingstaff.compat;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static com.balugaq.buildingstaff.compat.PlacementTransaction.Result.*;

/** Production ordering with explicit callback doubles; native provider tests are separate. */
class PlacementTransactionTest {
    static final class Ops implements PlacementTransaction.Operations {
        boolean paid=true, placed=true, rolledBack=true;
        int reserveCalls, placeCalls, rollbackCalls, refundCalls;
        Throwable reserveFailure, placeFailure, rollbackFailure, refundFailure, reportFailure;
        final List<String> trace=new ArrayList<>();
        final List<Throwable> failures=new ArrayList<>();
        private void fail(Throwable value) {
            if(value instanceof RuntimeException r) throw r;
            if(value instanceof Error e) throw e;
        }
        @Override public boolean reserve(){ reserveCalls++; trace.add("reserve"); fail(reserveFailure); return paid; }
        @Override public boolean place(){ placeCalls++; trace.add("place"); fail(placeFailure); return placed; }
        @Override public boolean rollback(){ rollbackCalls++; trace.add("rollback"); fail(rollbackFailure); return rolledBack; }
        @Override public void refund(){ refundCalls++; trace.add("refund"); fail(refundFailure); }
        @Override public void report(Throwable value){ failures.add(value); fail(reportFailure); }
    }
    @Test void successfulPlacementChargesOnceBeforeAnyCallback(){
        Ops o=new Ops(); assertEquals(PLACED,PlacementTransaction.execute(o));
        assertEquals(List.of("reserve","place"),o.trace);
    }
    @Test void insufficientPaymentCannotCallProvider(){
        Ops o=new Ops();o.paid=false;assertEquals(NO_PAYMENT,PlacementTransaction.execute(o));
        assertEquals(List.of("reserve"),o.trace);
    }
    @Test void cancelledPlacementRequiresRollbackBeforeRefund(){
        Ops o=new Ops();o.placed=false;assertEquals(ROLLED_BACK,PlacementTransaction.execute(o));
        assertEquals(List.of("reserve","place","rollback","refund"),o.trace);
    }
    @Test void vetoedRollbackNeverRefundsPaidItem(){
        Ops o=new Ops();o.placed=false;o.rolledBack=false;
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));
        assertEquals(List.of("reserve","place","rollback"),o.trace);
    }
    @Test void failedReservationNeverGuessesRefund(){
        Ops o=new Ops();o.reserveFailure=new IllegalStateException("partial write");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(List.of("reserve"),o.trace);
        assertSame(o.reserveFailure,o.failures.getFirst());
    }
    @Test void brokenReservationAbiNeverReachesWorld(){
        Ops o=new Ops();o.reserveFailure=new NoSuchMethodError("inventory ABI");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(0,o.placeCalls);
    }
    @Test void providerExceptionStillAttemptsVerifiedRollback(){
        Ops o=new Ops();o.placeFailure=new IllegalStateException("provider failed");
        assertEquals(ROLLED_BACK,PlacementTransaction.execute(o));
        assertEquals(List.of("reserve","place","rollback","refund"),o.trace);assertSame(o.placeFailure,o.failures.getFirst());
    }
    @Test void providerLinkageErrorStillRequiresRollback(){
        Ops o=new Ops();o.placeFailure=new IncompatibleClassChangeError("provider ABI");
        assertEquals(ROLLED_BACK,PlacementTransaction.execute(o));assertEquals(1,o.refundCalls);
    }
    @Test void providerExceptionPlusRollbackVetoRetainsPayment(){
        Ops o=new Ops();o.placeFailure=new IllegalArgumentException("failed");o.rolledBack=false;
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(0,o.refundCalls);
    }
    @Test void rollbackExceptionCannotBeMistakenForSuccess(){
        Ops o=new Ops();o.placed=false;o.rollbackFailure=new IllegalStateException("rollback");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(0,o.refundCalls);
    }
    @Test void rollbackLinkageErrorCannotBeMistakenForSuccess(){
        Ops o=new Ops();o.placed=false;o.rollbackFailure=new NoClassDefFoundError("provider");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(0,o.refundCalls);
    }
    @Test void exceptionalRefundIsNeverRetried(){
        Ops o=new Ops();o.placed=false;o.refundFailure=new IllegalStateException("partial refund");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(1,o.refundCalls);assertEquals(1,o.rollbackCalls);
    }
    @Test void refundLinkageErrorIsNotRetried(){
        Ops o=new Ops();o.placed=false;o.refundFailure=new NoSuchMethodError("refund ABI");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));assertEquals(1,o.refundCalls);
    }
    @Test void brokenLoggerCannotPreventProviderRollback(){
        Ops o=new Ops();o.placeFailure=new IllegalStateException("failure");o.reportFailure=new IllegalStateException("logger");
        assertEquals(ROLLED_BACK,PlacementTransaction.execute(o));assertEquals(1,o.refundCalls);
    }
    @Test void brokenLoggerAbiCannotPreventProviderRollback(){
        Ops o=new Ops();o.placeFailure=new IllegalStateException("failure");o.reportFailure=new NoSuchMethodError("logger ABI");
        assertEquals(ROLLED_BACK,PlacementTransaction.execute(o));assertEquals(1,o.refundCalls);
    }
    @Test void successfulPlacementDoesNotProbeRollback(){
        Ops o=new Ops();o.rollbackFailure=new AssertionError("must not run");
        assertEquals(PLACED,PlacementTransaction.execute(o));assertEquals(0,o.rollbackCalls);
    }
    @Test void successfulPlacementDoesNotTouchRefund(){
        Ops o=new Ops();o.refundFailure=new AssertionError("must not run");
        assertEquals(PLACED,PlacementTransaction.execute(o));assertEquals(0,o.refundCalls);
    }
    @Test void noPaymentDoesNotProbeDestructiveRecovery(){
        Ops o=new Ops();o.paid=false;o.rollbackFailure=new AssertionError("must not run");o.refundFailure=new AssertionError("must not run");
        assertEquals(NO_PAYMENT,PlacementTransaction.execute(o));
    }
    @Test void eachOperationRunsAtMostOnceEvenWithTwoFailures(){
        Ops o=new Ops();o.placeFailure=new IllegalStateException("place");o.rollbackFailure=new IllegalStateException("rollback");
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(o));
        assertEquals(1,o.reserveCalls);assertEquals(1,o.placeCalls);assertEquals(1,o.rollbackCalls);assertEquals(0,o.refundCalls);assertEquals(2,o.failures.size());
    }
    @Test void detachedOperationsHaveNoCrossTransactionState(){
        Ops failed=new Ops();failed.placed=false;failed.rolledBack=false;
        assertEquals(RECOVERY_REQUIRED,PlacementTransaction.execute(failed));
        Ops healthy=new Ops();assertEquals(PLACED,PlacementTransaction.execute(healthy));
        assertEquals(List.of("reserve","place"),healthy.trace);assertEquals(0,failed.refundCalls);
    }
    @Test void generatedOutcomeCombinationsMaintainConservationOrdering(){
        Random random=new Random(261001L);
        for(int n=0;n<2000;n++){
            Ops o=new Ops();o.paid=random.nextBoolean();o.placed=random.nextBoolean();o.rolledBack=random.nextBoolean();
            var result=PlacementTransaction.execute(o);
            assertEquals(!o.paid?NO_PAYMENT:o.placed?PLACED:o.rolledBack?ROLLED_BACK:RECOVERY_REQUIRED,result);
            assertEquals(1,o.reserveCalls);assertEquals(o.paid?1:0,o.placeCalls);
            assertEquals(o.paid&&!o.placed?1:0,o.rollbackCalls);
            assertEquals(o.paid&&!o.placed&&o.rolledBack?1:0,o.refundCalls);
        }
    }
}
