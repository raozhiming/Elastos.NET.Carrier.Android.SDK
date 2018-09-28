package org.elastos.carrier.common;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.common.TestCondition.ConditionArgs;

abstract public class TestContext {
    TestContext() {};

    public static abstract class AbsCarrierContextExtra {
        public AbsCarrierContextExtra() {}
    }

    public static class CarrierContext {
        public CarrierContext() {
            mReadyCond = new ConditionArgs();
            mCond = new ConditionArgs();
        }

        public Carrier mCarrier = null;
        public ConditionArgs mReadyCond = null;
        public ConditionArgs mCond = null;
        public boolean mRobotIsOnline = false;
        public AbsCarrierContextExtra mExtra = null;
    };
}
