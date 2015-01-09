package com.rxnetty.example;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import java.nio.charset.Charset;

import org.codehaus.jackson.map.ObjectMapper;

import rx.Observable;
import rx.functions.Action1;

/**
 * Simple class which demonstrates mocking using RxNetty
 * 
 */
public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        HttpServer<ByteBuf, ByteBuf> server = main.createServer();
        server.start();

        RxNetty.createHttpGet("http://localhost:8484/").toBlocking()
                .forEach(new Action1<HttpClientResponse<ByteBuf>>() {
                    public void call(HttpClientResponse<ByteBuf> t1) {
                        System.out.println(t1.getStatus());
                    }
                });

        RxNetty.createHttpGet("http://localhost:8484/error").toBlocking()
                .forEach(new Action1<HttpClientResponse<ByteBuf>>() {
                    public void call(HttpClientResponse<ByteBuf> t1) {
                        System.out.println(t1.getStatus());
                    }
                });

        RxNetty.createHttpGet("http://localhost:8484/data").toBlocking()
                .forEach(new Action1<HttpClientResponse<ByteBuf>>() {
                    public void call(HttpClientResponse<ByteBuf> t1) {
                        System.out.println(t1.getStatus());
                        /*
                         * Print out the content.
                         */
                        t1.getContent().forEach(new Action1<ByteBuf>() {
                            public void call(ByteBuf t1) {
                                System.out.println(t1.toString(Charset.forName("UTF-8")));
                            }
                        });
                    }
                });

        try {
            server.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method for creating server to be used in this "test", including
     * specific responses for specific requests.
     * 
     * @return
     */
    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(8484, new RequestHandler<ByteBuf, ByteBuf>() {
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                    final HttpServerResponse<ByteBuf> response) {
                System.out.println("Server => Request: " + request.getUri());
                try {
                    /*
                     * Handle different request paths
                     */
                    if (request.getUri().matches("^.*/error.*")) {
                        throw new RuntimeException("forced error");
                    }
                    if (request.getUri().matches("^.*/data.*")) {
                        response.writeString(generateJson());
                    }
                    response.setStatus(HttpResponseStatus.OK);
                    return response.close();
                } catch (Throwable e) {
                    System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.writeString("Error 500: Bad Request\n");
                    return response.close();
                }
            }
        });
        return server;
    }

    private String generateJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(new JsonResponse());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Simple class for Jackson to output JSON
     * 
     */
    private class JsonResponse {
        private int num = 34;
        private String string = "A String";

        public int getNum() {
            return num;
        }

        public String getString() {
            return string;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public void setString(String string) {
            this.string = string;
        }
    }
}
