package com.prioritize.service;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
class CanvasFeedClientTest {
    @Test void onlyAcceptsCanvasHttpsFeedUrlsAndPublicAddresses() throws Exception {
        var client=new CanvasFeedClient();
        assertEquals("school.instructure.com",client.validate("https://school.instructure.com/feeds/calendars/user_fixture.ics").getHost());
        for(String url:new String[]{"http://school.instructure.com/feeds/calendars/user_fixture.ics",
                "https://127.0.0.1/feeds/calendars/user_fixture.ics", "https://school.instructure.com.evil.test/feeds/calendars/user_fixture.ics",
                "https://school.instructure.com:8443/feeds/calendars/user_fixture.ics", "https://school.instructure.com/feeds/calendars/user_fixture.ics?redirect=http://localhost",
                "https://school.instructure.com@evil.test/feeds/calendars/user_fixture.ics", "https://school.instructure.com/feeds/calendars/../user_fixture.ics"})
            assertThrows(RuntimeException.class,()->client.validate(url));
        for(String ip:new String[]{"127.0.0.1","10.0.0.1","169.254.169.254","100.100.100.100","::1","fc00::1","192.168.1.1"})
            assertFalse(CanvasFeedClient.isPublic(InetAddress.getByName(ip)));
    }
    @Test void boundsResponseSizeBeforeAllocatingWholeFeed() {
        var body=new CanvasFeedClient.LimitedBody();
        boolean[] cancelled={false};
        body.onSubscribe(new Flow.Subscription(){public void request(long n){} public void cancel(){cancelled[0]=true;}});
        body.onNext(List.of(ByteBuffer.allocate(CanvasFeedClient.MAX_BYTES+1)));
        assertTrue(cancelled[0]); assertTrue(body.body.isCompletedExceptionally()); assertEquals(0,body.bytes.size());
    }
    @Test void encryptsLinksWithDistinctNoncesAndRejectsTampering() {
        var crypto=new CanvasFeedCrypto("test-secret");
        String one=crypto.encrypt("private-feed"),two=crypto.encrypt("private-feed");
        assertNotEquals(one,two); assertFalse(one.contains("private-feed")); assertEquals("private-feed",crypto.decrypt(one));
        assertThrows(IllegalStateException.class,()->new CanvasFeedCrypto("different-secret").decrypt(one));
    }
}
