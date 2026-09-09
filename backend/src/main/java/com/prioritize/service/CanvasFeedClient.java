package com.prioritize.service;

import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.prioritize.exception.ApiException;

@Component
public class CanvasFeedClient {
    static final int MAX_BYTES = 2 * 1024 * 1024;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public URI validate(String value) {
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !host.toLowerCase(Locale.ROOT).endsWith(".instructure.com")
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !uri.getRawPath().matches("/feeds/calendars/user_[A-Za-z0-9_-]+\\.ics")
                    || value.length() > 2048) throw new IllegalArgumentException();
            return uri;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Use the HTTPS Calendar Feed link from your school's *.instructure.com Canvas site.");
        }
    }
    public String fetch(URI uri) {
        CompletableFuture<HttpResponse<byte[]>> future = null;
        try {
            validate(uri.toString());
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublic(address)) throw new IllegalArgumentException();
            }
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                    .header("Accept", "text/calendar").header("User-Agent", "Prioritize-CanvasFeed/1.0").GET().build();
            future = client.sendAsync(request, info -> new LimitedBody());
            var response = future.get(20, TimeUnit.SECONDS);
            if (response.statusCode() != 200) throw new IllegalArgumentException();
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (future != null) future.cancel(true);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Canvas could not be refreshed. Check your Calendar Feed link and try again. Your previous calendar is unchanged.");
        }
    }
    static boolean isPublic(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) return false;
        byte[] b = a.getAddress();
        if (b.length == 16) return (b[0] & 0xe0) == 0x20; // Global IPv6 unicast only.
        int x=b[0]&255,y=b[1]&255;
        return x != 0 && x < 224 && !(x==100 && y>=64 && y<=127) && !(x==192 && y==0)
                && !(x==198 && (y==18 || y==19));
    }
    static class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        final CompletableFuture<byte[]> body = new CompletableFuture<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return body; }
        public void onSubscribe(Flow.Subscription s) { subscription=s; s.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (bytes.size() + buffer.remaining() > MAX_BYTES) {
                    subscription.cancel(); body.completeExceptionally(new IllegalStateException("Feed too large")); return;
                }
                byte[] b = new byte[buffer.remaining()]; buffer.get(b); bytes.writeBytes(b);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { body.completeExceptionally(error); }
        public void onComplete() { body.complete(bytes.toByteArray()); }
    }
}
