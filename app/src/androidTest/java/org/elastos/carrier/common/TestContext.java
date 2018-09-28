package org.elastos.carrier.common;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;

abstract public class TestContext {
    public CarrierContext carrier = null;

    TestContext() {};

    public abstract void context_reset(TestContext context);

    public static abstract class AbsCarrierContextExtra {
        public AbsCarrierContextExtra() {}
    }

    public static class CarrierContext {
        public CarrierContext() {
            ready_cond = new TestCondition.ConditionArgs();
            cond = new TestCondition.ConditionArgs();
        }

        public AbstractCarrierHandler cbs = null;
        public Carrier carrier = null;
        public TestCondition.ConditionArgs ready_cond = null;
        public TestCondition.ConditionArgs cond = null;
        public Thread thread = null;
        public boolean robot_online = false;
        public boolean fadd_in_progress = false; // exclusive on robot

        public AbsCarrierContextExtra extra = null;
    };
}
