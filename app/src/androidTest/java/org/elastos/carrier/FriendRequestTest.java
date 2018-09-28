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
import org.elastos.carrier.common.SocketUtils.TestCmds;
import org.elastos.carrier.exceptions.ElastosException;
import org.elastos.carrier.robot.RobotProxy;
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
    private static String robotid = null;
    private static String robotaddr = null;

    static class TestHandler extends AbstractCarrierHandler {
        @Override
        public void onReady(Carrier carrier) {
            ready_cb(sTestContext);
            Log.d(TAG, "============================onReady=====================");
        }

        @Override
        public void onFriendConnection(Carrier carrier, String friendId, ConnectionStatus status) {
            CarrierContext wctxt = sTestContext;

            ((CarrierContextExtra)wctxt.extra).connection_status = status;
            wctxt.robot_online = (status == ConnectionStatus.Connected);
            wakeup(sTestContext);

            Log.d(TAG, "Robot connection status changed -> " + status.toString());
        }

        @Override
        public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
            CarrierContextExtra extra = (CarrierContextExtra)sTestContext.extra;

            extra.from  = userId;
            extra.hello = hello;
            extra.info = info;

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
    public void test_add_friend()
    {
        CarrierContext wctxt = sTestContext;
        CarrierContextExtra extra = (CarrierContextExtra)wctxt.extra;
        String userid = null;

        TestCondition.cond_reset(sTestContext.cond);

        int rc = remove_friend_anyway(robotid);
        assertEquals(rc, 0);

        try {
            assertFalse(sTestContext.carrier.isFriend(robotid));
            wctxt.carrier.addFriend(robotaddr, "hello");

            // wait until robot having received "fadd” request.
            TestCmds cmds = new TestCmds();
            rc = read_ack(cmds);
            assertEquals(rc, 2);
            assertEquals(cmds.args[0], "hello");
            assertEquals(cmds.args[1], "hello");

            userid = wctxt.carrier.getUserId();
            rc = write_cmd("faccept " + userid + "\n");
            assertTrue(rc > 0);

            // wait for friend_added() callback to be invoked.
            TestCondition.cond_trywait(wctxt.cond, 60000);
            assertTrue(wctxt.carrier.isFriend(robotid));
            // wait for friend connection (online) callback to be invoked.
            TestCondition.cond_wait(wctxt.cond);
            assertTrue(extra.connection_status == ConnectionStatus.Connected);

            TestCmds cmds2 = new TestCmds();
            rc = read_ack(cmds2);
            assertEquals(rc, 2);
            assertEquals(cmds.args[0], "fadd");
            assertEquals(cmds.args[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void test_accept_friend()
    {
        CarrierContext wctxt = sTestContext;
        CarrierContextExtra extra = (CarrierContextExtra)wctxt.extra;
        String userid = null;
        String useraddr = null;
        final String hello = "hello";
        int rc = 0;

        TestCondition.cond_reset(sTestContext.cond);

        rc = remove_friend_anyway(robotid);
        assertEquals(rc, 0);

        try {
            assertFalse(wctxt.carrier.isFriend(robotid));
            userid = wctxt.carrier.getUserId();
            useraddr = wctxt.carrier.getAddress();

            rc = write_cmd("fadd " + userid + useraddr + hello + "\n");
            assertTrue(rc > 0);

            // wait for friend_request callback invoked;
            TestCondition.cond_trywait(wctxt.cond, 60000);
            assertTrue(extra.from != null);
            assertTrue(extra.hello != null);

            assertEquals(extra.from, robotid);
            assertEquals(extra.from, extra.info.getUserId());
            assertEquals(extra.hello, hello);
            //TODO: test robot user info;

            wctxt.carrier.acceptFriend(robotid);

            // wait for friend added callback invoked;
            TestCondition.cond_wait(wctxt.cond);
            assertTrue(wctxt.carrier.isFriend(robotid));

            // wait for friend connection (online) callback invoked.
            TestCondition.cond_wait(wctxt.cond);
            assertTrue(extra.connection_status == ConnectionStatus.Connected);

            TestCmds cmds = new TestCmds();
            rc = read_ack(cmds);
            assertEquals(rc, 2);
            assertEquals(cmds.args[0], "fadd");
            assertEquals(cmds.args[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void test_add_friend_be_friend()
    {
        CarrierContext wctxt = sTestContext;
        int rc;

        TestCondition.cond_reset(sTestContext.cond);

        try {
            Log.d(TAG, "robotid=["+robotid+"], robotaddr=["+robotaddr+"]");
            rc = add_friend_anyway(robotid, robotaddr);
            assertEquals(rc, 0);
            assertTrue(wctxt.carrier.isFriend(robotid));

            wctxt.carrier.addFriend(robotaddr, "hello");
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
    public void test_add_self_be_friend()
    {
        CarrierContext wctxt = sTestContext;
        int rc;

        String address = null;

        try {
            address = wctxt.carrier.getAddress();
            wctxt.carrier.addFriend(address, "hello");
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
            //TODO
            TestCmds cmds = new TestCmds();
            if (mSocketUtils.read_ack(cmds) > 0 && cmds.args[0].equals("ready")) {
                robotid = cmds.args[1];
                robotaddr = cmds.args[2];
                Log.d(TAG, "robotaddr=["+robotaddr+"], robotid=["+robotid+"]");
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
        sTestContext.extra = new CarrierContextExtra();

        try {
            Carrier.initializeInstance(sCarrierOptions, sTestHandler);
            sTestContext.carrier = Carrier.getInstance();

            TestCondition.cond_reset(sTestContext.cond);
            TestCondition.cond_reset(sTestContext.ready_cond);

            sTestContext.carrier.start(10);

            //The self carrier node will be ready.
            TestCondition.cond_wait(sTestContext.ready_cond);

            Log.i(TAG, "carrier client is ready now");

        } catch (ElastosException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            sTestContext.carrier.kill();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int add_friend_anyway(String userid, String address)
    {
        CarrierContext wctxt = sTestContext;
        int rc;

        try {
            if (wctxt.carrier.isFriend(userid)) {
                while(!wctxt.robot_online) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }

                return 1;
            }

            wctxt.carrier.addFriend(address, "auto-reply");

            // wait for friend_added callback invoked.
            TestCondition.cond_wait(wctxt.cond);

            // wait for friend_connection (online) callback invoked.
            TestCondition.cond_wait(wctxt.cond);

            // wait until robot being notified us connected.
            TestCmds cmds = new TestCmds();
            rc = read_ack(cmds);
            assertEquals(rc, 2);
            assertEquals(cmds.args[0], "fadd");
            assertEquals(cmds.args[1], "succeeded");
        }
        catch (ElastosException e) {
            e.printStackTrace();
            assertTrue(false);
            return -1;
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            return -1;
        }

        return 0;
    }

    private int remove_friend_anyway(String userid)
    {
        CarrierContext wctxt = sTestContext;
        try {
            if (!wctxt.carrier.isFriend(userid)) {
                while (wctxt.robot_online) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }
                return 0;
            } else {
                while (!wctxt.robot_online) {
                    //sleep 500 microseconds = 500 * 1000 nanoseconds
                    Thread.sleep(0, 500 * 1000);
                }
            }

            wctxt.carrier.removeFriend(userid);

            UserInfo info = wctxt.carrier.getSelfInfo();

            write_cmd("fremove " + info.getUserId());

            // wait for friend_connection (online -> offline) callback invoked.
            TestCondition.cond_wait(wctxt.cond);

            // wait for friend_removed callback invoked.
            TestCondition.cond_wait(wctxt.cond);

            // wait for completion of robot "fremove" command.
            TestCmds cmds = new TestCmds();
            int count = read_ack(cmds);
            assertEquals(count, 2);
            assertEquals(cmds.args[0], "fremove");
            assertEquals(cmds.args[1], "succeeded");
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

    private int write_cmd(String cmd2Robot) {
        return mSocketUtils.write_cmd(cmd2Robot);
    }

    private int read_ack(TestCmds cmds) {
        return mSocketUtils.read_ack(cmds);
    }

    private static Context getAppContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    private static String getAppPath() {
        return getAppContext().getFilesDir().getAbsolutePath();
    }

    static void wakeup(CarrierContext context)
    {
        TestCondition.cond_signal(context.cond);
    }

    static void ready_cb(CarrierContext context)
    {
        TestCondition.cond_signal(context.ready_cond);
    }

    static class CarrierContextExtra extends TestContext.AbsCarrierContextExtra {
        CarrierContextExtra() {
            super();
            from = null;
            info = null;
            hello = null;
            len = 0;
            connection_status = ConnectionStatus.Disconnected;
        }

        String from;
        // for friend request
        UserInfo info;

        String hello;
        int len;
        ConnectionStatus connection_status;
    };
}
