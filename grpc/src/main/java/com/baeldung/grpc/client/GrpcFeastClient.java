package com.baeldung.grpc.client;

import feast.Row;
import feast.proto.serving.ServingAPIProto;
import feast.proto.serving.ServingServiceGrpc;
import feast.proto.types.ValueProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrpcFeastClient {
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
            .usePlaintext()
            .build();

        ServingServiceGrpc.ServingServiceBlockingStub stub
          = ServingServiceGrpc.newBlockingStub(channel);

        List<Row> rows = Arrays.asList(
                Row.create().set("driver_id", 1).setEntityTimestamp(Instant.ofEpochSecond(100)));
        HashSet<String> entityRefs = new HashSet<>();
        List<ServingAPIProto.GetOnlineFeaturesRequestV2.EntityRow> entityRows =
                rows.stream()
                        .map(
                                row -> {
                                    entityRefs.addAll(row.getFields().keySet());
                                    return ServingAPIProto.GetOnlineFeaturesRequestV2.EntityRow.newBuilder()
                                            .setTimestamp(row.getEntityTimestamp())
                                            .putAllFields(row.getFields())
                                            .build();
                                })
                        .collect(Collectors.toList());

        ServingAPIProto.GetOnlineFeaturesResponse response =
                stub.getOnlineFeaturesV2(
                        ServingAPIProto.GetOnlineFeaturesRequestV2.newBuilder()
                                .addAllFeatures(createFeatureRefs(Arrays.asList("driver:name", "driver:rating", "driver:null_value")))
                                .addAllEntityRows(entityRows)
                                .setProject("driver_project")
                                .build());

        List<Row> responserow = response.getFieldValuesList().stream()
                .map(
                        fieldValues -> {
                            Row row = Row.create();
                            for (String fieldName : fieldValues.getFieldsMap().keySet()) {
                                row.set(
                                        fieldName,
                                        fieldValues.getFieldsMap().get(fieldName),
                                        fieldValues.getStatusesMap().get(fieldName));
                            }
                            return row;
                        })
                .collect(Collectors.toList());

        System.out.println("Response received from server:\n");

        for (Row row:responserow){
            Row rowTest = row;
            for (Map.Entry<String, ServingAPIProto.GetOnlineFeaturesResponse.FieldStatus> entry:row.getStatuses().entrySet()){
                ServingAPIProto.GetOnlineFeaturesResponse.FieldStatus status = entry.getValue();
                String key = entry.getKey();
            }

            for (Map.Entry<String, ValueProto.Value> entry:row.getFields().entrySet()){
                ValueProto.Value value = entry.getValue();
                String key = entry.getKey();
            }
        }

        channel.shutdown();
    }

    public static List<ServingAPIProto.FeatureReferenceV2> createFeatureRefs(List<String> featureRefStrings) {
        if (featureRefStrings == null) {
            throw new IllegalArgumentException("FeatureReferences cannot be null");
        }

        List<ServingAPIProto.FeatureReferenceV2> featureRefs =
                featureRefStrings.stream()
                        .map(refStr -> parseFeatureRef(refStr))
                        .collect(Collectors.toList());

        return featureRefs;
    }

    public static ServingAPIProto.FeatureReferenceV2 parseFeatureRef(String featureRefString) {
        featureRefString = featureRefString.trim();
        if (featureRefString.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse a empty feature reference");
        }
        if (featureRefString.contains("/")) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unsupported feature reference: Specifying project in string"
                                    + " Feature References is not longer supported: %s",
                            featureRefString));
        }
        if (!featureRefString.contains(":")) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unsupported feature reference: %s - FeatureTable name and Feature name should be provided in string"
                                    + " Feature References, in <featureTableName>:<featureName> format.",
                            featureRefString));
        }

        String[] featureReferenceParts = featureRefString.split(":");
        ServingAPIProto.FeatureReferenceV2 featureRef =
                ServingAPIProto.FeatureReferenceV2.newBuilder()
                        .setFeatureTable(featureReferenceParts[0])
                        .setName(featureReferenceParts[1])
                        .build();

        return featureRef;
    }
}
