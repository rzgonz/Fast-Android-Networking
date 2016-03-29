package com.androidnetworking.internal;

import com.androidnetworking.common.AndroidNetworkingRequest;
import com.androidnetworking.core.Core;
import com.androidnetworking.runnables.DataHunter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by amitshekhar on 22/03/16.
 */
public class AndroidNetworkingRequestQueue {

    private final static String TAG = AndroidNetworkingRequestQueue.class.getSimpleName();
    private final Set<AndroidNetworkingRequest> mCurrentRequests = new HashSet<AndroidNetworkingRequest>();
    private AtomicInteger mSequenceGenerator = new AtomicInteger();
    private static AndroidNetworkingRequestQueue sInstance = null;

    public static void initialize() {
        getInstance();
    }

    public static AndroidNetworkingRequestQueue getInstance() {
        if (sInstance == null) {
            synchronized (AndroidNetworkingRequestQueue.class) {
                sInstance = new AndroidNetworkingRequestQueue();
            }
        }
        return sInstance;
    }


    public interface RequestFilter {
        public boolean apply(AndroidNetworkingRequest request);
    }


    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentRequests) {
            for (AndroidNetworkingRequest request : mCurrentRequests) {
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(AndroidNetworkingRequest request) {
                return request.getTag() == tag;
            }
        });
    }

    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public AndroidNetworkingRequest addRequest(AndroidNetworkingRequest request) {
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }
        request.setSequenceNumber(getSequenceNumber());
        request.setFuture(Core.getInstance().getExecutorSupplier().forNetworkTasks().submit(new DataHunter(request)));
        return request;
    }

    public void finish(AndroidNetworkingRequest request) {
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
    }
}