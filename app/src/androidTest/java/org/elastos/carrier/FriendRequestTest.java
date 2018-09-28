package org.elastos.carrier;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.elastos.carrier.common.TestCondition;
import org.elastos.carrier.common.TestContext;
import org.elastos.carrier.common.TestContext.CarrierContext;
import org.elastos.carrier.common.TestOptions;
import org.elastos.carrier.common.SocketUtils;
import org.elastos.carrier.common.SocketUtils.TestRecvDataArgs;
import org.elastos.carrier.exceptions.ElastosException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class FriendRequestTest {
    private static final String TAG = "FriendRequestTest";
    private static TestOptions sCarrierOptions = null;
    private static TestHandler sTestHandler = null;
    private static CarrierContext sTestContext = null;
    private static String ROBOTHOST = "192.168.0.107";
    private static String ROBOTPORT = "8888";
    private static SocketUtils mSocketUtils = null;
    private static String mRobotId = null;
    private static String mRobotAddress = null;

    static class TestHandler extends AbstractCarrierHandler {
        @Override
        public void onReady(Carrier carrier) {
            TestCondition.condSignal(sTestContext.mReadyCond);
            Log.d(TAG, "============================onReady=====================");
        }

        @Override
        public void onFriendConnection(Carrier carrier, String friendId, ConnectionStatus status) {
            CarrierContext context = sTestContext;

            ((CarrierContextExtra)context.mExtra).mConnectionStatus = status;
            context.mRobotIsOnline = (status == ConnectionStatus.Connected);
            wakeup(sTestContext);

            Log.d(TAG, "Robot connection status changed -> " + status.toString());
        }

        @Override
        public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
            CarrierContextExtra extra = (CarrierContextExtra)sTestContext.mExtra;

            extra.mFrom  = userId;
            extra.mHello = hello;
            extra.mUserInfo = info;

            wakeup(sTestContext);
        }

        @Override
        public void onFriendAdded(Carrier carrier, FriendInfo info) {
            wakeup(sTestContext);
            Log.d(TAG, "Friend [" + info.getUserId() + "] added.");
        }

        @Override
        public void onFriendRemoved(Carrier carrier, String friendId) {
            wakeup(sTestContext);
            Log.d(TAG, "Friend [" + friendId + "] removed");
        }
    }

    @Test
    public void testAddFriend()
    {
        CarrierContext context = sTestContext;
        CarrierContextExtra extra = (CarrierContextExtra)context.mExtra;
        String userId = null;

        TestCondition.condReset(sTestContext.mCond);

        int rc = removeFriendAnyway(mRobotId);
        assertEquals(rc, 0);

        try {
            assertFalse(sTestContext.mCarrier.isFriend(mRobotId));
            context.mCarrier.addFriend(mRobotAddress, "hello");

            // wait until robot having received "fadd” request.
            TestRecvDataArgs args = new TestRecvDataArgs();
            rc = recvDataFromRobot(args);
            assertEquals(rc, 2);
            assertEquals(args.mArgs[0], "hello");
            assertEquals(args.mArgs[1], "hello");

            userId = context.mCarrier.getUserId();
            rc = sendData2Robot("faccept " + userId + "\n");
            assertTrue(rc > 0);

            // wait for friend_added() callback to be invoked.
            TestCondition.condTrywait(context.mCond, 60000);
            assertTrue(context.mCarrier.isFriend(mRobotId));
            // wait for friend connection (online) callback to be invoked.
            TestCondition.condWait(context.mCond);
            assertTrue(extra.mConnectionStatus == ConnectionStatus.Connected);

            TestRecvDataArgs cmds2 = new TestRecvDataArgs();
            rc = recvDataFromRobot(cmds2);
            assertEquals(rc, 2);
            assertEquals(args.mArgs[0], "fadd");
            assertEquals(args.mArgs[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testAcceptFriend()
    {
        CarrierContext context = sTestContext;
        CarrierContextExtra extra = (CarrierContextExtra)context.mExtra;
        String userId = null;
        String userAddr = null;
        final String hello = "hello";
        int rc = 0;

        TestCondition.condReset(sTestContext.mCond);

        rc = removeFriendAnyway(mRobotId);
        assertEquals(rc, 0);

        try {
            assertFalse(context.mCarrier.isFriend(mRobotId));
            userId = context.mCarrier.getUserId();
            userAddr = context.mCarrier.getAddress();

            rc = sendData2Robot("fadd " + userId + userAddr + hello + "\n");
            assertTrue(rc > 0);

            // wait for friend_request callback invoked;
            TestCondition.condTrywait(context.mCond, 60000);
            assertTrue(extra.mFrom != null);
            assertTrue(extra.mHello != null);

            assertEquals(extra.mFrom, mRobotId);
            assertEquals(extra.mFrom, extra.mUserInfo.getUserId());
            assertEquals(extra.mHello, hello);

            context.mCarrier.acceptFriend(mRobotId);

            // wait for friend added callback invoked;
            TestCondition.condWait(context.mCond);
            assertTrue(context.mCarrier.isFriend(mRobotId));

            // wait for friend connection (online) callback invoked.
            TestCondition.condWait(context.mCond);
            assertTrue(extra.mConnectionStatus == ConnectionStatus.Connected);

            TestRecvDataArgs args = new TestRecvDataArgs();
            rc = recvDataFromRobot(args);
            assertEquals(rc, 2);
            assertEquals(args.mArgs[0], "fadd");
            assertEquals(args.mArgs[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testAddFriendBeFriend()
    {
        CarrierContext context = sTestContext;
        int rc;

        TestCondition.condReset(sTestContext.mCond);

        try {
            Log.d(TAG, "robotid=["+mRobotId+"], robotaddr=["+mRobotAddress+"]");
            rc = addFriendAnyway(mRobotId, mRobotAddress);
            assertEquals(rc, 0);
            assertTrue(context.mCarrier.isFriend(mRobotId));

            context.mCarrier.addFriend(mRobotAddress, "hello");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertEquals(e.getErrorCode(), 0x8100000B);
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testAddSelfBeFriend()
    {
        CarrierContext context = sTestContext;
        int rc;

        String address = null;

        try {
            address = context.mCarrier.getAddress();
            context.mCarrier.addFriend(address, "hello");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertEquals(e.getErrorCode(), 0x81000001);
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @BeforeClass
    public static void setUp() {
        mSocketUtils = new SocketUtils();
        try {
            if (!mSocketUtils.connectRobot(ROBOTHOST, ROBOTPORT)) {
                Log.d(TAG, "Can't connect to the robot.");
                assertTrue(false);
            }

            //Get the robot's id and address.
            TestRecvDataArgs args = new TestRecvDataArgs();
            if (recvDataFromRobot(args) > 0 && args.mArgs[0].equals("ready")) {
                mRobotId = args.mArgs[1];
                mRobotAddress = args.mArgs[2];
                Log.d(TAG, "robotaddr=["+mRobotAddress+"], robotid=["+mRobotId+"]");
            }
            else {
                mSocketUtils.disconnectRobot();
                assertTrue(false);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        sCarrierOptions = new TestOptions(getAppPath());
        sTestHandler = new TestHandler();
        sTestContext = new CarrierContext();
        sTestContext.mExtra = new CarrierContextExtra();

        try {
            Carrier.initializeInstance(sCarrierOptions, sTestHandler);
            sTestContext.mCarrier = Carrier.getInstance();

            TestCondition.condReset(sTestContext.mCond);
            TestCondition.condReset(sTestContext.mReadyCond);

            sTestContext.mCarrier.start(10);

            //The self carrier node will be ready.
            TestCondition.condWait(sTestContext.mReadyCond);

            Log.i(TAG, "carrier client is ready now");

        }
        catch (ElastosException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            sTestContext.mCarrier.kill();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int addFriendAnyway(String userId, String address)
    {
        CarrierContext wctxt = sTestContext;
        int rc;

        try {
            if (wctxt.mCarrier.isFriend(userId)) {
                while(!wctxt.mRobotIsOnline) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }

                return 1;
            }

            wctxt.mCarrier.addFriend(address, "auto-reply");

            // wait for friend_added callback invoked.
            TestCondition.condWait(wctxt.mCond);

            // wait for friend_connection (online) callback invoked.
            TestCondition.condWait(wctxt.mCond);

            // wait until robot being notified us connected.
            TestRecvDataArgs args = new TestRecvDataArgs();
            rc = recvDataFromRobot(args);
            assertEquals(rc, 2);
            assertEquals(args.mArgs[0], "fadd");
            assertEquals(args.mArgs[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
            return -1;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            assertTrue(false);
            return -1;
        }

        return 0;
    }

    private int removeFriendAnyway(String userId)
    {
        CarrierContext wctxt = sTestContext;
        try {
            if (!wctxt.mCarrier.isFriend(userId)) {
                while (wctxt.mRobotIsOnline) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }
                return 0;
            } else {
                while (!wctxt.mRobotIsOnline) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }
            }

            wctxt.mCarrier.removeFriend(userId);

            UserInfo info = wctxt.mCarrier.getSelfInfo();

            sendData2Robot("fremove " + info.getUserId());

            // wait for friend_connection (online -> offline) callback invoked.
            TestCondition.condWait(wctxt.mCond);

            // wait for friend_removed callback invoked.
            TestCondition.condWait(wctxt.mCond);

            // wait for completion of robot "fremove" command.
            TestRecvDataArgs args = new TestRecvDataArgs();
            int count = recvDataFromRobot(args);
            assertEquals(count, 2);
            assertEquals(args.mArgs[0], "fremove");
            assertEquals(args.mArgs[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            return -1;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    private static int sendData2Robot(String cmd2Robot) {
        return mSocketUtils.sendData2Robot(cmd2Robot);
    }

    private static int recvDataFromRobot(TestRecvDataArgs args) {
        return mSocketUtils.recvDataFromRobot(args);
    }

    private static Context getAppContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    private static String getAppPath() {
        return getAppContext().getFilesDir().getAbsolutePath();
    }

    static void wakeup(CarrierContext context)
    {
        TestCondition.condSignal(context.mCond);
    }

    static class CarrierContextExtra extends TestContext.AbsCarrierContextExtra {
        CarrierContextExtra() {
            super();
            mFrom = null;
            mUserInfo = null;
            mHello = null;
            mConnectionStatus = ConnectionStatus.Disconnected;
        }

        String mFrom;
        // for friend request
        UserInfo mUserInfo;

        String mHello;
        ConnectionStatus mConnectionStatus;
    };
}
