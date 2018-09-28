package org.elastos.carrier.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestCondition {
    static public class ConditionArgs {
        public ConditionArgs() {
            mLock = new ReentrantLock();
            mCond = mLock.newCondition();
            mSignaled = 0;
        }
        private Lock mLock;
        private Condition mCond;
        private int mSignaled;
    }

    public static void condWait(ConditionArgs cond)
    {
        cond.mLock.lock();
        try {
            if (cond.mSignaled <= 0) {
                cond.mCond.await();
            }
            cond.mSignaled--;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            cond.mLock.unlock();
        }
    }

    public static boolean condTrywait(ConditionArgs cond, int ms)
    {
        boolean bRet = false;
        cond.mLock.lock();
        try {
            long endtime = System.currentTimeMillis() + ms;
            long estimateNum = cond.mCond.awaitNanos(TimeUnit.MILLISECONDS.toNanos(endtime));
            if (estimateNum <= 0) {
                //time out
            }
            else {
                cond.mSignaled--;
                bRet = true;
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            cond.mSignaled--;
            bRet = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            cond.mLock.unlock();
        }

        return bRet;
    }

    public static void condSignal(ConditionArgs cond)
    {
        cond.mLock.lock();
        try {
            cond.mSignaled++;
            cond.mCond.signal();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            cond.mLock.unlock();
        }
    }

    public static void condReset(ConditionArgs cond)
    {
        cond.mLock.lock();
        try {
            long timeout = 1000;
            while (cond.mCond.awaitNanos(timeout) > 0) {
                //
            }

            cond.mSignaled++;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            cond.mLock.unlock();
        }
    }

    public static void condDeinit(ConditionArgs cond)
    {
        cond.mSignaled = 0;
    }
}
