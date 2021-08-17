package com.baeldung.grpc.server;

//import com.baeldung.grpc.HelloRequest;
//import com.baeldung.grpc.HelloResponse;
//import com.baeldung.grpc.HelloServiceGrpc.HelloServiceImplBase;

import com.google.protobuf.Descriptors;
import feast.proto.serving.ServingAPIProto;
import feast.proto.serving.ServingServiceGrpc;
import feast.proto.types.ValueProto;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class FeastServiceImpl extends ServingServiceGrpc.ServingServiceImplBase {
    @Override
    public void getOnlineFeaturesV2(ServingAPIProto.GetOnlineFeaturesRequestV2 request, StreamObserver<ServingAPIProto.GetOnlineFeaturesResponse> responseObserver) {

        for (ServingAPIProto.GetOnlineFeaturesRequestV2.EntityRow row:request.getEntityRowsList()){
            for (Map.Entry<String, ValueProto.Value> field:row.getFieldsMap().entrySet()){
                ValueProto.Value value = field.getValue();
                String key = field.getKey();
                System.out.println(key);
            }
        }

        for (ServingAPIProto.FeatureReferenceV2 featureReferenceV2:request.getFeaturesList()){
            for (Map.Entry<Descriptors.FieldDescriptor, Object> field:featureReferenceV2.getAllFields().entrySet()){
                Object value = field.getValue();
                Descriptors.FieldDescriptor key = field.getKey();
                System.out.println(key);
            }
        }

        ServingAPIProto.GetOnlineFeaturesResponse response = ServingAPIProto.GetOnlineFeaturesResponse
                .newBuilder()
                .addFieldValues(0, ServingAPIProto.GetOnlineFeaturesResponse.FieldValues
                        .newBuilder()
                        .putStatusesValue("qwe", 1)
                        .putStatuses("qwe2", ServingAPIProto.GetOnlineFeaturesResponse.FieldStatus.PRESENT)
                        .putFields("qwe2", ValueProto.Value.newBuilder().setStringVal("qweqweqew").build())
                        .build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
