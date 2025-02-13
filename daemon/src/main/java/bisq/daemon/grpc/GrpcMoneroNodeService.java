/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */
package bisq.daemon.grpc;

import bisq.core.api.CoreApi;
import bisq.core.xmr.MoneroNodeSettings;

import bisq.proto.grpc.GetMoneroNodeSettingsReply;
import bisq.proto.grpc.GetMoneroNodeSettingsRequest;
import bisq.proto.grpc.IsMoneroNodeRunningReply;
import bisq.proto.grpc.IsMoneroNodeRunningRequest;
import bisq.proto.grpc.MoneroNodeGrpc.MoneroNodeImplBase;
import bisq.proto.grpc.StartMoneroNodeReply;
import bisq.proto.grpc.StartMoneroNodeRequest;
import bisq.proto.grpc.StopMoneroNodeReply;
import bisq.proto.grpc.StopMoneroNodeRequest;

import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import static bisq.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import static bisq.proto.grpc.MoneroNodeGrpc.getStartMoneroNodeMethod;
import static bisq.proto.grpc.MoneroNodeGrpc.getStopMoneroNodeMethod;
import static bisq.proto.grpc.MoneroNodeGrpc.getIsMoneroNodeRunningMethod;
import static bisq.proto.grpc.MoneroNodeGrpc.getGetMoneroNodeSettingsMethod;
import static java.util.concurrent.TimeUnit.SECONDS;

import bisq.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import bisq.daemon.grpc.interceptor.GrpcCallRateMeter;
import monero.common.MoneroError;

@Slf4j
public class GrpcMoneroNodeService extends MoneroNodeImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcMoneroNodeService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void isMoneroNodeRunning(IsMoneroNodeRunningRequest request,
                                    StreamObserver<IsMoneroNodeRunningReply> responseObserver) {
        try {
            var reply = IsMoneroNodeRunningReply.newBuilder()
                    .setIsRunning(coreApi.isMoneroNodeRunning())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getMoneroNodeSettings(GetMoneroNodeSettingsRequest request,
                                      StreamObserver<GetMoneroNodeSettingsReply> responseObserver) {
        try {
            var settings = coreApi.getMoneroNodeSettings();
            var builder = GetMoneroNodeSettingsReply.newBuilder();
            if (settings != null) {
                builder.setSettings(settings.toProtoMessage());
            }
            var reply = builder.build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void startMoneroNode(StartMoneroNodeRequest request,
                                StreamObserver<StartMoneroNodeReply> responseObserver) {
        try {
            var settings = request.getSettings();
            coreApi.startMoneroNode(MoneroNodeSettings.fromProto(settings));
            var reply = StartMoneroNodeReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (MoneroError me) {
            handleMoneroError(me, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void stopMoneroNode(StopMoneroNodeRequest request,
                               StreamObserver<StopMoneroNodeReply> responseObserver) {
        try {
            coreApi.stopMoneroNode();
            var reply = StopMoneroNodeReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (MoneroError me) {
            handleMoneroError(me, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    private void handleMoneroError(MoneroError me, StreamObserver<?> responseObserver) {
        // MoneroError is caused by the node startup failing, don't treat as unknown server error
        // by wrapping with a handled exception type.
        var headerLengthLimit = 8192; // MoneroErrors may print the entire monerod help text which causes a header overflow in grpc
        if (me.getMessage().length() > headerLengthLimit) {
            exceptionHandler.handleException(log, new IllegalStateException(me.getMessage().substring(0, headerLengthLimit - 1)), responseObserver);
        } else {
            exceptionHandler.handleException(log, new IllegalStateException(me), responseObserver);
        }
    }

    final ServerInterceptor[] interceptors() {
        Optional<ServerInterceptor> rateMeteringInterceptor = rateMeteringInterceptor();
        return rateMeteringInterceptor.map(serverInterceptor ->
                new ServerInterceptor[]{serverInterceptor}).orElseGet(() -> new ServerInterceptor[0]);
    }

    private Optional<ServerInterceptor> rateMeteringInterceptor() {
        return getCustomRateMeteringInterceptor(coreApi.getConfig().appDataDir, this.getClass())
                .or(() -> Optional.of(CallRateMeteringInterceptor.valueOf(
                        new HashMap<>() {{
                            int allowedCallsPerTimeWindow = 10;
                            put(getIsMoneroNodeRunningMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getGetMoneroNodeSettingsMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStartMoneroNodeMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStopMoneroNodeMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                        }}
                )));
    }
}
